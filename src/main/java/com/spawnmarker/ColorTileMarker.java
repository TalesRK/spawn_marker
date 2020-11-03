package com.spawnmarker;

import lombok.Value;
import net.runelite.api.coords.WorldPoint;

import javax.annotation.Nullable;
import java.awt.*;

/**
 * Used to denote marked tiles and their colors.
 */
@Value
public class ColorTileMarker {

    WorldPoint worldPoint;

    @Nullable
    Color color;

    @Nullable
    String label;

}
