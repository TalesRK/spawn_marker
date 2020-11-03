package com.spawnmarker;

import lombok.EqualsAndHashCode;
import lombok.Value;

import javax.annotation.Nullable;
import java.awt.*;

/**
 * Used for serialization of tiles points.
 */
@Value
@EqualsAndHashCode(exclude = { "color", "label" })
public class TilePoint {

    int regionId;

    int regionX;

    int regionY;

    int z;

    @Nullable
    Color color;

    @Nullable
    String label;
}
