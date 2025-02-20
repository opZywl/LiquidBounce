/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.exploit.ModuleKick

/**
 * AutoLeave module
 *
 * Automatically makes you leave the server whenever your health is low.
 */
object ModuleAutoLeave : ClientModule("AutoLeave", Category.COMBAT) {

    private val health by float("Health", 8f, 0f..20f)

    private val delay by int("Delay", 0, 0..60, "ticks")
    private val mode by enumChoice("Mode", ModuleKick.KickModeEnum.QUIT)

    @Suppress("unused")
    val tickRepeatable = tickHandler {
        if (player.health <= health && !player.abilities.creativeMode && !mc.isIntegratedServerRunning) {
            // Delay to bypass anti cheat or combat log detections
            waitTicks(delay)

            // Kick (@see kick module)
            ModuleKick.kick(mode)

            // Deactivate module after leaving from server
            enabled = false
        }
    }

}
