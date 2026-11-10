package com.example.testclient.util;

import net.minecraft.util.math.Vec3d;

public final class Rot {
    private Rot() {}

    /** @return {yaw, pitch} von 'from' nach 'to' */
    public static float[] to(Vec3d from, Vec3d to) {
        double dx = to.x - from.x, dy = to.y - from.y, dz = to.z - from.z;
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz)));
        return new float[]{yaw, pitch};
    }
}
