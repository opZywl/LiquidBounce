/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render2DEvent
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.player.InventoryCleaner
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.ClientUtils.LOGGER
import net.ccbluex.liquidbounce.utils.EntityUtils.isLookingOnEntities
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.render.ColorUtils.rainbow
import net.ccbluex.liquidbounce.utils.render.RenderUtils.disableGlCap
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawEntityBox
import net.ccbluex.liquidbounce.utils.render.RenderUtils.enableGlCap
import net.ccbluex.liquidbounce.utils.render.RenderUtils.resetCaps
import net.ccbluex.liquidbounce.utils.render.shader.shaders.GlowShader
import net.ccbluex.liquidbounce.value.*
import net.minecraft.entity.item.EntityItem
import net.minecraft.util.Vec3
import org.lwjgl.opengl.GL11.*
import java.awt.Color
import kotlin.math.pow

object ItemESP : Module("ItemESP", Category.RENDER, hideModule = false) {
    private val mode by choices("Mode", arrayOf("Box", "OtherBox", "Glow"), "Box")

    private val itemText by bool("ItemText", false)

    private val glowRenderScale by float("Glow-Renderscale", 1f, 0.5f..2f) { mode == "Glow" }
    private val glowRadius by int("Glow-Radius", 4, 1..5) { mode == "Glow" }
    private val glowFade by int("Glow-Fade", 10, 0..30) { mode == "Glow" }
    private val glowTargetAlpha by float("Glow-Target-Alpha", 0f, 0f..1f) { mode == "Glow" }

    private val colorRainbow by bool("Rainbow", true)
    private val colorRed by int("R", 0, 0..255) { !colorRainbow }
    private val colorGreen by int("G", 255, 0..255) { !colorRainbow }
    private val colorBlue by int("B", 0, 0..255) { !colorRainbow }

    private val maxRenderDistance by object : IntegerValue("MaxRenderDistance", 50, 1..100) {
        override fun onUpdate(value: Int) {
            maxRenderDistanceSq = value.toDouble().pow(2.0)
        }
    }

    private val scale by float("Scale", 3F, 1F..5F) { itemText }
    private val itemCounts by bool("ItemCounts", true) { itemText }
    private val font by font("Font", Fonts.font40) { itemText }
    private val fontShadow by bool("Shadow", true) { itemText }

    private var maxRenderDistanceSq = 0.0
        set(value) {
            field = if (value <= 0.0) maxRenderDistance.toDouble().pow(2.0) else value
        }

    private val onLook by bool("OnLook", false)
    private val maxAngleDifference by float("MaxAngleDifference", 90f, 5.0f..90f) { onLook }

    private val thruBlocks by bool("ThruBlocks", true)

    val color
        get() = if (colorRainbow) rainbow() else Color(colorRed, colorGreen, colorBlue)

    // TODO: Removed highlighting of EntityArrow to not complicate things even further

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        if (mc.theWorld == null || mc.thePlayer == null || mode == "Glow")
            return

        runCatching {
            mc.theWorld.loadedEntityList.asSequence()
                .filterIsInstance<EntityItem>()
                .filter { mc.thePlayer.getDistanceSqToEntity(it) <= maxRenderDistanceSq }
                .filter { !onLook || isLookingOnEntities(it, maxAngleDifference.toDouble()) }
                .filter { thruBlocks || RotationUtils.isVisible(Vec3(it.posX, it.posY, it.posZ)) }
                .forEach { entityItem ->
                    val isUseful =
                        InventoryCleaner.handleEvents() && InventoryCleaner.highlightUseful && InventoryCleaner.isStackUseful(
                            entityItem.entityItem,
                            mc.thePlayer.openContainer.inventory,
                            mc.theWorld.loadedEntityList.filterIsInstance<EntityItem>().associateBy { it.entityItem }
                        )

                    if (itemText) {
                        renderEntityText(entityItem, if (isUseful) Color.green else color)
                    }

                    // Only render green boxes on useful items, if ItemESP is enabled, render boxes of ItemESP.color on useless items as well
                    drawEntityBox(entityItem, if (isUseful) Color.green else color, mode == "Box")
                }
        }.onFailure {
            LOGGER.error("An error occurred while rendering ItemESP!", it)
        }
    }

    @EventTarget
    fun onRender2D(event: Render2DEvent) {
        if (mc.theWorld == null || mc.thePlayer == null || mode != "Glow")
            return

        runCatching {
            mc.theWorld.loadedEntityList.asSequence()
                .filterIsInstance<EntityItem>()
                .filter { mc.thePlayer.getDistanceSqToEntity(it) <= maxRenderDistanceSq }
                .filter { !onLook || isLookingOnEntities(it, maxAngleDifference.toDouble()) }
                .filter { thruBlocks || RotationUtils.isVisible(Vec3(it.posX, it.posY, it.posZ)) }
                .forEach { entityItem ->
                    val isUseful =
                        InventoryCleaner.handleEvents() && InventoryCleaner.highlightUseful && InventoryCleaner.isStackUseful(
                            entityItem.entityItem,
                            mc.thePlayer.openContainer.inventory,
                            mc.theWorld.loadedEntityList.filterIsInstance<EntityItem>().associateBy { it.entityItem }
                        )

                    GlowShader.startDraw(event.partialTicks, glowRenderScale)

                    mc.renderManager.renderEntityStatic(entityItem, event.partialTicks, true)

                    // Only render green boxes on useful items, if ItemESP is enabled, render boxes of ItemESP.color on useless items as well
                    GlowShader.stopDraw(if (isUseful) Color.green else color, glowRadius, glowFade, glowTargetAlpha)
                }
        }.onFailure {
            LOGGER.error("An error occurred while rendering ItemESP!", it)
        }
    }

    private fun renderEntityText(entity: EntityItem, color: Color) {
        val thePlayer = mc.thePlayer ?: return
        val renderManager = mc.renderManager
        val rotateX = if (mc.gameSettings.thirdPersonView == 2) -1.0f else 1.0f

        glPushAttrib(GL_ENABLE_BIT)
        glPushMatrix()

        // Translate to entity position
        val partialTicks = mc.timer.renderPartialTicks
        val interpolatedPosX = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks
        val interpolatedPosY = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks + 1F
        val interpolatedPosZ = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks

        glTranslated(
            interpolatedPosX - renderManager.renderPosX,
            interpolatedPosY - renderManager.renderPosY,
            interpolatedPosZ - renderManager.renderPosZ
        )

        glRotatef(-renderManager.playerViewY, 0F, 1F, 0F)
        glRotatef(renderManager.playerViewX * rotateX, 1F, 0F, 0F)

        disableGlCap(GL_LIGHTING, GL_DEPTH_TEST)
        enableGlCap(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        val fontRenderer = font

        // Scale
        val scale = ((thePlayer.getDistanceToEntity(entity) / 4F).coerceAtLeast(1F) / 150F) * scale
        glScalef(-scale, -scale, scale)

        val itemStack = entity.entityItem
        val text = itemStack.displayName + if (itemCounts) " (${itemStack.stackSize})" else ""

        // Draw text
        val width = fontRenderer.getStringWidth(text) * 0.5f
        fontRenderer.drawString(
            text, 1F + -width, if (fontRenderer == Fonts.minecraftFont) 1F else 1.5F, color.rgb, fontShadow
        )

        resetCaps()
        glPopMatrix()
        glPopAttrib()
    }

    override fun handleEvents() =
        super.handleEvents() || (InventoryCleaner.handleEvents() && InventoryCleaner.highlightUseful)
}
