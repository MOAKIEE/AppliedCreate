package com.loliball.appliedcreate.client

import appeng.api.client.AEKeyRenderHandler
import appeng.api.client.AEKeyRendering
import com.loliball.appliedcreate.AppliedCreate
import com.loliball.appliedcreate.storage.StressKey
import com.loliball.appliedcreate.storage.StressKeyType
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.client.renderer.texture.TextureAtlas
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.Level
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

@OnlyIn(Dist.CLIENT)
object StressKeyRenderHandler : AEKeyRenderHandler<StressKey> {

    // Sprite location in the atlas — we'll use the stress_key texture
    private val STRESS_ICON = ResourceLocation(AppliedCreate.MOD_ID, "item/stress_key")

    fun register() {
        AEKeyRendering.register(StressKeyType.TYPE, StressKey::class.java, this)
    }

    override fun drawInGui(minecraft: Minecraft, guiGraphics: GuiGraphics, x: Int, y: Int, stack: StressKey) {
        val sprite = minecraft.getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(STRESS_ICON)
        guiGraphics.blit(x, y, 0, 16, 16, sprite)
    }

    override fun drawOnBlockFace(
        poseStack: PoseStack,
        buffers: MultiBufferSource,
        what: StressKey,
        scale: Float,
        combinedLight: Int,
        level: Level
    ) {
        val sprite = Minecraft.getInstance()
            .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
            .apply(STRESS_ICON)

        val color = 0xFFDCAA50.toInt() // warm amber tint

        poseStack.pushPose()
        poseStack.translate(0f, 0f, 0.01f)

        val buffer = buffers.getBuffer(RenderType.solid())

        val adjustedScale = scale - 0.05f
        val x0 = -adjustedScale / 2
        val y0 = adjustedScale / 2
        val x1 = adjustedScale / 2
        val y1 = -adjustedScale / 2

        val transform = poseStack.last().pose()
        // MC 1.20.1 VertexConsumer uses separate method calls instead of chained builder
        buffer.vertex(transform, x0, y1, 0f)
            .color(color)
            .uv(sprite.u0, sprite.v1)
            .overlayCoords(OverlayTexture.NO_OVERLAY)
            .uv2(combinedLight)
            .normal(0f, 0f, 1f)
            .endVertex()
        buffer.vertex(transform, x1, y1, 0f)
            .color(color)
            .uv(sprite.u1, sprite.v1)
            .overlayCoords(OverlayTexture.NO_OVERLAY)
            .uv2(combinedLight)
            .normal(0f, 0f, 1f)
            .endVertex()
        buffer.vertex(transform, x1, y0, 0f)
            .color(color)
            .uv(sprite.u1, sprite.v0)
            .overlayCoords(OverlayTexture.NO_OVERLAY)
            .uv2(combinedLight)
            .normal(0f, 0f, 1f)
            .endVertex()
        buffer.vertex(transform, x0, y0, 0f)
            .color(color)
            .uv(sprite.u0, sprite.v0)
            .overlayCoords(OverlayTexture.NO_OVERLAY)
            .uv2(combinedLight)
            .normal(0f, 0f, 1f)
            .endVertex()
        poseStack.popPose()
    }

    override fun getDisplayName(stack: StressKey): Component {
        return stack.displayName
    }
}
