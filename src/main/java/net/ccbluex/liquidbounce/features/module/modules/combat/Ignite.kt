/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.EntityUtils
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPackets
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.SilentHotbar
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils
import net.ccbluex.liquidbounce.utils.inventory.hotBarSlot
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.ccbluex.liquidbounce.value.bool
import net.minecraft.block.BlockAir
import net.minecraft.init.Items
import net.minecraft.item.ItemBucket
import net.minecraft.network.play.client.C03PacketPlayer.C05PacketPlayerLook
import net.minecraft.network.play.client.C09PacketHeldItemChange
import net.minecraft.util.EnumFacing
import net.minecraft.util.MathHelper
import net.minecraft.util.Vec3
import kotlin.math.atan2
import kotlin.math.sqrt

// TODO: This desperately needs a recode
object Ignite : Module("Ignite", Category.COMBAT, hideModule = false) {

    private val lighter by bool("Lighter", true)
    private val lavaBucket by bool("Lava", true)

    private val msTimer = MSTimer()

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (!msTimer.hasTimePassed(500))
            return

        val player = mc.thePlayer ?: return
        val world = mc.theWorld ?: return

        val lighterInHotbar = if (lighter) InventoryUtils.findItem(36, 44, Items.flint_and_steel) else null
        val lavaInHotbar = if (lavaBucket) InventoryUtils.findItem(36, 44, Items.lava_bucket) else null

        val fireInHotbar = lighterInHotbar ?: lavaInHotbar ?: return

        for (entity in world.loadedEntityList) {
            if (EntityUtils.isSelected(entity, true) && !entity.isBurning) {
                val blockPos = entity.position

                if (player.getDistanceSq(blockPos) >= 22.3 || !blockPos.isReplaceable() || blockPos.getBlock() !is BlockAir)
                    continue

                RotationUtils.resetTicks += 1

                SilentHotbar.selectSlotSilently(this, fireInHotbar, 0, immediate = true, render = false)

                val itemStack = player.hotBarSlot(fireInHotbar).stack

                if (itemStack.item is ItemBucket) {
                    val diffX = blockPos.x + 0.5 - player.posX
                    val diffY = blockPos.y + 0.5 - (player.entityBoundingBox.minY + player.eyeHeight)
                    val diffZ = blockPos.z + 0.5 - player.posZ
                    val sqrt = sqrt(diffX * diffX + diffZ * diffZ)
                    val yaw = (atan2(diffZ, diffX)).toDegreesF() - 90F
                    val pitch = -(atan2(diffY, sqrt)).toDegreesF()

                    sendPacket(
                        C05PacketPlayerLook(
                            player.rotationYaw +
                                    MathHelper.wrapAngleTo180_float(yaw - player.rotationYaw),
                            player.rotationPitch +
                                    MathHelper.wrapAngleTo180_float(pitch - player.rotationPitch),
                            player.onGround
                        )
                    )

                    player.sendUseItem(itemStack)
                } else {
                    for (side in EnumFacing.values()) {
                        val neighbor = blockPos.offset(side)

                        if (!neighbor.canBeClicked())
                            continue

                        val diffX = neighbor.x + 0.5 - player.posX
                        val diffY = neighbor.y + 0.5 - (player.entityBoundingBox.minY + player.eyeHeight)
                        val diffZ = neighbor.z + 0.5 - player.posZ
                        val sqrt = sqrt(diffX * diffX + diffZ * diffZ)
                        val yaw = (atan2(diffZ, diffX)).toDegreesF() - 90F
                        val pitch = -(atan2(diffY, sqrt)).toDegreesF()

                        sendPacket(
                            C05PacketPlayerLook(
                                player.rotationYaw +
                                        MathHelper.wrapAngleTo180_float(yaw - player.rotationYaw),
                                player.rotationPitch +
                                        MathHelper.wrapAngleTo180_float(pitch - player.rotationPitch),
                                player.onGround
                            )
                        )

                        if (player.onPlayerRightClick(neighbor, side.opposite, Vec3(side.directionVec), itemStack)) {
                            player.swingItem()
                            break
                        }
                    }
                }

                sendPackets(
                    C09PacketHeldItemChange(player.inventory.currentItem),
                    C05PacketPlayerLook(
                        player.rotationYaw,
                        player.rotationPitch,
                        player.onGround
                    )
                )

                msTimer.reset()
                break
            }
        }
    }
}
