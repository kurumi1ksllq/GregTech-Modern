package com.gregtechceu.gtceu.client.renderer.cover;

import com.gregtechceu.gtceu.api.cover.CoverUtil;
import com.gregtechceu.gtceu.client.renderer.pipe.quad.QuadHelper;
import com.gregtechceu.gtceu.utils.GTUtil;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

import org.apache.commons.lang3.tuple.Pair;
import org.joml.Vector3f;

import java.util.EnumMap;

public class CoverRendererValues {

    public static final float OVERLAY_DIST_1 = 0.08f;
    public static final float OVERLAY_DIST_2 = 0.09f;
    // spotless:off
    public static final EnumMap<Direction, AABB> PLATE_AABBS = new EnumMap<>(Direction.class);
    protected static final EnumMap<Direction, Pair<Vector3f, Vector3f>> PLATE_BOXES = new EnumMap<>(Direction.class);
    protected static final EnumMap<Direction, Pair<Vector3f, Vector3f>> OVERLAY_BOXES_1 = new EnumMap<>(Direction.class);
    protected static final EnumMap<Direction, Pair<Vector3f, Vector3f>> OVERLAY_BOXES_2 = new EnumMap<>(Direction.class);
    // spotless:on

    static {
        for (Direction facing : GTUtil.DIRECTIONS) {
            PLATE_AABBS.put(facing, CoverUtil.getCoverPlateBox(facing, 1d / 16));
        }
        for (var value : PLATE_AABBS.entrySet()) {
            // make sure that plates render slightly below any normal block quad
            PLATE_BOXES.put(value.getKey(), QuadHelper.fullOverlay(value.getKey(), value.getValue(), -OVERLAY_DIST_1));
            OVERLAY_BOXES_1.put(value.getKey(),
                    QuadHelper.fullOverlay(value.getKey(), value.getValue(), OVERLAY_DIST_1));
            OVERLAY_BOXES_2.put(value.getKey(),
                    QuadHelper.fullOverlay(value.getKey(), value.getValue(), OVERLAY_DIST_2));
        }
    }
}
