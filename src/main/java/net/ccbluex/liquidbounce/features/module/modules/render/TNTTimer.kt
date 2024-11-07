/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.EntityUtils.isLookingOnEntities
import net.ccbluex.liquidbounce.utils.render.RenderUtils.disableGlCap
import net.ccbluex.liquidbounce.utils.render.RenderUtils.enableGlCap
import net.ccbluex.liquidbounce.utils.render.RenderUtils.resetCaps
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.bool
import net.ccbluex.liquidbounce.value.float
import net.ccbluex.liquidbounce.value.font
import net.ccbluex.liquidbounce.value.int
import net.minecraft.entity.item.EntityTNTPrimed
import org.lwjgl.opengl.GL11.*
import kotlin.math.pow

object TNTTimer : Module("TNTTimer", Category.RENDER, spacedName = "TNT Timer", hideModule = false) {

    private val scale by float("Scale", 3F, 1F..4F)
    private val font by font("Font", Fonts.font40)
    private val fontShadow by bool("Shadow", true)

    private val colorRed by int("R", 255, 0..255)
    private val colorGreen by int("G", 255, 0..255)
    private val colorBlue by int("B", 255, 0..255)

    private val maxRenderDistance by object : IntegerValue("MaxRenderDistance", 100, 1..200) {
        override fun onUpdate(value: Int) {
            maxRenderDistanceSq = value.toDouble().pow(2.0)
        }
    }

    private val onLook by bool("OnLook", false)
    private val maxAngleDifference by float("MaxAngleDifference", 5.0f, 5.0f..90f) { onLook }

    private var maxRenderDistanceSq = 0.0
        set(value) {
            field = if (value <= 0.0) maxRenderDistance.toDouble().pow(2.0) else value
        }

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        val player = mc.thePlayer ?: return
        val world = mc.theWorld ?: return

        for (entity in world.loadedEntityList.filterNotNull()) {
            if (entity is EntityTNTPrimed && player.getDistanceSqToEntity(entity) <= maxRenderDistanceSq) {
                val explosionTime = entity.fuse / 5

                if (explosionTime > 0 && (isLookingOnEntities(entity, maxAngleDifference.toDouble()) || !onLook)) {
                    renderTNTTimer(entity, explosionTime)
                }
            }
        }
    }

    private fun renderTNTTimer(tnt: EntityTNTPrimed, timeRemaining: Int) {
        val thePlayer = mc.thePlayer ?: return
        val renderManager = mc.renderManager
        val rotateX = if (mc.gameSettings.thirdPersonView == 2) -1.0f else 1.0f

        glPushAttrib(GL_ENABLE_BIT)
        glPushMatrix()

        // Translate to TNT position
        glTranslated(
            tnt.lastTickPosX + (tnt.posX - tnt.lastTickPosX) - renderManager.renderPosX,
            tnt.lastTickPosY + (tnt.posY - tnt.lastTickPosY) - renderManager.renderPosY + 1.5,
            tnt.lastTickPosZ + (tnt.posZ - tnt.lastTickPosZ) - renderManager.renderPosZ
        )

        glRotatef(-renderManager.playerViewY, 0F, 1F, 0F)
        glRotatef(renderManager.playerViewX * rotateX, 1F, 0F, 0F)

        disableGlCap(GL_LIGHTING, GL_DEPTH_TEST)

        enableGlCap(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        val color = ((colorRed and 0xFF) shl 16) or ((colorGreen and 0xFF) shl 8) or (colorBlue and 0xFF)

        val text = "TNT Explodes in: $timeRemaining"

        val fontRenderer = font

        // Scale
        val scale = ((thePlayer.getDistanceToEntity(tnt) / 4F).coerceAtLeast(1F) / 150F) * scale
        glScalef(-scale, -scale, scale)

        // Draw text
        val width = fontRenderer.getStringWidth(text) * 0.5f
        fontRenderer.drawString(
            text, 1F + -width, if (fontRenderer == Fonts.minecraftFont) 1F else 1.5F, color, fontShadow
        )

        resetCaps()
        glPopMatrix()
        glPopAttrib()
    }

}