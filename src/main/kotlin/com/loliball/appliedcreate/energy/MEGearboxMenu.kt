package com.loliball.appliedcreate.energy

import appeng.menu.AEBaseMenu
import appeng.menu.guisync.GuiSync
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.MenuType
import kotlin.math.abs

class MEGearboxMenu(
    menuType: MenuType<*>,
    id: Int,
    playerInventory: Inventory,
    host: MEGearboxBlockEntity
) : AEBaseMenu(menuType, id, playerInventory, host) {

    companion object {
        const val ACTION_TOGGLE_MODE = "toggleMode"
        const val ACTION_SET_SPEED = "setSpeed"
        const val ACTION_SET_STRESS = "setStress"
    }

    private val gearbox: MEGearboxBlockEntity = host

    @JvmField
    @field:GuiSync(0)
    var currentMode: MEGearboxBlockEntity.Mode = MEGearboxBlockEntity.Mode.EXPORT

    @JvmField
    @field:GuiSync(1)
    var currentConfiguredSpeed: Int = MEGearboxBlockEntity.getDefaultSpeed()

    @JvmField
    @field:GuiSync(2)
    var currentSpeed: Double = 0.0

    @JvmField
    @field:GuiSync(3)
    var currentStress: Double = 0.0

    @JvmField
    @field:GuiSync(4)
    var currentConfiguredStress: Int = MEGearboxBlockEntity.getDefaultStress().toInt()

    init {
        registerClientAction(ACTION_TOGGLE_MODE, ::handleToggleMode)
        @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
        registerClientAction(ACTION_SET_SPEED, java.lang.Integer::class.java) { value -> handleSetSpeed(value.toInt()) }
        @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
        registerClientAction(ACTION_SET_STRESS, java.lang.Integer::class.java) { value -> handleSetStress(value.toInt()) }
    }

    private fun handleToggleMode() {
        gearbox.toggleMode()
    }

    private fun handleSetSpeed(value: Int) {
        val maxSpeed = MEGearboxBlockEntity.getMaxSpeed()
        gearbox.configuredSpeed = value.coerceIn(-maxSpeed, maxSpeed).let { if (it == 0) 1 else it }
    }

    private fun handleSetStress(value: Int) {
        gearbox.configuredStress = value.toFloat().coerceIn(MEGearboxBlockEntity.MIN_STRESS, MEGearboxBlockEntity.getMaxStress())
    }

    override fun broadcastChanges() {
        if (isServerSide) {
            currentMode = gearbox.mode
            currentConfiguredSpeed = gearbox.configuredSpeed
            currentConfiguredStress = gearbox.configuredStress.toInt()
            currentSpeed = abs(gearbox.configuredSpeed).toDouble()
            currentStress = if (gearbox.mode == MEGearboxBlockEntity.Mode.EXPORT) {
                (gearbox.calculateAddedStressCapacity() * abs(gearbox.speed)).toDouble()
            } else {
                (gearbox.getStressApplied() * abs(gearbox.speed)).toDouble()
            }
        }
        super.broadcastChanges()
    }

    fun requestToggleMode() {
        sendClientAction(ACTION_TOGGLE_MODE)
    }

    fun requestSetSpeed(value: Int) {
        sendClientAction(ACTION_SET_SPEED, value)
    }

    fun requestSetStress(value: Int) {
        sendClientAction(ACTION_SET_STRESS, value)
    }
}
