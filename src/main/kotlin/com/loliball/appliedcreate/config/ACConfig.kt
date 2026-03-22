package com.loliball.appliedcreate.config

import net.neoforged.neoforge.common.ModConfigSpec

class ACConfig {
    companion object {
        val SERVER: ServerConfig
        val SPEC: ModConfigSpec

        init {
            val builder = ModConfigSpec.Builder()
            SERVER = ServerConfig(builder)
            SPEC = builder.build()
        }
    }

    class ServerConfig(builder: ModConfigSpec.Builder) {
        val stressAmountPerOperation = builder
            .comment("Amount of stress per operation in ME storage")
            .defineInRange("amountPerOperation", 16384, 1, Int.MAX_VALUE)

        val stressAmountPerByte = builder
            .comment("Amount of stress per byte in ME storage")
            .defineInRange("amountPerByte", 1048576, 1, Int.MAX_VALUE)

        val meGearboxDefaultSpeed = builder
            .comment("Default speed (RPM) for ME Gearbox export mode")
            .defineInRange("defaultSpeed", 32, 1, 256)

        val meGearboxDefaultStress = builder
            .comment("Default stress multiplier for ME Gearbox")
            .defineInRange("defaultStress", 64.0, 0.0, 65536.0)

        val meGearboxMaxStress = builder
            .comment("Maximum stress multiplier for ME Gearbox")
            .defineInRange("maxStress", 65536.0, 1.0, 1000000.0)

        val kineticAePer256Rpm = builder
            .comment("AE energy generated per 256 RPM")
            .defineInRange("aePer256Rpm", 640.0, 1.0, 10000.0)

        val kineticBaseStressSu = builder
            .comment("Base stress units (SU) for Kinetic Energy Acceptor calculation")
            .defineInRange("baseStressSu", 16384, 1, Int.MAX_VALUE)

        val kineticMaxMultiplier = builder
            .comment("Maximum multiplier for Kinetic Energy Acceptor")
            .defineInRange("maxMultiplier", 16, 1, 256)
    }
}
