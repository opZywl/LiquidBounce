/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.value.bool

object SilentHotbarModule : Module("SilentHotbar", Category.RENDER) {
    val keepHighlightedName by bool("KeepHighlightedName", false)
    val keepHotbarSlot by bool("KeepHotbarSlot", false)
    val keepItemInHandInFirstPerson by bool("KeepItemInHandInFirstPerson", false)
    val keepItemInHandInThirdPerson by bool("KeepItemInHandInThirdPerson", false)
}