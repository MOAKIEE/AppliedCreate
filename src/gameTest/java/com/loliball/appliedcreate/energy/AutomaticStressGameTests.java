package com.loliball.appliedcreate.energy;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.StorageCells;
import appeng.blockentity.storage.DriveBlockEntity;
import appeng.core.definitions.AEBlocks;
import com.loliball.appliedcreate.AppliedCreate;
import com.loliball.appliedcreate.p2p.KineticBridgeRegistry;
import com.loliball.appliedcreate.storage.StressKey;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import java.util.function.Consumer;

/** Real Create propagation, native mixins, powered AE grids and stress cells. Never shipped. */
@GameTestHolder("appliedcreate_stress")
@PrefixGameTestTemplate(false)
public final class AutomaticStressGameTests {
    private static final BlockPos POS = new BlockPos(2, 2, 2);
    private static final StressKey KEY = StressKey.getINSTANCE();
    private record Rig(MEGearboxBlockEntity first, MEGearboxBlockEntity second, KineticBlockEntity shaft) {}

    private static void check(boolean ok, String message) {
        if (!ok) throw new GameTestAssertException(message);
    }

    private static Rig rig(GameTestHelper h, long stored, boolean importing) {
        h.setBlock(POS.below(), AEBlocks.CREATIVE_ENERGY_CELL.block());
        h.setBlock(POS.east(), AEBlocks.DRIVE.block());
        var state = AppliedCreate.Companion.getME_GEARBOX_BLOCK().get().defaultBlockState()
                .setValue(DirectionalKineticBlock.FACING, Direction.UP);
        h.setBlock(POS, state);
        h.setBlock(POS.above(), state);
        MEGearboxBlockEntity a = h.getBlockEntity(POS), b = h.getBlockEntity(POS.above());
        a.setConfiguredSpeed(16);
        b.setConfiguredSpeed(16);
        if (importing) { a.toggleMode(); b.toggleMode(); }
        h.setBlock(POS.above(2), AllBlocks.SHAFT.get().defaultBlockState()
                .setValue(BlockStateProperties.AXIS, Direction.Axis.Y));
        var cell = new ItemStack(AppliedCreate.Companion.getSTRESS_CELL_1M().get());
        var storage = StorageCells.getCellInventory(cell, null);
        check(storage != null, "stress storage cell is not registered");
        check(storage.insert(KEY, stored, Actionable.MODULATE, IActionSource.ofMachine(a)) == stored,
                "could not seed stress storage");
        storage.persist();
        DriveBlockEntity drive = h.getBlockEntity(POS.east());
        drive.getInternalInventory().setItemDirect(0, cell);
        return new Rig(a, b, h.getBlockEntity(POS.above(2)));
    }

    private static void ready(Rig r) {
        check(r.first.getMainNode().isActive() && r.second.getMainNode().isActive(), "AE grid is inactive");
        check(r.first.getMainNode().getGrid() == r.second.getMainNode().getGrid(), "ports must share an AE grid");
        check(r.first.hasNetwork() && r.second.hasNetwork() && r.shaft.hasNetwork(), "missing kinetic network");
        check(r.first.getOrCreateNetwork() == r.second.getOrCreateNetwork()
                && r.first.getOrCreateNetwork() == r.shaft.getOrCreateNetwork(), "rotation failed to join ports");
    }

    private static long stored(Rig r) {
        return r.first.getMainNode().getGrid().getStorageService().getInventory()
                .extract(KEY, Long.MAX_VALUE, Actionable.SIMULATE, IActionSource.ofMachine(r.first));
    }

    private static long insert(Rig r, long amount) {
        return r.first.getMainNode().getGrid().getStorageService().getInventory()
                .insert(KEY, amount, Actionable.MODULATE, IActionSource.ofMachine(r.first));
    }

    private static long sent(Rig r) { return r.first.allocatedStress() + r.second.allocatedStress(); }

    private static float published(Rig r, String name) {
        try {
            var field = com.simibubi.create.content.kinetics.KineticNetwork.class.getDeclaredField(name);
            field.setAccessible(true);
            return field.getFloat(r.first.getOrCreateNetwork());
        } catch (ReflectiveOperationException e) { throw new RuntimeException(e); }
    }

    private static KineticBlockEntity millstone(GameTestHelper h) {
        h.setBlock(POS.above(3), AllBlocks.MILLSTONE.get());
        return h.getBlockEntity(POS.above(3));
    }

    @GameTest(template = "empty", timeoutTicks = 150)
    public static void emptyNetworkRotatesWithZeroStoredSU(GameTestHelper h) {
        var r = rig(h, 0, false);
        h.runAfterDelay(100, () -> {
            ready(r);
            check(Math.abs(r.shaft.getSpeed()) == 16, "zero-SU idle shaft did not rotate");
            check(sent(r) == 0 && stored(r) == 0, "idle network consumed/generated stress");
            check(!r.first.isOverStressed() && !r.shaft.isOverStressed(), "zero-load network was overloaded");
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 150)
    public static void addingMachineAndRepeatedUpdatesChargesOnce(GameTestHelper h) {
        var r = rig(h, 1_000_000, false);
        h.runAfterDelay(40, () -> { ready(r); millstone(h); });
        h.runAfterDelay(45, () -> {
            KineticBlockEntity mill = h.getBlockEntity(POS.above(3));
            check(mill.getOrCreateNetwork() == r.first.getOrCreateNetwork(), "millstone did not join network");
            long expected = (long) Math.ceil(mill.calculateStressApplied() * Math.abs(mill.getTheoreticalSpeed()));
            check(expected > 0 && sent(r) == expected && !mill.isOverStressed(), "actual machine load not funded");
            var network = r.first.getOrCreateNetwork();
            long before = stored(r);
            for (int i = 0; i < 100; i++) {
                network.updateCapacity(); network.updateStress(); network.updateNetwork();
                check(!mill.isOverStressed(), "intermediate update overloaded a funded network");
            }
            check(stored(r) == before, "same-tick updates withdrew repeatedly");
            network.updateStressFor(r.shaft, 10);
            check(!mill.isOverStressed() && !r.shaft.isOverStressed(), "new stress was published before allocation");
            check(sent(r) == expected + 160, "additional load was not allocated");
            check(before - stored(r) == 160, "only the same-tick demand increase should be charged");
            network.updateStressFor(r.shaft, 0);
            check(sent(r) == expected, "removed load did not reduce allocation");
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 150)
    public static void fractionalDemandHasNoRoundedUpSpareCapacity(GameTestHelper h) {
        var r = rig(h, 10_000, false);
        h.runAfterDelay(40, () -> {
            ready(r);
            long before = stored(r);
            r.shaft.getOrCreateNetwork().updateStressFor(r.shaft, 0.03125f); // 0.5 SU at 16 RPM
            check(before - stored(r) == 1 && sent(r) == 1, "fractional demand was not prepaid as one stored unit");
            check(r.first.allocatedCapacity() + r.second.allocatedCapacity() == 0.5,
                    "payment rounding exposed unused capacity");
            check(published(r, "currentCapacity") == 0.5f && published(r, "currentStress") == 0.5f,
                    "published network retained spare ME capacity");
            for (int i = 0; i < 100; i++) r.shaft.getOrCreateNetwork().updateNetwork();
            check(before - stored(r) == 1, "fractional demand was charged repeatedly");
            r.shaft.getOrCreateNetwork().updateStressFor(r.shaft, 0.09375f); // 1.5 SU shared by two ports
            check(before - stored(r) == 2, "increased fractional demand was not billed by its increment");
            check(r.first.allocatedCapacity() + r.second.allocatedCapacity() == 1.5,
                    "multiple exporters retained the rounding remainder");
            check(published(r, "currentCapacity") == 1.5f && published(r, "currentStress") == 1.5f,
                    "shared fractional allocation left spare capacity");
        });
        h.runAfterDelay(41, () -> {
            check(published(r, "currentCapacity") == published(r, "currentStress"),
                    "end-of-tick settlement restored rounded-up spare capacity");
            check(!r.shaft.isOverStressed(), "exact fractional supply caused overload");
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 150)
    public static void sharedNearlyFullCellPublishesOnlyActualCollectionLoad(GameTestHelper h) {
        var r = rig(h, 0, true);
        h.setBlock(POS.above(3), AllBlocks.CREATIVE_MOTOR.get().defaultBlockState()
                .setValue(DirectionalKineticBlock.FACING, Direction.DOWN));
        long[] measured = {-1, -1, -1};
        h.runAfterDelay(40, () -> {
            ready(r);
            check(insert(r, Long.MAX_VALUE) > 0, "failed to fill cell");
            check(r.first.getMainNode().getGrid().getStorageService().getInventory().extract(
                    KEY, 10, Actionable.MODULATE, IActionSource.ofMachine(r.first)) == 10, "failed to leave ten free units");
            long before = stored(r);
            r.first.getOrCreateNetwork().updateNetwork();
            NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, ServerTickEvent.Post.class,
                    new Consumer<ServerTickEvent.Post>() {
                        @Override public void accept(ServerTickEvent.Post event) {
                            measured[0] = stored(r) - before;
                            measured[1] = (long) published(r, "currentStress");
                            measured[2] = sent(r);
                            NeoForge.EVENT_BUS.unregister(this);
                        }
                    });
        });
        h.runAfterDelay(41, () -> {
            check(measured[0] == 10, "shared limited space was credited incorrectly: " + measured[0]);
            check(measured[1] == 10 && measured[2] == 10, "published load did not equal actual collection");
            check(!r.shaft.isOverStressed(), "full inventory overloaded native generation");
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 150)
    public static void sharedInventoryShortageStopsAndRecovers(GameTestHelper h) {
        var r = rig(h, 1, false);
        h.runAfterDelay(40, () -> { ready(r); millstone(h); });
        h.runAfterDelay(45, () -> {
            KineticBlockEntity mill = h.getBlockEntity(POS.above(3));
            check(mill.isOverStressed() && mill.getSpeed() == 0 && sent(r) == 0, "unfunded capacity was published");
            check(stored(r) + r.first.storedStress() + r.second.storedStress() == 1,
                    "failed joint payment lost or duplicated reserved units");
            insert(r, 10_000);
            r.first.getOrCreateNetwork().updateStress();
            check(!mill.isOverStressed() && Math.abs(mill.getSpeed()) == 16, "refill did not recover immediately");
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 150)
    public static void meOutputCannotFeedAnImporter(GameTestHelper h) {
        var r = rig(h, 100_000, false);
        r.second.toggleMode();
        h.runAfterDelay(100, () -> {
            ready(r);
            check(Math.abs(r.shaft.getSpeed()) == 16, "empty loop did not rotate");
            check(stored(r) == 100_000 && sent(r) == 0 && r.second.getTransferRate() == 0,
                    "ME-generated capacity was harvested as native generation");
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 150)
    public static void twoImportersShareNativeSurplus(GameTestHelper h) {
        var r = rig(h, 0, true);
        h.setBlock(POS.above(3), AllBlocks.CREATIVE_MOTOR.get().defaultBlockState()
                .setValue(DirectionalKineticBlock.FACING, Direction.DOWN));
        CreativeMotorBlockEntity motor = h.getBlockEntity(POS.above(3));
        final long[] before = new long[2];
        h.runAfterDelay(40, () -> {
            ready(r);
            var network = r.first.getOrCreateNetwork();
            check(network == motor.getOrCreateNetwork(), "native source is not connected");
            network.updateStressFor(r.shaft, 10);
            long nativeSU = (long) (motor.calculateAddedStressCapacity() * Math.abs(motor.getGeneratedSpeed()));
            long perPort = (long) (MEGearboxBlockEntity.Companion.getMaxStress() * Math.abs(r.first.getTheoreticalSpeed()));
            before[0] = stored(r);
            before[1] = Math.min(nativeSU - 160, perPort * 2);
            check(before[1] > 0 && sent(r) == before[1], "collectors did not subtract ordinary load");
            check(published(r, "currentCapacity") == published(r, "currentStress"),
                    "collected surplus remained available as unclaimed capacity");
        });
        h.runAfterDelay(41, () -> {
            check(stored(r) - before[0] == before[1], "shared importers duplicated or lost native surplus");
            check(r.first.getTransferRate() + r.second.getTransferRate() == before[1], "wrong actual import rate");
            check(!r.shaft.isOverStressed(), "automatic collectors overloaded native generation");
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 150)
    public static void savedNetworkDoesNotRetainPaidCapacity(GameTestHelper h) {
        var r = rig(h, 1_000_000, false);
        h.runAfterDelay(40, () -> millstone(h));
        h.runAfterDelay(45, () -> {
            ready(r);
            check(sent(r) > 0, "test requires paid capacity");
            for (var member : r.first.getOrCreateNetwork().members.keySet()) {
                var tag = member.saveWithoutMetadata(h.getLevel().registryAccess());
                check(tag.getCompound("Network").getFloat("Capacity") == 0,
                        "paid ME capacity persisted as a free unloaded source");
                if (member instanceof MEGearboxBlockEntity) {
                    check(!tag.getCompound("Network").contains("AddedCapacity"), "gearbox persisted allocated capacity");
                }
            }
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 150)
    public static void rejectedJointReservationSurvivesDropAndReload(GameTestHelper h) {
        var r = rig(h, 1, false);
        h.runAfterDelay(40, () -> {
            ready(r);
            r.first.getOrCreateNetwork().updateStressFor(r.shaft, 100);
            var port = r.first.storedStress() > 0 ? r.first : r.second;
            check(port.storedStress() == 1 && sent(r) == 0, "expected a one-unit unspent reservation");
            var drops = Block.getDrops(port.getBlockState(), h.getLevel(), port.getBlockPos(), port);
            var drop = drops.stream().filter(s -> s.is(AppliedCreate.Companion.getME_GEARBOX_BLOCK().asItem()))
                    .findFirst().orElseThrow();
            var data = drop.get(DataComponents.BLOCK_ENTITY_DATA);
            check(data != null && data.copyTag().getLong("StressBuffer") == 1, "block drop lost reserved stress");
            var restored = new MEGearboxBlockEntity(AppliedCreate.Companion.getME_GEARBOX_BE().get(),
                    port.getBlockPos(), port.getBlockState());
            restored.read(data.copyTag(), h.getLevel().registryAccess(), false);
            check(restored.storedStress() == 1 && restored.allocatedStress() == 0, "reload lost buffer or restored free capacity");
            h.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void oldMultiplierIsIgnored(GameTestHelper h) {
        var state = AppliedCreate.Companion.getME_GEARBOX_BLOCK().get().defaultBlockState();
        var port = new MEGearboxBlockEntity(AppliedCreate.Companion.getME_GEARBOX_BE().get(), POS, state);
        var old = new CompoundTag();
        old.putString("GearboxMode", "IMPORT");
        old.putInt("GearboxSpeed", -32);
        old.putFloat("GearboxStress", 1_000_000);
        port.read(old, h.getLevel().registryAccess(), false);
        check(port.getMode() == MEGearboxBlockEntity.Mode.IMPORT && port.getConfiguredSpeed() == -32,
                "migration lost mode or output speed");
        check(port.calculateAddedStressCapacity() == 0 && port.calculateStressApplied() == 0,
                "legacy multiplier leaked into automatic allocation");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 150)
    public static void nativeCapacityChangesBeforeOverloadAndNoStaleSource(GameTestHelper h) {
        var r = rig(h, 0, true);
        h.setBlock(POS.above(3), AllBlocks.CREATIVE_MOTOR.get().defaultBlockState()
                .setValue(DirectionalKineticBlock.FACING, Direction.DOWN));
        CreativeMotorBlockEntity motor = h.getBlockEntity(POS.above(3));
        h.runAfterDelay(40, () -> {
            ready(r);
            var network = r.first.getOrCreateNetwork();
            network.updateCapacityFor(motor, 100);
            network.updateStressFor(r.shaft, 80);
            check(sent(r) == 320 && !r.shaft.isOverStressed(), "collectors failed to release load before overload");
            network.updateCapacityFor(motor, 90);
            check(sent(r) == 160 && !r.shaft.isOverStressed(), "falling native capacity overloaded an affordable load");
            network.updateCapacityFor(motor, 0);
            check(sent(r) == 0 && r.shaft.isOverStressed(), "collectors retained capacity without native funding");
            network.updateCapacityFor(motor, 100);
            check(sent(r) == 320 && !r.shaft.isOverStressed(), "native source recovery failed");
            // Keep the stale source map entry deliberately: loaded identity must still be checked.
            motor.setRemoved();
            network.updateCapacity();
            check(sent(r) == 0, "removed source retained import allocation: " + sent(r));
            check(r.shaft.getSpeed() == 0, "removed source retained machine rotation: " + r.shaft.getSpeed());
            motor.clearRemoved();
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 150)
    public static void bridgeLoadJoinsAndLeavesOneAllocation(GameTestHelper h) {
        var r = rig(h, 1_000_000, false);
        var remote = POS.east(3);
        h.setBlock(remote, AllBlocks.MILLSTONE.get());
        KineticBlockEntity mill = h.getBlockEntity(remote);
        var group = h.absolutePos(POS.north());
        var bridge = KineticBridgeRegistry.INSTANCE;
        h.runAfterDelay(40, () -> {
            ready(r);
            bridge.registerEndpoint(h.getLevel(), group, r.shaft.getBlockPos());
            bridge.registerEndpoint(h.getLevel(), group, mill.getBlockPos());
        });
        h.runAfterDelay(45, () -> {
            check(mill.hasNetwork() && mill.getOrCreateNetwork() == r.first.getOrCreateNetwork(),
                    "P2P virtual edge did not merge the remote kinetic network");
            long load = (long) Math.ceil(mill.calculateStressApplied() * Math.abs(mill.getTheoreticalSpeed()));
            check(load > 0 && sent(r) == load && !mill.isOverStressed(), "remote load was not allocated once");
            bridge.unregisterEndpoint(h.getLevel(), group, mill.getBlockPos());
            bridge.unregisterEndpoint(h.getLevel(), group, r.shaft.getBlockPos());
        });
        h.runAfterDelay(50, () -> {
            check(sent(r) == 0 && mill.getSpeed() == 0, "disconnected remote load retained allocation or rotation");
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 150)
    public static void poweredGridLossRemovesPaidCapacity(GameTestHelper h) {
        var r = rig(h, 1_000_000, false);
        h.runAfterDelay(40, () -> millstone(h));
        h.runAfterDelay(45, () -> {
            check(sent(r) > 0, "expected a funded network");
            h.setBlock(POS.below(), net.minecraft.world.level.block.Blocks.AIR);
        });
        h.runAfterDelay(100, () -> {
            check(!r.first.getMainNode().isActive() && !r.second.getMainNode().isActive(), "ME grid retained power");
            check(sent(r) == 0 && r.shaft.getSpeed() == 0,
                    "offline ports retained capacity=" + sent(r) + " or rotation=" + r.shaft.getSpeed());
            h.succeed();
        });
    }
}
