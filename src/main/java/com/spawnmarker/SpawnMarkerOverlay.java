package com.spawnmarker;

import com.google.common.base.Strings;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.*;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.awt.*;
import java.util.Collection;

public class SpawnMarkerOverlay extends Overlay {

    private static final int MAX_DRAW_DISTANCE = 32;

    private final Client client;
    private final SpawnMarkerPlugin plugin;

    @Inject
    private SpawnMarkerOverlay(Client client, SpawnMarkerPlugin plugin) {
        this.plugin = plugin;
        this.client = client;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
        setPriority(OverlayPriority.LOW);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        final Collection<ColorTileMarker> points = plugin.getPoints();
        for (final ColorTileMarker point : points) {
            WorldPoint worldPoint = point.getWorldPoint();
            if (worldPoint.getPlane() != client.getPlane()) {
                continue;
            }

            Color tileColor = point.getColor();
            drawTile(graphics, worldPoint, tileColor, point.getLabel());
        }

        return null;
    }

    private void drawTile(Graphics2D graphics, WorldPoint point, Color color, @Nullable String label) {
        WorldPoint playerLocation = client.getLocalPlayer().getWorldLocation();

        if (point.distanceTo(playerLocation) >= MAX_DRAW_DISTANCE) {
            return;
        }

        LocalPoint lp = LocalPoint.fromWorld(client, point);
        if (lp == null) {
            return;
        }

        Polygon poly = Perspective.getCanvasTilePoly(client, lp);
        if (poly != null) {
            OverlayUtil.renderPolygon(graphics, poly, color);
        }

        if (!Strings.isNullOrEmpty(label)) {
            Point canvasTextLocation = Perspective.getCanvasTextLocation(client, graphics, lp, label, 0);
            if (canvasTextLocation != null) {
                OverlayUtil.renderTextLocation(graphics, canvasTextLocation, label, color);
            }
        }
    }

}
