package com.loliball.appliedcreate.ponder

import com.loliball.appliedcreate.AppliedCreate
import net.createmod.catnip.utility.Pointing
import net.createmod.ponder.api.scene.SceneBuilder
import net.createmod.ponder.api.scene.SceneBuildingUtil
import net.minecraft.core.Direction
import net.minecraft.world.item.ItemStack

object PatternProviderScenes {

    fun andesitePatternProvider(scene: SceneBuilder, util: SceneBuildingUtil) {
        scene.title("andesite_pattern_provider", "Andesite Pattern Provider")
        scene.configureBasePlate(0, 0, 5)
        scene.showBasePlate()
        scene.idle(10)

        val providerPos = util.grid().at(2, 1, 2)
        val providerSelection = util.select().position(providerPos)

        scene.world().showSection(providerSelection, Direction.DOWN)
        scene.idle(20)

        scene.overlay().showText(60)
            .text("The Andesite Pattern Provider connects AE2 automation to Create Mechanical Crafters")
            .pointAt(util.vector().blockSurface(providerPos, Direction.WEST))
            .placeNearTarget()
            .attachKeyFrame()
        scene.idle(70)

        scene.overlay().showText(60)
            .text("It can hold up to 9 AE2 processing patterns")
            .pointAt(util.vector().blockSurface(providerPos, Direction.WEST))
            .placeNearTarget()
            .attachKeyFrame()
        scene.idle(70)

        scene.overlay().showControls(
            util.vector().blockSurface(providerPos, Direction.NORTH),
            Pointing.RIGHT,
            40
        ).rightClick()
        scene.idle(10)

        scene.overlay().showText(50)
            .text("Right-click to open the GUI and insert processing patterns")
            .pointAt(util.vector().blockSurface(providerPos, Direction.NORTH))
            .placeNearTarget()
        scene.idle(60)

        scene.overlay().showText(80)
            .text("Place it adjacent to your Mechanical Crafter array. It will distribute ingredients to the correct crafter slots automatically")
            .pointAt(util.vector().blockSurface(providerPos, Direction.UP))
            .placeNearTarget()
            .attachKeyFrame()
        scene.idle(90)

        scene.overlay().showControls(
            util.vector().blockSurface(providerPos, Direction.NORTH),
            Pointing.RIGHT,
            40
        ).rightClick()
            .withItem(ItemStack(AppliedCreate.BRASS_PATTERN_PROVIDER_UPGRADE_ITEM.get()))
        scene.idle(10)

        scene.overlay().showText(60)
            .text("Use a Brass Pattern Provider Upgrade to upgrade it to 36 pattern slots")
            .pointAt(util.vector().blockSurface(providerPos, Direction.NORTH))
            .placeNearTarget()
            .attachKeyFrame()
        scene.idle(70)

        scene.markAsFinished()
    }

    fun brassPatternProvider(scene: SceneBuilder, util: SceneBuildingUtil) {
        scene.title("brass_pattern_provider", "Brass Pattern Provider")
        scene.configureBasePlate(0, 0, 5)
        scene.showBasePlate()
        scene.idle(10)

        val providerPos = util.grid().at(2, 1, 2)
        val providerSelection = util.select().position(providerPos)

        scene.world().showSection(providerSelection, Direction.DOWN)
        scene.idle(20)

        scene.overlay().showText(60)
            .text("The Brass Pattern Provider is the upgraded version of the Andesite Pattern Provider")
            .pointAt(util.vector().blockSurface(providerPos, Direction.WEST))
            .placeNearTarget()
            .attachKeyFrame()
        scene.idle(70)

        scene.overlay().showText(60)
            .text("It can hold up to 36 AE2 processing patterns — four times the Andesite variant")
            .pointAt(util.vector().blockSurface(providerPos, Direction.WEST))
            .placeNearTarget()
            .attachKeyFrame()
        scene.idle(70)

        scene.overlay().showControls(
            util.vector().blockSurface(providerPos, Direction.NORTH),
            Pointing.RIGHT,
            40
        ).rightClick()
        scene.idle(10)

        scene.overlay().showText(50)
            .text("Right-click to open the GUI and manage your processing patterns")
            .pointAt(util.vector().blockSurface(providerPos, Direction.NORTH))
            .placeNearTarget()
        scene.idle(60)

        scene.overlay().showText(80)
            .text("Like the Andesite variant, place it adjacent to Mechanical Crafters for automatic ingredient distribution")
            .pointAt(util.vector().blockSurface(providerPos, Direction.UP))
            .placeNearTarget()
            .attachKeyFrame()
        scene.idle(90)

        scene.overlay().showText(60)
            .text("Also available as a cable subpart for compact ME network builds")
            .independent()
            .attachKeyFrame()
        scene.idle(70)

        scene.markAsFinished()
    }
}