package com.loliball.appliedcreate.config

import net.minecraftforge.common.ForgeConfigSpec

/**
 * Applied Create server-side configuration.
 *
 * All values are read once at startup (no hot-reload required).
 * If Configured mod is installed, players can edit these in-game via its GUI.
 */
object ACConfig {

    val SPEC: ForgeConfigSpec
    val SERVER: ServerConfig

    init {
        val builder = ForgeConfigSpec.Builder()
        SERVER = ServerConfig(builder)
        SPEC = builder.build()
    }

    class ServerConfig(builder: ForgeConfigSpec.Builder) {

        // ── Stress Key Type ──
        val stressAmountPerOperation: ForgeConfigSpec.IntValue
        val stressAmountPerByte: ForgeConfigSpec.IntValue

        // ── ME Gearbox ──
        val meGearboxDefaultSpeed: ForgeConfigSpec.IntValue
        val meGearboxDefaultStress: ForgeConfigSpec.DoubleValue
        val meGearboxMaxStress: ForgeConfigSpec.DoubleValue

        // ── Kinetic Energy Acceptor ──
        val keaAePer256Rpm: ForgeConfigSpec.DoubleValue
        val keaBaseStressSu: ForgeConfigSpec.IntValue
        val keaMaxMultiplier: ForgeConfigSpec.IntValue

        init {
            builder.comment("Stress Key Type settings")
                .push("stress_key_type")

            stressAmountPerOperation = builder
                .comment("Amount of stress units per AE2 operation (default: 16384)")
                .defineInRange("amountPerOperation", 1024 * 16, 1, Int.MAX_VALUE)

            stressAmountPerByte = builder
                .comment("Amount of stress units per byte of storage (default: 1048576)")
                .defineInRange("amountPerByte", 1024 * 1024, 1, Int.MAX_VALUE)

            builder.pop()

            builder.comment("ME Gearbox settings")
                .push("me_gearbox")

            meGearboxDefaultSpeed = builder
                .comment("Default RPM for export mode (default: 32)")
                .defineInRange("defaultSpeed", 32, 1, 256)

            meGearboxDefaultStress = builder
                .comment("Default stress capacity/impact in SU (default: 64.0)")
                .defineInRange("defaultStress", 64.0, 0.0, 65536.0)

            meGearboxMaxStress = builder
                .comment("Maximum configurable stress in SU (default: 65536.0)")
                .defineInRange("maxStress", 65536.0, 1.0, 1048576.0)

            builder.pop()

            builder.comment("Kinetic Energy Acceptor settings")
                .push("kinetic_energy_acceptor")

            keaAePer256Rpm = builder
                .comment("AE power generated per tick at 256 RPM with 1x multiplier (default: 640.0)")
                .defineInRange("aePer256Rpm", 640.0, 0.0, 1000000.0)

            keaBaseStressSu = builder
                .comment("Base stress impact in SU at max RPM (default: 16384)")
                .defineInRange("baseStressSu", 16384, 0, Int.MAX_VALUE)

            keaMaxMultiplier = builder
                .comment("Maximum scroll multiplier value (default: 16)")
                .defineInRange("maxMultiplier", 16, 1, 256)

            builder.pop()
        }
    }
}
