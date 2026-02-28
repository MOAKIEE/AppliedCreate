package com.loliball.appliedcreate.cannon

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.AllPartialModels
import com.simibubi.create.content.schematics.cannon.LaunchedItem
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer
import dev.engine_room.flywheel.api.visualization.VisualizationManager
import net.createmod.catnip.render.CachedBuffers
import net.createmod.ponder.render.VirtualRenderHelper
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.Direction
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.Mth
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.phys.Vec3
import com.mojang.math.Axis

class MEBlueprintCannonRenderer(context: BlockEntityRendererProvider.Context) :
    SafeBlockEntityRenderer<MEBlueprintCannonBlockEntity>() {

    override fun renderSafe(
        be: MEBlueprintCannonBlockEntity,
        partialTicks: Float,
        ms: PoseStack,
        buffer: MultiBufferSource,
        light: Int,
        overlay: Int
    ) {
        val blocksLaunching = be.flyingBlocks.isNotEmpty()
        if (blocksLaunching) {
            renderLaunchedBlocks(be, partialTicks, ms, buffer, light, overlay)
        }

        if (VisualizationManager.supportsVisualization(be.level)) return

        val pos = be.blockPos
        val state = be.blockState

        // Calculate cannon angles
        val target = be.printer.currentTarget
        val yaw: Double
        val pitch: Double

        if (target != null) {
            var diff = Vec3.atLowerCornerOf(target.subtract(pos))
            if (be.previousTarget != null) {
                diff = Vec3.atLowerCornerOf(be.previousTarget!!)
                    .add(
                        Vec3.atLowerCornerOf(target.subtract(be.previousTarget!!))
                            .scale(partialTicks.toDouble())
                    )
                    .subtract(Vec3.atLowerCornerOf(pos))
            }

            val diffX = diff.x()
            val diffZ = diff.z()
            yaw = Mth.atan2(diffX, diffZ) / Math.PI * 180
            val distance = Mth.sqrt((diffX * diffX + diffZ * diffZ).toFloat())
            val yOffset = distance * 2.0
            pitch = Mth.atan2(distance.toDouble(), diff.y() * 3 + yOffset) / Math.PI * 180 + 10
        } else {
            yaw = be.defaultYaw.toDouble()
            pitch = 40.0
        }

        // Calculate recoil
        var recoil = 0.0
        for (launched in be.flyingBlocks) {
            if (launched.ticksRemaining == 0) continue
            if ((launched.ticksRemaining + 1 - partialTicks) > launched.totalTicks - 10) {
                recoil = maxOf(
                    recoil,
                    (launched.ticksRemaining + 1 - partialTicks).toDouble() - launched.totalTicks + 10
                )
            }
        }

        ms.pushPose()

        val vb = buffer.getBuffer(RenderType.solid())

        @Suppress("UNCHECKED_CAST")
        val connector = CachedBuffers.partial(AllPartialModels.SCHEMATICANNON_CONNECTOR, state)
        connector.translate(.5f, 0f, .5f)
        connector.rotate(((yaw + 90) / 180 * Math.PI).toFloat(), Direction.UP)
        connector.translate(-.5f, 0f, -.5f)
        connector.light<net.createmod.catnip.render.SuperByteBuffer>(light)
        connector.renderInto(ms, vb)

        val pipe = CachedBuffers.partial(AllPartialModels.SCHEMATICANNON_PIPE, state)
        pipe.translate(.5f, 15f / 16f, .5f)
        pipe.rotate(((yaw + 90) / 180 * Math.PI).toFloat(), Direction.UP)
        pipe.rotate((pitch / 180 * Math.PI).toFloat(), Direction.SOUTH)
        pipe.translate(-.5f, -15f / 16f, -.5f)
        pipe.translate(0f, (-recoil / 100).toFloat(), 0f)
        pipe.light<net.createmod.catnip.render.SuperByteBuffer>(light)
        pipe.renderInto(ms, vb)

        ms.popPose()
    }

    private fun renderLaunchedBlocks(
        be: MEBlueprintCannonBlockEntity,
        partialTicks: Float,
        ms: PoseStack,
        buffer: MultiBufferSource,
        light: Int,
        overlay: Int
    ) {
        for (launched in be.flyingBlocks) {
            if (launched.ticksRemaining == 0) continue

            val start = Vec3.atCenterOf(be.blockPos.above())
            val targetVec = Vec3.atCenterOf(launched.target)
            val distance = targetVec.subtract(start)

            val yDifference = targetVec.y - start.y
            val throwHeight = Math.sqrt(distance.lengthSqr()) * 0.6 + yDifference
            val cannonOffset = distance.add(0.0, throwHeight, 0.0)
                .normalize()
                .scale(2.0)
            val adjustedStart = start.add(cannonOffset)
            val adjustedYDiff = targetVec.y - adjustedStart.y

            val progress =
                (launched.totalTicks.toFloat() - (launched.ticksRemaining + 1 - partialTicks)) / launched.totalTicks
            val blockLocationXZ = targetVec.subtract(adjustedStart)
                .scale(progress.toDouble())
                .multiply(1.0, 0.0, 1.0)

            val t = progress.toDouble()
            val yOffset = 2 * (1 - t) * t * throwHeight + t * t * adjustedYDiff
            val blockLocation = blockLocationXZ.add(0.5, yOffset + 1.5, 0.5)
                .add(cannonOffset)

            ms.pushPose()
            ms.translate(blockLocation.x, blockLocation.y, blockLocation.z)

            ms.translate(.125, .125, .125)
            ms.mulPose(Axis.YP.rotationDegrees(360 * progress))
            ms.mulPose(Axis.XP.rotationDegrees(360 * progress))
            ms.translate(-.125, -.125, -.125)

            if (launched is LaunchedItem.ForBlockState) {
                var blockState = launched.state
                if (launched is LaunchedItem.ForBelt) {
                    blockState = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", "shaft")).defaultBlockState()
                }
                val scale = 0.3f
                ms.scale(scale, scale, scale)
                Minecraft.getInstance()
                    .blockRenderer
                    .renderSingleBlock(
                        blockState, ms, buffer, light, overlay,
                        VirtualRenderHelper.VIRTUAL_DATA, @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS") null
                    )
            } else if (launched is LaunchedItem.ForEntity) {
                val scale = 1.2f
                ms.scale(scale, scale, scale)
                Minecraft.getInstance()
                    .itemRenderer
                    .renderStatic(
                        launched.stack, ItemDisplayContext.GROUND, light, overlay,
                        ms, buffer, be.level, 0
                    )
            }

            ms.popPose()

            // Render particles for launch
            if (launched.ticksRemaining == launched.totalTicks && be.firstRenderTick) {
                val particleStart = start.subtract(0.5, 0.5, 0.5)
                be.firstRenderTick = false
                for (i in 0 until 10) {
                    val r = be.level!!.random
                    val sX = cannonOffset.x * 0.01
                    val sY = (cannonOffset.y + 1) * 0.01
                    val sZ = cannonOffset.z * 0.01
                    val rX = r.nextFloat() - sX * 40
                    val rY = r.nextFloat() - sY * 40
                    val rZ = r.nextFloat() - sZ * 40
                    be.level!!.addParticle(
                        ParticleTypes.CLOUD,
                        particleStart.x + rX, particleStart.y + rY, particleStart.z + rZ,
                        sX, sY, sZ
                    )
                }
            }
        }
    }

    override fun shouldRenderOffScreen(be: MEBlueprintCannonBlockEntity): Boolean = true

    override fun getViewDistance(): Int = 128
}
