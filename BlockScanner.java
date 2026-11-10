package com.example.testclient.util;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** Scannt pro Tick nur eine Y-Ebene, damit es nicht laggt. */
public class BlockScanner {
    public volatile List<BlockPos> found = new ArrayList<>();
    private List<BlockPos> building = new ArrayList<>();
    private int layer = 0;

    public void tick(MinecraftClient mc, int r, int vr, Predicate<BlockState> test) {
        ClientWorld w = mc.world;
        if (w == null || mc.player == null) return;
        BlockPos c = mc.player.getBlockPos();
        int y = c.getY() - vr + layer;
        if (y >= w.getBottomY() && y < w.getBottomY() + w.getHeight()) {
            BlockPos.Mutable m = new BlockPos.Mutable();
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    int bx = c.getX() + x, bz = c.getZ() + z;
                    if (!w.getChunkManager().isChunkLoaded(bx >> 4, bz >> 4)) continue;
                    m.set(bx, y, bz);
                    if (test.test(w.getBlockState(m))) building.add(m.toImmutable());
                }
            }
        }
        if (++layer > 2 * vr) {
            layer = 0;
            found = building;
            building = new ArrayList<>();
        }
    }

    public void reset() { found = new ArrayList<>(); building = new ArrayList<>(); layer = 0; }
}
