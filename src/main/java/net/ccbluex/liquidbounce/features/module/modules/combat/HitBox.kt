/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot.isBot
import net.ccbluex.liquidbounce.features.module.modules.misc.Teams
import net.ccbluex.liquidbounce.utils.extensions.isAnimal
import net.ccbluex.liquidbounce.utils.extensions.isClientFriend
import net.ccbluex.liquidbounce.utils.extensions.isMob
import net.ccbluex.liquidbounce.value.bool
import net.ccbluex.liquidbounce.value.float
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer

object HitBox : Module("HitBox", Category.COMBAT, hideModule = false) {

    private val targetPlayers by bool("TargetPlayers", true)
    private val playerSize by float("PlayerSize", 0.4F, 0F..1F) { targetPlayers }
    private val friendSize by float("FriendSize", 0.4F, 0F..1F) { targetPlayers }
    private val teamMateSize by float("TeamMateSize", 0.4F, 0F..1F) { targetPlayers }
    private val botSize by float("BotSize", 0.4F, 0F..1F) { targetPlayers }

    private val targetMobs by bool("TargetMobs", false)
    private val mobSize by float("MobSize", 0.4F, 0F..1F) { targetMobs }

    private val targetAnimals by bool("TargetAnimals", false)
    private val animalSize by float("AnimalSize", 0.4F, 0F..1F) { targetAnimals }

    fun determineSize(entity: Entity): Float {
        return when (entity) {
            is EntityPlayer -> {
                if (entity.isSpectator || !targetPlayers) {
                    return 0F
                }

                if (isBot(entity)) {
                    return botSize
                } else if (entity.isClientFriend() && !handleEvents()) {
                    return friendSize
                } else if (handleEvents() && Teams.isInYourTeam(entity)) {
                    return teamMateSize
                }

                playerSize
            }

            else -> {
                if (entity.isMob() && targetMobs) {
                    return mobSize
                } else if (entity.isAnimal() && targetAnimals) {
                    return animalSize
                }

                0F
            }
        }
    }
}