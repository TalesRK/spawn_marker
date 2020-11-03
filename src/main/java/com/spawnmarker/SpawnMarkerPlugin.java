package com.spawnmarker;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NpcID;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.NpcSpawned;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import java.awt.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@PluginDescriptor(
        name = "Spawn Marker",
        description = "Marks the tile where a creature have spawned",
        tags = {"spot", "marker", "npc", "spawn", "tracker"}
)
public class SpawnMarkerPlugin extends Plugin {

    @Getter(AccessLevel.PACKAGE)
    private final List<ColorTileMarker> points = new ArrayList<>();
    private static LocalDateTime startedUpAt;

    private static final String CONFIG_GROUP = "spawnMarker";
    private static final String REGION_PREFIX = "region_";
    private static final Gson GSON = new Gson();
    private static final int MAX_SAVE_DISTANCE = 12;
    private static final Color DEFAULT_COLOR = new Color(255, 108, 0);
    private static final Set<Integer> EVENT_NPCS = ImmutableSet.of(
            NpcID.BEE_KEEPER_6747,
            NpcID.CAPT_ARNAV,
            NpcID.DR_JEKYLL, NpcID.DR_JEKYLL_314,
            NpcID.DRUNKEN_DWARF,
            NpcID.DUNCE_6749,
            NpcID.EVIL_BOB, NpcID.EVIL_BOB_6754,
            NpcID.FLIPPA_6744,
            NpcID.FREAKY_FORESTER_6748,
            NpcID.FROG_5429,
            NpcID.GENIE, NpcID.GENIE_327,
            NpcID.GILES, NpcID.GILES_5441,
            NpcID.LEO_6746,
            NpcID.MILES, NpcID.MILES_5440,
            NpcID.MYSTERIOUS_OLD_MAN_6750, NpcID.MYSTERIOUS_OLD_MAN_6751,
            NpcID.MYSTERIOUS_OLD_MAN_6752, NpcID.MYSTERIOUS_OLD_MAN_6753,
            NpcID.NILES, NpcID.NILES_5439,
            NpcID.PILLORY_GUARD,
            NpcID.POSTIE_PETE_6738,
            NpcID.QUIZ_MASTER_6755,
            NpcID.RICK_TURPENTINE, NpcID.RICK_TURPENTINE_376,
            NpcID.SANDWICH_LADY,
            NpcID.SERGEANT_DAMIEN_6743
    );

    @Inject
    private Client client;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private SpawnMarkerOverlay overlay;

    @Inject
    private ConfigManager configManager;

    @Override
    protected void startUp() throws Exception {
        overlayManager.add(overlay);
        startedUpAt = LocalDateTime.now();
    }

    @Override
    protected void shutDown() throws Exception {
        this.clearPointsSaved();
        overlayManager.remove(overlay);
    }

    private void clearPointsSaved(){
        points.clear();
        int[] regions = client.getMapRegions();
        if (regions != null) {
            for (int regionId : regions) {
                savePoints(regionId, null);
            }
        }
    }

    @Subscribe
    public void onNpcSpawned(NpcSpawned npcSpawned) {
        if (LocalDateTime.now().isAfter(startedUpAt.plusSeconds(5))) {
            String npcName = npcSpawned.getActor().getName();

            if (StringUtils.isNotBlank(npcName) && !EVENT_NPCS.contains(npcSpawned.getNpc().getId()) && npcSpawned.getNpc().getCombatLevel() > 0) {
                WorldPoint playerLocation = client.getLocalPlayer().getWorldLocation();
                WorldPoint npcLocation = npcSpawned.getActor().getWorldLocation();

                if (npcLocation.distanceTo(playerLocation) > MAX_SAVE_DISTANCE) {
                    return;
                }
                markTile(npcSpawned.getActor().getLocalLocation(), npcName);
            }
        }
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged gameStateChanged) {
        startedUpAt = LocalDateTime.now();
        if (gameStateChanged.getGameState() != GameState.LOGGED_IN) {
            return;
        }
        loadPoints();
    }

    private void savePoints(int regionId, Collection<TilePoint> points) {
        if (points == null || points.isEmpty()) {
            configManager.unsetConfiguration(CONFIG_GROUP, REGION_PREFIX + regionId);
            return;
        }

        String json = GSON.toJson(points);
        configManager.setConfiguration(CONFIG_GROUP, REGION_PREFIX + regionId, json);
    }

    private Collection<TilePoint> getPoints(int regionId) {
        String json = configManager.getConfiguration(CONFIG_GROUP, REGION_PREFIX + regionId);
        if (Strings.isNullOrEmpty(json)) {
            return Collections.emptyList();
        }

        return GSON.fromJson(json, new TypeToken<List<TilePoint>>(){}.getType());
    }

    private void loadPoints() {
        points.clear();

        int[] regions = client.getMapRegions();

        if (regions == null) {
            return;
        }

        for (int regionId : regions) {
            Collection<TilePoint> regionPoints = getPoints(regionId);
            Collection<ColorTileMarker> colorTileMarkers = translateToColorTileMarker(regionPoints);
            points.addAll(colorTileMarkers);
        }
    }

    /**
     * Translate a collection of tile points to color tile markers, accounting for instances
     *
     * @param points {@link TilePoint}s to be converted to {@link ColorTileMarker}s
     * @return A collection of color tile markers, converted from the passed tile points, accounting for local
     *         instance points. See {@link WorldPoint#toLocalInstance(Client, WorldPoint)}
     */
    private Collection<ColorTileMarker> translateToColorTileMarker(Collection<TilePoint> points) {
        if (points.isEmpty()) {
            return Collections.emptyList();
        }

        return points.stream()
                .map(point -> new ColorTileMarker(
                        WorldPoint.fromRegion(point.getRegionId(), point.getRegionX(), point.getRegionY(), point.getZ()),
                        point.getColor(), point.getLabel()))
                .flatMap(colorTile -> {
                    final Collection<WorldPoint> localWorldPoints = WorldPoint.toLocalInstance(client, colorTile.getWorldPoint());
                    return localWorldPoints.stream().map(wp -> new ColorTileMarker(wp, colorTile.getColor(), colorTile.getLabel()));
                })
                .collect(Collectors.toList());
    }

    private void markTile(LocalPoint localPoint, String label) {
        if (localPoint == null) {
            return;
        }

        WorldPoint worldPoint = WorldPoint.fromLocalInstance(client, localPoint);

        int regionId = worldPoint.getRegionID();
        TilePoint point = new TilePoint(regionId, worldPoint.getRegionX(), worldPoint.getRegionY(), client.getPlane(), DEFAULT_COLOR, label);
        log.debug("Updating point: {} - {}", point, worldPoint);

        List<TilePoint> groundMarkerPoints = new ArrayList<>(getPoints(regionId));
        if (!groundMarkerPoints.contains(point)) {
            groundMarkerPoints.add(point);
        }

        savePoints(regionId, groundMarkerPoints);

        loadPoints();
    }

}
