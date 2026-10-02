package com.loliball.appliedcreate.energy;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.StorageCells;
import appeng.blockentity.storage.DriveBlockEntity;
import appeng.core.definitions.AEBlocks;
import com.loliball.appliedcreate.AppliedCreate;
import com.loliball.appliedcreate.storage.StressKey;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Optional addon tests. Every FE move debits the real source before crediting the real receiver. */
@GameTestHolder("appliedcreate_compat")
public final class StressConversionGameTests {
    private static final Logger LOG = LoggerFactory.getLogger("AppliedCreateStressCompat");
    private static final BlockPos OUT = new BlockPos(3, 2, 3), IN = new BlockPos(9, 2, 3);
    private static final long SEED = 20_000_000;
    private static final StressKey KEY = StressKey.getINSTANCE();

    private record Rig(MEGearboxBlockEntity out, MEGearboxBlockEntity in, KineticBlockEntity generator,
                       KineticBlockEntity motor, BlockEntity energyOutput) {}

    private static boolean exists(String id) {
        return BuiltInRegistries.BLOCK.containsKey(ResourceLocation.parse(id));
    }

    private static Block block(String id) {
        check(exists(id), "missing test dependency " + id);
        return BuiltInRegistries.BLOCK.get(ResourceLocation.parse(id));
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new GameTestAssertException(message);
    }

    @GameTestGenerator
    public static List<TestFunction> conversions() {
        List<TestFunction> tests = new ArrayList<>();
        String[] generators = {"createaddition:alternator", "create_new_age:generator_coil",
                "create_better_motors:andesite_alternator", "create_better_motors:brass_alternator"};
        String[] motors = {"createaddition:electric_motor", "create_new_age:basic_motor",
                "create_new_age:reinforced_motor", "create_better_motors:starter_motor",
                "create_better_motors:basic_motor", "create_better_motors:hardened_motor",
                "create_better_motors:nitro_motor"};
        for (String generator : generators) {
            if (!exists(generator)) continue;
            for (String motor : motors) {
                if (!exists(motor)) continue;
                String name = "closed_loop_" + generator.replace(':', '_') + "_to_" + motor.replace(':', '_');
                tests.add(new TestFunction("conversion", "appliedcreate_compat:" + name,
                        "appliedcreate_compat:empty", 5_100, 1, true, h -> closedLoop(h, generator, motor, false)));
            }
        }
        if (exists("createaddition:alternator") && exists("createaddition:electric_motor")) {
            tests.add(new TestFunction("conversion", "appliedcreate_compat:cca_pulsed_closed_loop",
                    "appliedcreate_compat:empty", 5_100, 1, true,
                    h -> closedLoop(h, "createaddition:alternator", "createaddition:electric_motor", true)));
        }
        for (String motor : motors) {
            if (!exists(motor) || motor.startsWith("create_new_age:")) continue;
            tests.add(new TestFunction("conversion", "appliedcreate_compat:pulse_" + motor.replace(':', '_'),
                    "appliedcreate_compat:empty", 150, 1, true, h -> motorPulse(h, motor)));
        }
        if (exists("createaddition:electric_motor")) {
            tests.add(new TestFunction("conversion", "appliedcreate_compat:standalone_cca_pulse",
                    "appliedcreate_compat:empty", 150, 1, true,
                    h -> standalonePulse(h, "createaddition:electric_motor", "createaddition:alternator", 961)));
        }
        if (exists("create_better_motors:starter_motor")) {
            tests.add(new TestFunction("conversion", "appliedcreate_compat:standalone_better_pulse",
                    "appliedcreate_compat:empty", 150, 1, true,
                    h -> standalonePulse(h, "create_better_motors:starter_motor",
                            "create_better_motors:andesite_alternator", 1921)));
        }
        return tests;
    }

    private static void standalonePulse(GameTestHelper h, String motorId, String generatorId, int seed) {
        // No ME gearbox, cell, AE grid or Applied Create conversion is present in this control.
        h.setBlock(OUT, block(motorId).defaultBlockState().setValue(DirectionalKineticBlock.FACING, Direction.UP));
        h.setBlock(OUT.above(), AllBlocks.SHAFT.get().defaultBlockState().setValue(BlockStateProperties.AXIS, Direction.Axis.Y));
        h.setBlock(OUT.above(2), block(generatorId).defaultBlockState().setValue(DirectionalKineticBlock.FACING, Direction.DOWN));
        KineticBlockEntity motor = h.getBlockEntity(OUT), generator = h.getBlockEntity(OUT.above(2));
        try { motor.getClass().getMethod("setRPM", float.class).invoke(motor, 256f); }
        catch (ReflectiveOperationException e) { throw new RuntimeException(e); }
        h.runAtTickTime(30, () -> check(energy(motor).receiveEnergy(seed, false) == seed, "could not seed standalone motor"));
        h.runAtTickTime(100, () -> {
            int produced = energy(generator).extractEnergy(Integer.MAX_VALUE, false);
            LOG.info("STANDALONE_CONVERSION motor={} generator={} seedFE={} producedFE={} remainingMotorFE={} meBlocks=0",
                    motorId, generatorId, seed, produced, energy(motor).getEnergyStored());
            check(produced > seed, "known upstream pulse gain was not reproduced without ME");
            h.succeed();
        });
    }

    /** Check real inventory writes against the motor's independently calculated physical capacity. */
    private static final class CollectionAudit implements AutoCloseable {
        final Rig rig;
        long before, paidBefore, expected, ticks, totalExpected, totalActual;
        String failure;
        boolean sample;
        final Consumer<ServerTickEvent.Post> pre = this::beforeCollection;
        final Consumer<ServerTickEvent.Post> post = this::afterCollection;

        CollectionAudit(Rig rig) {
            this.rig = rig;
            NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, false, ServerTickEvent.Post.class, pre);
            NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, ServerTickEvent.Post.class, post);
        }

        private long balance() { return stored(rig) + rig.out.storedStress() + rig.in.storedStress(); }

        private long paidThisTick(int tick) {
            try {
                var budgetField = MEGearboxBlockEntity.class.getDeclaredField("budget");
                budgetField.setAccessible(true);
                Object budget = budgetField.get(rig.out);
                var tickField = StressBudget.class.getDeclaredField("tick");
                var paidField = StressBudget.class.getDeclaredField("committed");
                tickField.setAccessible(true);
                paidField.setAccessible(true);
                return tickField.getLong(budget) == tick ? paidField.getLong(budget) : 0;
            } catch (ReflectiveOperationException e) { throw new RuntimeException(e); }
        }

        private void beforeCollection(ServerTickEvent.Post event) {
            sample = rig.out.getMainNode().isActive() && rig.in.getMainNode().isActive();
            if (!sample) return;
            // Force any pending demand change to settle before measuring the import-only write.
            if (rig.out.hasNetwork()) rig.out.getOrCreateNetwork().updateNetwork();
            if (rig.in.hasNetwork()) rig.in.getOrCreateNetwork().updateNetwork();
            before = balance();
            paidBefore = paidThisTick(event.getServer().getTickCount());
            boolean connected = rig.in.hasNetwork() && rig.motor.hasNetwork()
                    && rig.in.getOrCreateNetwork() == rig.motor.getOrCreateNetwork();
            expected = connected && rig.in.getSpeed() != 0 && !rig.in.isOverStressed()
                    ? Math.min((long) Math.floor(rig.motor.calculateAddedStressCapacity() * Math.abs((double) rig.motor.getGeneratedSpeed())),
                            (long) Math.floor(MEGearboxBlockEntity.Companion.getMaxStress() * Math.abs((double) rig.in.getTheoreticalSpeed())))
                    : 0;
        }

        private void afterCollection(ServerTickEvent.Post event) {
            if (!sample) return;
            // A previously unfunded exporter may recover using this very tick's deposit,
            // depending on network iteration order. Include that real, once-only payment.
            long paidAfter = paidThisTick(event.getServer().getTickCount());
            long actual = balance() - before + (paidAfter - paidBefore);
            ticks++;
            totalExpected += expected;
            totalActual += actual;
            if (actual != expected && failure == null) {
                failure = "collection mismatched real source capacity: expected=" + expected + " actual=" + actual;
            }
        }

        @Override public void close() {
            NeoForge.EVENT_BUS.unregister(pre);
            NeoForge.EVENT_BUS.unregister(post);
        }
    }

    private static Rig rig(GameTestHelper h, String generatorId, String motorId) {
        // Creative AE cells power only AE idle/transfer costs, never any FE motor or stress inventory.
        for (int x = OUT.getX(); x <= IN.getX(); x++) {
            h.setBlock(new BlockPos(x, 1, 3), AEBlocks.CREATIVE_ENERGY_CELL.block());
        }
        var gearbox = AppliedCreate.Companion.getME_GEARBOX_BLOCK().get().defaultBlockState()
                .setValue(DirectionalKineticBlock.FACING, Direction.UP);
        h.setBlock(OUT, gearbox);
        h.setBlock(IN, gearbox);
        MEGearboxBlockEntity out = h.getBlockEntity(OUT), in = h.getBlockEntity(IN);
        out.setConfiguredSpeed(256);
        in.toggleMode();
        h.setBlock(OUT.east(), AEBlocks.DRIVE.block());
        var cell = new ItemStack(AppliedCreate.Companion.getSTRESS_CELL_1M().get());
        var storage = StorageCells.getCellInventory(cell, null);
        check(storage != null && storage.insert(KEY, SEED, Actionable.MODULATE, IActionSource.ofMachine(out)) == SEED,
                "cannot seed finite stress cell");
        storage.persist();
        DriveBlockEntity drive = h.getBlockEntity(OUT.east());
        drive.getInternalInventory().setItemDirect(0, cell);

        KineticBlockEntity generator;
        BlockEntity energyOutput;
        var shaft = AllBlocks.SHAFT.get().defaultBlockState().setValue(BlockStateProperties.AXIS, Direction.Axis.Y);
        // Keep FE outputs away from the gearbox's AE energy-input capability.
        h.setBlock(OUT.above(), shaft);
        h.setBlock(IN.above(), shaft);
        if (generatorId.equals("create_new_age:generator_coil")) {
            h.setBlock(OUT.above(2), block("create_new_age:carbon_brushes").defaultBlockState()
                    .setValue(DirectionalKineticBlock.FACING, Direction.UP));
            h.setBlock(OUT.above(3), block(generatorId).defaultBlockState()
                    .setValue(BlockStateProperties.AXIS, Direction.Axis.Y));
            generator = h.getBlockEntity(OUT.above(3));
            energyOutput = h.getBlockEntity(OUT.above(2));
            try {
                @SuppressWarnings("unchecked")
                List<BlockPos> magnets = (List<BlockPos>) generator.getClass().getField("magnetPositions").get(generator);
                for (BlockPos pos : magnets) h.getLevel().setBlockAndUpdate(pos, block("create_new_age:redstone_magnet").defaultBlockState());
            } catch (ReflectiveOperationException e) { throw new RuntimeException(e); }
        } else {
            h.setBlock(OUT.above(2), block(generatorId).defaultBlockState()
                    .setValue(DirectionalKineticBlock.FACING, Direction.DOWN));
            generator = h.getBlockEntity(OUT.above(2));
            energyOutput = generator;
        }
        h.setBlock(IN.above(2), block(motorId).defaultBlockState()
                .setValue(DirectionalKineticBlock.FACING, Direction.DOWN));
        KineticBlockEntity motor = h.getBlockEntity(IN.above(2));
        try {
            if (motorId.startsWith("create_new_age:")) {
                var behaviour = motor.getClass().getField("speedBehavior").get(motor);
                behaviour.getClass().getMethod("setValue", int.class).invoke(behaviour, 128);
            } else {
                motor.getClass().getMethod("setRPM", float.class).invoke(motor, 256f);
            }
        } catch (ReflectiveOperationException e) { throw new RuntimeException(e); }
        return new Rig(out, in, generator, motor, energyOutput);
    }

    private static IEnergyStorage energy(BlockEntity be) {
        var storage = be.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, be.getBlockPos(), Direction.NORTH);
        check(storage != null, "missing FE capability at " + be.getBlockPos());
        return storage;
    }

    private static long stored(Rig r) {
        return r.out.getMainNode().getGrid().getStorageService().getInventory()
                .extract(KEY, Long.MAX_VALUE, Actionable.SIMULATE, IActionSource.ofMachine(r.out));
    }

    private static void motorPulse(GameTestHelper h, String motorId) {
        var r = rig(h, "createaddition:alternator", motorId);
        r.out.toggleMode();
        long[] seed = {0}, initial = {0}, running = {0};
        h.runAtTickTime(30, () -> {
            initial[0] = stored(r);
            try {
                int cost = (int) r.motor.getClass().getMethod("getEnergyConsumptionRate", float.class).invoke(null, 256f);
                int wanted = cost * 2 + 1;
                while (seed[0] < wanted) {
                    int accepted = energy(r.motor).receiveEnergy((int) (wanted - seed[0]), false);
                    check(accepted > 0, "cannot seed pulse motor");
                    seed[0] += accepted;
                }
            } catch (ReflectiveOperationException e) { throw new RuntimeException(e); }
        });
        h.onEachTick(() -> {
            if (h.getTick() > 30 && Math.abs(r.motor.getSpeed()) > 0) running[0]++;
        });
        h.runAtTickTime(100, () -> {
            LOG.info("STRESS_PULSE motor={} seedFE={} remainingFE={} harvestedSU={} rotatingTicks={}",
                    motorId, seed[0], energy(r.motor).getEnergyStored(), stored(r) - initial[0], running[0]);
            check(running[0] > 0 && seed[0] > energy(r.motor).getEnergyStored(), "pulse motor never consumed FE");
            h.succeed();
        });
    }

    private static void closedLoop(GameTestHelper h, String generatorId, String motorId, boolean burst) {
        var r = rig(h, generatorId, motorId);
        var audit = new CollectionAudit(r);
        long[] transferred = {0}, peak = {SEED}, samples = {0}, machineTicks = {0};
        long[] nextBurst = {30};
        if (burst) r.out.toggleMode();
        h.onEachTick(() -> {
            if (h.getTick() < 30 || h.getTick() > 5_000) return;
            check(r.out.getMainNode().isActive() && r.in.getMainNode().isActive(), "AE grid inactive");
            check(r.out.getMainNode().getGrid() == r.in.getMainNode().getGrid(), "stress inventory must be shared");
            IEnergyStorage from = energy(r.energyOutput), to = energy(r.motor);
            int canReceive = to.receiveEnergy(Integer.MAX_VALUE, true);
            if (burst) {
                // A real buffer/controller can deliver exactly two paid ticks plus one FE
                // only while the motor is stopped. No energy is created by this transport.
                canReceive = h.getTick() >= nextBurst[0] && r.motor.getGeneratedSpeed() == 0 && to.getEnergyStored() == 0
                        && from.extractEnergy(961, true) == 961 ? Math.min(961, canReceive) : 0;
                boolean generate = h.getTick() < 1_000 && h.getTick() >= nextBurst[0]
                        && canReceive == 0 && r.motor.getGeneratedSpeed() == 0;
                if ((r.out.getMode() == MEGearboxBlockEntity.Mode.EXPORT) != generate) r.out.toggleMode();
            }
            int withdrawn = from.extractEnergy(canReceive, false);
            check(withdrawn >= 0 && withdrawn <= canReceive, "invalid real FE extraction");
            int accepted = to.receiveEnergy(withdrawn, false);
            check(accepted == withdrawn, "FE transport discarded units");
            transferred[0] += accepted;
            if (burst && accepted > 0) nextBurst[0] = h.getTick() + 30;
            long remaining = stored(r);
            peak[0] = Math.max(peak[0], remaining);
            samples[0]++;
            if (Math.abs(r.motor.getSpeed()) > 0) machineTicks[0]++;
            // Diagnostic measurements are retained even for a failing conservation assertion.
            if (h.getTick() % 1_000 == 0) {
                LOG.info("STRESS_LOOP generator={} motor={} tick={} initial={} stored={} peak={} feTransferred={} out={} in={} motorFE={}",
                        generatorId, motorId, h.getTick(), SEED, remaining, peak[0], transferred[0],
                        r.out.allocatedStress(), r.in.allocatedStress(), to.getEnergyStored());
            }
        });
        // Settle motor/generator FE buffers after shutting the generator down. Otherwise a
        // loop can hide its profit in a charged battery while its stored SU appears constant.
        h.runAtTickTime(1_000, () -> {
            if (r.out.getMode() == MEGearboxBlockEntity.Mode.EXPORT) r.out.toggleMode();
        });
        h.runAtTickTime(5_001, () -> {
            audit.close();
            LOG.info("STRESS_RESULT generator={} motor={} burst={} initial={} final={} peak={} feTransferred={} motorTicks={} samples={}",
                    generatorId, motorId, burst, SEED, stored(r), peak[0], transferred[0], machineTicks[0], samples[0]);
            check(transferred[0] > 0 && machineTicks[0] > 0, "loop was never powered; fixture did not test conversion");
            LOG.info("STORAGE_AUDIT generator={} motor={} burst={} ticks={} expectedSU={} collectedSU={} upstreamGain={}",
                    generatorId, motorId, burst, audit.ticks, audit.totalExpected, audit.totalActual, peak[0] > SEED);
            check(audit.failure == null, audit.failure == null ? "" : audit.failure);
            check(audit.ticks > 100 && audit.totalActual > 0, "no live source collection was audited");
            h.succeed();
        });
    }
}
