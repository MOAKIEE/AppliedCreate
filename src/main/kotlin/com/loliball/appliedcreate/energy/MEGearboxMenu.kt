package com.loliball.appliedcreate.energy

import appeng.menu.AEBaseMenu
import appeng.menu.guisync.GuiSync
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.MenuType

class MEGearboxMenu(
    menuType: MenuType<*>,
    id: Int,
    playerInventory: Inventory,
    host: MEGearboxBlockEntity
) : AEBaseMenu(menuType, id, playerInventory, host) {

    companion object {
        const val ACTION_TOGGLE_MODE = "toggleMode"
        const val ACTION_SET_SPEED = "setSpeed"
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
    var currentTransferRate: Long = 0

    @JvmField
    @field:GuiSync(5)
    var currentActive: Boolean = false

    init {
        registerClientAction(ACTION_TOGGLE_MODE, ::handleToggleMode)
        @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
        registerClientAction(ACTION_SET_SPEED, java.lang.Integer::class.java) { value -> handleSetSpeed(value.toInt()) }
    }

    private fun handleToggleMode() {
        gearbox.toggleMode()
    }

    private fun handleSetSpeed(value: Int) {
        if (gearbox.mode != MEGearboxBlockEntity.Mode.EXPORT) return
        val maxSpeed = MEGearboxBlockEntity.getMaxSpeed()
        gearbox.configuredSpeed = value.coerceIn(-maxSpeed, maxSpeed).let { if (it == 0) 1 else it }
    }

    override fun broadcastChanges() {
        if (isServerSide) {
            currentMode = gearbox.mode
            currentConfiguredSpeed = gearbox.configuredSpeed
            currentSpeed = gearbox.speed.toDouble()
            currentStress = gearbox.allocatedCapacity()
            currentTransferRate = gearbox.transferRate
            currentActive = gearbox.mainNode.isActive
        }
        super.broadcastChanges()
    }

    fun requestToggleMode() {
        sendClientAction(ACTION_TOGGLE_MODE)
    }

    fun requestSetSpeed(value: Int) {
        sendClientAction(ACTION_SET_SPEED, value)
    }

}
