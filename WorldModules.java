package com.example.testclient.modules;

import com.example.testclient.settings.*;
import com.example.testclient.util.*;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.entity.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.chunk.WorldChunk;
import org.lwjgl.glfw.GLFW;

import java.util.*;

final class FullBright extends Module {
    FullBright() { super("FullBright", "World", "Dunkle Bereiche heller sehen (clientseitig)"); }

    static void giveNightVision(MinecraftClient mc) {
        if (mc.player != null) mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 1000, 0, false, false));
    }

    @Override public void onTick() { if (inWorld()) giveNightVision(mc); }
    @Override public void onDisable() { if (inWorld()) mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION); }
}

final class EntityEsp extends Module {
    final BoolSetting players = bool("Spieler", true), mobs = bool("Mobs", true), items = bool("Items", false),
            glow = bool("Glow", true), box = bool("Box", true), names = bool("Namen/Distanz", true);
    final NumSetting range = num("Range", 64, 8, 160, 4);
    private final List<Entity> glowing = new ArrayList<>();

    EntityEsp() { super("ESP", "World", "Spieler/Mobs durch Waende erkennen"); }

    private boolean wanted(Entity e) {
        if (e == mc.player) return false;
        if (e instanceof PlayerEntity) return players.value;
        if (e instanceof ItemEntity) return items.value;
        return e instanceof LivingEntity && mobs.value;
    }

    @Override public void onTick() {
        if (!inWorld()) return;
        glowing.removeIf(e -> { if (e.isRemoved()) return true; return false; });
        double r2 = range.value * range.value;
        for (Entity e : mc.world.getEntities()) {
            boolean on = glow.value && wanted(e) && mc.player.squaredDistanceTo(e) <= r2;
            if (on) { e.setGlowing(true); if (!glowing.contains(e)) glowing.add(e); }
            else if (glowing.remove(e)) e.setGlowing(false);
        }
    }

    @Override public void onDisable() { for (Entity e : glowing) e.setGlowing(false); glowing.clear(); }

    @Override public void onHud(DrawContext ctx) {
        if (!inWorld() || !box.value) return;
        double r2 = range.value * range.value;
        for (Entity e : mc.world.getEntities()) {
            if (!wanted(e) || mc.player.squaredDistanceTo(e) > r2) continue;
            int col = e instanceof PlayerEntity ? 0xFFFF5C7A : e instanceof ItemEntity ? 0xFF5CE1FF : 0xFFFFD25C;
            String label = names.value ? e.getName().getString() + " " + (int) mc.player.distanceTo(e) + "m" : null;
            Boxes.box2d(ctx, e.getBoundingBox(), col, label);
        }
    }
}

final class ChestEsp extends Module {
    final BoolSetting chest = bool("Kisten", true), ender = bool("Endertruhen", true), barrel = bool("Faesser", true),
            shulker = bool("Shulker", true);
    final NumSetting radius = num("Radius", 32, 8, 64, 4), vertical = num("Vertikal", 24, 4, 64, 4);
    private final BlockScanner scanner = new BlockScanner();

    ChestEsp() { super("ChestESP", "World", "Kisten & Behaelter erkennen"); }

    private boolean match(BlockState s) {
        Block b = s.getBlock();
        return (chest.value && (b == Blocks.CHEST || b == Blocks.TRAPPED_CHEST)) || (ender.value && b == Blocks.ENDER_CHEST)
            || (barrel.value && b == Blocks.BARREL) || (shulker.value && b instanceof ShulkerBoxBlock);
    }

    @Override public void onEnable() { scanner.reset(); }
    @Override public void onTick() { if (inWorld()) scanner.tick(mc, (int) radius.value, (int) vertical.value, this::match); }

    @Override public void onHud(DrawContext ctx) {
        if (!inWorld()) return;
        int n = 0;
        for (BlockPos pos : scanner.found) {
            if (n++ > 250) break;
            Block b = mc.world.getBlockState(pos).getBlock();
            int col = b == Blocks.ENDER_CHEST ? 0xFFB26BFF : b == Blocks.BARREL ? 0xFFC98B4B : b instanceof ShulkerBoxBlock ? 0xFFFF7BD0 : 0xFFFFC857;
            String l = (int) Math.sqrt(mc.player.squaredDistanceTo(Vec3d.ofCenter(pos))) + "m";
            Boxes.box2d(ctx, new Box(pos), col, l);
        }
    }
}

final class BlockScan extends Module {
    final NumSetting radius = num("Radius", 24, 8, 48, 4);
    final BoolSetting diamond = bool("Diamant", true), debris = bool("Ancient Debris", true),
            spawner = bool("Spawner", true), chest = bool("Kisten", false), emerald = bool("Smaragd", false);
    private final BlockScanner scanner = new BlockScanner();
    private List<String> lines = new ArrayList<>();
    private List<BlockPos> top = new ArrayList<>();

    BlockScan() { super("BlockScan", "World", "Listet Erze/Spawner in der Naehe mit Koordinaten"); }

    private boolean wanted(BlockState s) {
        Block b = s.getBlock();
        String p = Registries.BLOCK.getId(b).getPath();
        return (diamond.value && p.endsWith("diamond_ore")) || (debris.value && p.equals("ancient_debris"))
            || (spawner.value && b == Blocks.SPAWNER) || (chest.value && (b == Blocks.CHEST || b == Blocks.TRAPPED_CHEST))
            || (emerald.value && p.endsWith("emerald_ore"));
    }

    @Override public void onEnable() { scanner.reset(); lines.clear(); top.clear(); }

    @Override public void onTick() {
        if (!inWorld()) return;
        scanner.tick(mc, (int) radius.value, 24, this::wanted);
        Vec3d pp = mc.player.getPos();
        List<BlockPos> all = new ArrayList<>(scanner.found);
        all.sort(Comparator.comparingDouble(b -> b.getSquaredDistance(pp)));
        top = all.size() > 12 ? new ArrayList<>(all.subList(0, 12)) : all;
        List<String> l = new ArrayList<>();
        for (int i = 0; i < Math.min(6, top.size()); i++) {
            BlockPos b = top.get(i);
            l.add(mc.world.getBlockState(b).getBlock().getName().getString() + "  " + b.getX() + " " + b.getY() + " " + b.getZ()
                + "  (" + (int) Math.sqrt(b.getSquaredDistance(pp)) + "m)");
        }
        lines = l;
    }

    @Override public void onHud(DrawContext ctx) {
        if (!inWorld()) return;
        if (!lines.isEmpty()) HudStack.panel(ctx, "BlockScan (" + scanner.found.size() + ")", lines);
        for (BlockPos b : top) Boxes.box2d(ctx, new Box(b), 0xFF5CF0D2, null);
    }
}

final class Scaffold extends Module {
    final NumSetting delay = num("Delay (Ticks)", 1, 0, 10, 1), expand = num("Expand", 0, 0, 5, 1);
    final BoolSetting autoSwitch = bool("Auto-Slot", true);
    private int cd, oldSlot = -1;

    Scaffold() { super("Scaffold", "World", "Automatisch Bloecke unter dir platzieren"); }

    private int blockSlot(ClientPlayerEntity p) {
        int sel = p.getInventory().getSelectedSlot();
        ItemStack cur = p.getInventory().getStack(sel);
        if (!cur.isEmpty() && cur.getItem() instanceof BlockItem) return sel;
        if (!autoSwitch.value) return -1;
        for (int i = 0; i < 9; i++) {
            ItemStack s = p.getInventory().getStack(i);
            if (!s.isEmpty() && s.getItem() instanceof BlockItem) return i;
        }
        return -1;
    }

    private boolean place(ClientPlayerEntity p, BlockPos pos, int slot) {
        for (Direction d : Direction.values()) {
            BlockPos n = pos.offset(d);
            if (mc.world.getBlockState(n).isReplaceable()) continue;
            Direction face = d.getOpposite();
            Vec3d hit = Vec3d.ofCenter(n).add(face.getOffsetX() * 0.5, face.getOffsetY() * 0.5, face.getOffsetZ() * 0.5);
            if (oldSlot < 0) oldSlot = p.getInventory().getSelectedSlot();
            p.getInventory().setSelectedSlot(slot);
            mc.interactionManager.interactBlock(p, Hand.MAIN_HAND, new BlockHitResult(hit, face, n, false));
            p.swingHand(Hand.MAIN_HAND);
            return true;
        }
        return false;
    }

    @Override public void onTick() {
        if (!inWorld() || mc.currentScreen != null) return;
        if (cd-- > 0) return;
        ClientPlayerEntity p = mc.player;
        int slot = blockSlot(p);
        if (slot < 0) return;
        BlockPos base = p.getBlockPos().down();
        List<BlockPos> list = new ArrayList<>();
        list.add(base);
        double[] d = Move.dir(mc);
        for (int i = 1; i <= (int) expand.value; i++) list.add(base.add((int) Math.round(d[0] * i), 0, (int) Math.round(d[1] * i)));
        for (BlockPos pos : list) {
            if (!mc.world.getBlockState(pos).isReplaceable()) continue;
            if (place(p, pos, slot)) { cd = (int) delay.value; return; }
        }
    }

    @Override public void onDisable() {
        if (inWorld() && oldSlot >= 0) mc.player.getInventory().setSelectedSlot(oldSlot);
        oldSlot = -1;
    }
}

final class InventoryMove extends Module {
    InventoryMove() { super("InventoryMove", "World", "Im Inventar weiterlaufen"); }

    @Override public void onTick() {
        if (!inWorld() || !(mc.currentScreen instanceof HandledScreen<?>)) return;
        long win = mc.getWindow().getHandle();
        KeyBinding[] keys = {mc.options.forwardKey, mc.options.backKey, mc.options.leftKey, mc.options.rightKey,
                mc.options.jumpKey, mc.options.sneakKey, mc.options.sprintKey};
        for (KeyBinding k : keys) {
            int code = k.getDefaultKey().getCode();
            k.setPressed(GLFW.glfwGetKey(win, code) == GLFW.GLFW_PRESS);
        }
    }
}

final class FastPlace extends Module {
    final NumSetting delay = num("Delay (Ticks)", 1, 0, 4, 1);
    private int cd;

    FastPlace() { super("FastPlace", "World", "Bloecke schneller platzieren"); }

    @Override public void onTick() {
        if (!inWorld() || mc.currentScreen != null || !mc.options.useKey.isPressed()) return;
        if (cd-- > 0) return;
        ClientPlayerEntity p = mc.player;
        if (!(p.getMainHandStack().getItem() instanceof BlockItem)) return;
        if (mc.crosshairTarget instanceof BlockHitResult h) {
            mc.interactionManager.interactBlock(p, Hand.MAIN_HAND, h);
            p.swingHand(Hand.MAIN_HAND);
            cd = (int) delay.value;
        }
    }
}

final class Nuker extends Module {
    final NumSetting range = num("Range", 4, 1, 6, 1), perTick = num("Bloecke/Tick (Creative)", 3, 1, 10, 1);
    final BoolSetting oresOnly = bool("Nur Erze", false);

    Nuker() { super("Nuker", "World", "Bloecke automatisch abbauen"); }

    @Override public void onTick() {
        if (!inWorld() || mc.currentScreen != null || mc.interactionManager == null) return;
        ClientPlayerEntity p = mc.player;
        BlockPos c = p.getBlockPos();
        int r = (int) range.value;
        Vec3d eye = p.getEyePos();
        List<BlockPos> cand = new ArrayList<>();
        for (int x = -r; x <= r; x++) for (int y = -r; y <= r; y++) for (int z = -r; z <= r; z++) {
            BlockPos pos = c.add(x, y, z);
            if (eye.squaredDistanceTo(Vec3d.ofCenter(pos)) > r * r) continue;
            BlockState s = mc.world.getBlockState(pos);
            if (s.isAir() || !s.getFluidState().isEmpty() || s.getHardness(mc.world, pos) < 0) continue;
            if (oresOnly.value) {
                String path = Registries.BLOCK.getId(s.getBlock()).getPath();
                if (!path.endsWith("_ore") && !path.equals("ancient_debris")) continue;
            }
            cand.add(pos);
        }
        if (cand.isEmpty()) return;
        cand.sort(Comparator.comparingDouble(b -> eye.squaredDistanceTo(Vec3d.ofCenter(b))));
        boolean creative = mc.interactionManager.getCurrentGameMode().isCreative();
        int n = creative ? (int) perTick.value : 1;
        for (int i = 0; i < Math.min(n, cand.size()); i++) {
            if (creative) mc.interactionManager.attackBlock(cand.get(i), Direction.UP);
            else mc.interactionManager.updateBlockBreakingProgress(cand.get(i), Direction.UP);
        }
        p.swingHand(Hand.MAIN_HAND);
    }
}

final class BaseFinder extends Module {
    final NumSetting radius = num("Chunk-Radius", 8, 2, 16, 1), min = num("Min. Score", 8, 2, 60, 1);
    private record Hit(int x, int y, int z, int score) {}
    private volatile List<Hit> hits = new ArrayList<>();
    private int timer;

    BaseFinder() { super("Base Finder", "World", "Findet Basen anhand von Kisten, Ofen, Betten, Schildern ..."); }

    private static int weight(BlockEntity be) {
        if (be instanceof BeaconBlockEntity) return 4;
        if (be instanceof ShulkerBoxBlockEntity || be instanceof EnchantingTableBlockEntity) return 3;
        if (be instanceof ChestBlockEntity || be instanceof BarrelBlockEntity || be instanceof EnderChestBlockEntity
            || be instanceof AbstractFurnaceBlockEntity || be instanceof HopperBlockEntity
            || be instanceof DispenserBlockEntity || be instanceof BrewingStandBlockEntity) return 2;
        if (be instanceof BedBlockEntity || be instanceof SignBlockEntity || be instanceof BannerBlockEntity
            || be instanceof LecternBlockEntity) return 1;
        return 0;
    }

    private static long key(int x, int z) { return ((long) x << 32) ^ (z & 0xffffffffL); }

    private void scan() {
        int pcx = mc.player.getBlockPos().getX() >> 4, pcz = mc.player.getBlockPos().getZ() >> 4, r = (int) radius.value;
        Map<Long, int[]> raw = new HashMap<>(); // {score, sumY, count}
        for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
            WorldChunk ch = mc.world.getChunkManager().getWorldChunk(pcx + dx, pcz + dz);
            if (ch == null) continue;
            int s = 0, sy = 0, cnt = 0;
            for (BlockEntity be : ch.getBlockEntities().values()) {
                int w = weight(be);
                if (w > 0) { s += w; sy += be.getPos().getY(); cnt++; }
            }
            if (s > 0) raw.put(key(pcx + dx, pcz + dz), new int[]{s, sy, cnt});
        }
        List<int[]> cand = new ArrayList<>(); // {cx, cz, smoothedScore, avgY}
        for (Map.Entry<Long, int[]> e : raw.entrySet()) {
            int cx = (int) (e.getKey() >> 32), cz = (int) (long) e.getKey();
            int sum = 0;
            for (int ax = -1; ax <= 1; ax++) for (int az = -1; az <= 1; az++) {
                int[] v = raw.get(key(cx + ax, cz + az));
                if (v != null) sum += v[0];
            }
            if (sum >= min.value) cand.add(new int[]{cx, cz, sum, e.getValue()[1] / Math.max(1, e.getValue()[2])});
        }
        cand.sort((a, b) -> Integer.compare(b[2], a[2]));
        Set<Long> used = new HashSet<>();
        List<Hit> out = new ArrayList<>();
        for (int[] c : cand) {
            if (used.contains(key(c[0], c[1]))) continue;
            for (int ax = -1; ax <= 1; ax++) for (int az = -1; az <= 1; az++) used.add(key(c[0] + ax, c[1] + az));
            out.add(new Hit(c[0] * 16 + 8, c[3], c[1] * 16 + 8, c[2]));
        }
        hits = out;
    }

    @Override public void onEnable() { hits = new ArrayList<>(); timer = 0; }

    @Override public void onTick() {
        if (!inWorld()) return;
        if (timer-- <= 0) { timer = 40; scan(); }
    }

    @Override public void onHud(DrawContext ctx) {
        if (!inWorld()) return;
        List<Hit> list = new ArrayList<>(hits);
        Vec3d pp = mc.player.getPos();
        list.sort(Comparator.comparingDouble(h -> (h.x - pp.x) * (h.x - pp.x) + (h.z - pp.z) * (h.z - pp.z)));
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < Math.min(5, list.size()); i++) {
            Hit h = list.get(i);
            lines.add(h.x + " " + h.y + " " + h.z + "  Score " + h.score + "  (" + (int) Math.hypot(h.x - pp.x, h.z - pp.z) + "m)");
        }
        if (!lines.isEmpty()) HudStack.panel(ctx, "Base Finder (" + list.size() + ")", lines);
        for (Hit h : list) {
            double[] s = Proj.project(new Vec3d(h.x, h.y + 1, h.z));
            if (s == null) continue;
            String l = "Base? " + h.score + "  " + (int) Math.hypot(h.x - pp.x, h.z - pp.z) + "m";
            int w = Gfx.width(l) + 10;
            Gfx.round(ctx, (int) s[0] - w / 2, (int) s[1] - 8, w, 16, 6, 0xC0301A50);
            Gfx.text(ctx, l, (int) s[0] - w / 2 + 5, (int) s[1] - 4, 0xFFE0C8FF);
        }
    }
}
