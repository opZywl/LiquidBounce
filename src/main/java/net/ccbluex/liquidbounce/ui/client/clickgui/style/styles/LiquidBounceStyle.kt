/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.clickgui.style.styles

import net.ccbluex.liquidbounce.config.*
import net.ccbluex.liquidbounce.features.module.modules.render.ClickGUI.guiColor
import net.ccbluex.liquidbounce.features.module.modules.render.ClickGUI.scale
import net.ccbluex.liquidbounce.file.FileManager.saveConfig
import net.ccbluex.liquidbounce.file.FileManager.valuesConfig
import net.ccbluex.liquidbounce.ui.client.clickgui.ClickGui.clamp
import net.ccbluex.liquidbounce.ui.client.clickgui.Panel
import net.ccbluex.liquidbounce.ui.client.clickgui.elements.ButtonElement
import net.ccbluex.liquidbounce.ui.client.clickgui.elements.ModuleElement
import net.ccbluex.liquidbounce.ui.client.clickgui.style.Style
import net.ccbluex.liquidbounce.ui.font.AWTFontRenderer.Companion.assumeNonVolatile
import net.ccbluex.liquidbounce.ui.font.Fonts.font35
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlockName
import net.ccbluex.liquidbounce.utils.extensions.component1
import net.ccbluex.liquidbounce.utils.extensions.component2
import net.ccbluex.liquidbounce.utils.extensions.lerpWith
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawBorderedRect
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRect
import net.ccbluex.liquidbounce.utils.timing.WaitTickUtils
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.util.StringUtils
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import java.awt.Color
import kotlin.math.abs
import kotlin.math.roundToInt

@SideOnly(Side.CLIENT)
object LiquidBounceStyle : Style() {
    override fun drawPanel(mouseX: Int, mouseY: Int, panel: Panel) {
        drawBorderedRect(
            panel.x,
            panel.y,
            panel.x + panel.width,
            panel.y + panel.height + panel.fade,
            1,
            Color.GRAY.rgb,
            Int.MIN_VALUE
        )

        val xPos = panel.x - (font35.getStringWidth(StringUtils.stripControlCodes(panel.name)) - 100) / 2
        font35.drawString(panel.name, xPos, panel.y + 7, Color.WHITE.rgb)

        if (panel.scrollbar && panel.fade > 0) {
            drawRect(panel.x - 2, panel.y + 21, panel.x, panel.y + 16 + panel.fade, Color.DARK_GRAY.rgb)

            val visibleRange = panel.getVisibleRange()
            val minY =
                panel.y + 21 + panel.fade * if (visibleRange.first > 0) visibleRange.first / panel.elements.lastIndex.toFloat()
                else 0f
            val maxY =
                panel.y + 16 + panel.fade * if (visibleRange.last > 0) visibleRange.last / panel.elements.lastIndex.toFloat()
                else 0f

            drawRect(panel.x - 2, minY.roundToInt(), panel.x, maxY.roundToInt(), Color.GRAY.rgb)
        }
    }

    override fun drawHoverText(mouseX: Int, mouseY: Int, text: String) {
        val lines = text.lines()

        val width =
            lines.maxOfOrNull { font35.getStringWidth(it) + 14 } ?: return // Makes no sense to render empty lines
        val height = font35.fontHeight * lines.size + 3

        // Don't draw hover text beyond window boundaries
        val (scaledWidth, scaledHeight) = ScaledResolution(mc)
        val x = mouseX.clamp(0, (scaledWidth / scale - width).roundToInt())
        val y = mouseY.clamp(0, (scaledHeight / scale - height).roundToInt())

        drawBorderedRect(x + 9, y, x + width, y + height, 1, Color.GRAY.rgb, Int.MIN_VALUE)
        lines.forEachIndexed { index, text ->
            font35.drawString(text, x + 12, y + 3 + (font35.fontHeight) * index, Int.MAX_VALUE)
        }
    }

    override fun drawButtonElement(mouseX: Int, mouseY: Int, buttonElement: ButtonElement) {
        val xPos = buttonElement.x - (font35.getStringWidth(buttonElement.displayName) - 100) / 2
        font35.drawString(buttonElement.displayName, xPos, buttonElement.y + 6, buttonElement.color)
    }

    override fun drawModuleElementAndClick(
        mouseX: Int, mouseY: Int, moduleElement: ModuleElement, mouseButton: Int?
    ): Boolean {
        val xPos = moduleElement.x - (font35.getStringWidth(moduleElement.displayName) - 100) / 2
        font35.drawString(
            moduleElement.displayName, xPos, moduleElement.y + 6, if (moduleElement.module.state) {
                if (moduleElement.module.isActive) guiColor
                // Make inactive modules have alpha set to 100
                else (guiColor and 0x00FFFFFF) or (0x64 shl 24)
            } else Int.MAX_VALUE
        )

        val moduleValues = moduleElement.module.values.filter { it.shouldRender() }
        if (moduleValues.isNotEmpty()) {
            font35.drawString(
                if (moduleElement.showSettings) "-" else "+",
                moduleElement.x + moduleElement.width - 8,
                moduleElement.y + moduleElement.height / 2,
                Color.WHITE.rgb
            )

            if (moduleElement.showSettings) {
                var yPos = moduleElement.y + 4

                val minX = moduleElement.x + moduleElement.width + 4
                val maxX = moduleElement.x + moduleElement.width + moduleElement.settingsWidth

                for (value in moduleValues) {
                    assumeNonVolatile = value.get() is Number

                    val suffix = value.suffix ?: ""

                    when (value) {
                        is BoolValue -> {
                            val text = value.name

                            moduleElement.settingsWidth = font35.getStringWidth(text) + 8

                            drawRect(minX, yPos + 2, maxX, yPos + 14, Int.MIN_VALUE)

                            if (mouseButton == 0 && mouseX in minX..maxX && mouseY in yPos + 2..yPos + 14) {
                                value.toggle()
                                clickSound()
                                return true
                            }

                            font35.drawString(
                                text, minX + 2, yPos + 4, if (value.get()) guiColor else Int.MAX_VALUE
                            )

                            yPos += 12
                        }

                        is ListValue -> {
                            val text = value.name

                            moduleElement.settingsWidth = font35.getStringWidth(text) + 16

                            if (mouseButton == 0 && mouseX in minX..maxX && mouseY in yPos + 2..yPos + 14) {
                                value.openList = !value.openList
                                clickSound()
                                return true
                            }

                            drawRect(minX, yPos + 2, maxX, yPos + 14, Int.MIN_VALUE)

                            font35.drawString("§c$text", minX + 2, yPos + 4, Color.WHITE.rgb)
                            font35.drawString(
                                if (value.openList) "-" else "+",
                                maxX - if (value.openList) 5 else 6,
                                yPos + 4,
                                Color.WHITE.rgb
                            )

                            yPos += 12

                            for (valueOfList in value.values) {
                                moduleElement.settingsWidth = font35.getStringWidth(valueOfList) + 16

                                if (value.openList) {
                                    drawRect(minX, yPos + 2, maxX, yPos + 14, Int.MIN_VALUE)

                                    if (mouseButton == 0 && mouseX in minX..maxX && mouseY in yPos + 2..yPos + 14) {
                                        value.set(valueOfList)
                                        clickSound()
                                        return true
                                    }

                                    font35.drawString(
                                        ">",
                                        minX + 2,
                                        yPos + 4,
                                        if (value.get() == valueOfList) guiColor else Int.MAX_VALUE
                                    )
                                    font35.drawString(
                                        valueOfList,
                                        minX + 10,
                                        yPos + 4,
                                        if (value.get() == valueOfList) guiColor else Int.MAX_VALUE
                                    )

                                    yPos += 12
                                }
                            }
                        }

                        is FloatValue -> {
                            val text = value.name + "§f: §c" + round(value.get()) + " §8${suffix}§c"
                            moduleElement.settingsWidth = font35.getStringWidth(text) + 8

                            if ((mouseButton == 0 || sliderValueHeld == value) && mouseX in minX..maxX && mouseY in yPos + 15..yPos + 21) {
                                val percentage = (mouseX - minX - 4) / (maxX - minX - 8).toFloat()
                                value.set(
                                    round(value.minimum + (value.maximum - value.minimum) * percentage).coerceIn(
                                        value.range
                                    )
                                )

                                // Keep changing this slider until mouse is unpressed.
                                sliderValueHeld = value

                                // Stop rendering and interacting only when this event was triggered by a mouse click.
                                if (mouseButton == 0) return true
                            }

                            drawRect(minX, yPos + 2, maxX, yPos + 24, Int.MIN_VALUE)
                            drawRect(minX + 4, yPos + 18, maxX - 4, yPos + 19, Int.MAX_VALUE)

                            val displayValue = value.get().coerceIn(value.range)
                            val sliderValue =
                                (moduleElement.x + moduleElement.width + (moduleElement.settingsWidth - 12) * (displayValue - value.minimum) / (value.maximum - value.minimum)).roundToInt()
                            drawRect(8 + sliderValue, yPos + 15, sliderValue + 11, yPos + 21, guiColor)

                            font35.drawString(text, minX + 2, yPos + 4, Color.WHITE.rgb)

                            yPos += 22
                        }

                        is IntegerValue -> {
                            val text = value.name + "§f: §c" + if (value is BlockValue) {
                                getBlockName(value.get()) + " (" + value.get() + ")"
                            } else {
                                value.get()
                            } + " §8${suffix}"

                            moduleElement.settingsWidth = font35.getStringWidth(text) + 8

                            if ((mouseButton == 0 || sliderValueHeld == value) && mouseX in minX..maxX && mouseY in yPos + 15..yPos + 21) {
                                val percentage = (mouseX - minX - 4) / (maxX - minX - 8).toFloat()
                                value.set(
                                    (value.minimum + (value.maximum - value.minimum) * percentage).roundToInt()
                                        .coerceIn(value.range)
                                )

                                // Keep changing this slider until mouse is unpressed.
                                sliderValueHeld = value

                                // Stop rendering and interacting only when this event was triggered by a mouse click.
                                if (mouseButton == 0) return true
                            }

                            drawRect(minX, yPos + 2, maxX, yPos + 24, Int.MIN_VALUE)
                            drawRect(minX + 4, yPos + 18, maxX - 4, yPos + 19, Int.MAX_VALUE)

                            val displayValue = value.get().coerceIn(value.range)
                            val sliderValue =
                                moduleElement.x + moduleElement.width + (moduleElement.settingsWidth - 12) * (displayValue - value.minimum) / (value.maximum - value.minimum)
                            drawRect(8 + sliderValue, yPos + 15, sliderValue + 11, yPos + 21, guiColor)

                            font35.drawString(text, minX + 2, yPos + 4, Color.WHITE.rgb)

                            yPos += 22
                        }

                        is IntegerRangeValue -> {
                            val slider1 = value.get().first
                            val slider2 = value.get().last

                            val text = "${value.name}§f: §c$slider1 §f- §c$slider2 §8${suffix}§c (Beta)"
                            moduleElement.settingsWidth = font35.getStringWidth(text) + 8

                            if ((mouseButton == 0 || sliderValueHeld == value) && mouseX in minX..maxX && mouseY in yPos + 15..yPos + 21) {
                                val slider1Pos =
                                    minX + ((slider1 - value.minimum).toFloat() / (value.maximum - value.minimum)) * (maxX - minX)
                                val slider2Pos =
                                    minX + ((slider2 - value.minimum).toFloat() / (value.maximum - value.minimum)) * (maxX - minX)

                                val distToSlider1 = mouseX - slider1Pos
                                val distToSlider2 = mouseX - slider2Pos

                                val percentage = (mouseX - minX - 4F) / (maxX - minX - 8F)

                                if (abs(distToSlider1) <= abs(distToSlider2) && distToSlider2 <= 0) {
                                    value.setFirst(value.lerpWith(percentage).coerceIn(value.minimum, slider2))
                                } else value.setLast(value.lerpWith(percentage).coerceIn(slider1, value.maximum))

                                // Keep changing this slider until mouse is unpressed.
                                sliderValueHeld = value

                                // Stop rendering and interacting only when this event was triggered by a mouse click.
                                if (mouseButton == 0) return true
                            }

                            drawRect(minX, yPos + 2, maxX, yPos + 24, Int.MIN_VALUE)
                            drawRect(minX + 4, yPos + 18, maxX - 4, yPos + 19, Int.MAX_VALUE)

                            val displayValue1 = value.get().first
                            val displayValue2 = value.get().last

                            val sliderValue1 =
                                minX - 4 + (moduleElement.settingsWidth - 12) * (displayValue1 - value.minimum) / (value.maximum - value.minimum)
                            val sliderValue2 =
                                minX - 4 + (moduleElement.settingsWidth - 12) * (displayValue2 - value.minimum) / (value.maximum - value.minimum)

                            drawRect(8 + sliderValue1, yPos + 15, sliderValue1 + 11, yPos + 21, guiColor)
                            drawRect(8 + sliderValue2, yPos + 15, sliderValue2 + 11, yPos + 21, guiColor)

                            font35.drawString(text, minX + 2, yPos + 4, Color.WHITE.rgb)

                            yPos += 22
                        }

                        is FloatRangeValue -> {
                            val slider1 = value.get().start
                            val slider2 = value.get().endInclusive

                            val text =
                                "${value.name}§f: §c${round(slider1)} §f- §c${round(slider2)} §8${suffix}§c (Beta)"
                            moduleElement.settingsWidth = font35.getStringWidth(text) + 8

                            if ((mouseButton == 0 || sliderValueHeld == value) && mouseX in minX..maxX && mouseY in yPos + 15..yPos + 21) {
                                val slider1Pos =
                                    minX + (slider1 - value.minimum) / (value.maximum - value.minimum) * (maxX - minX)
                                val slider2Pos =
                                    minX + ((slider2 - value.minimum) / (value.maximum - value.minimum)) * (maxX - minX)

                                val distToSlider1 = mouseX - slider1Pos
                                val distToSlider2 = mouseX - slider2Pos

                                val percentage = (mouseX - minX - 4F) / (maxX - minX - 8F)

                                if (abs(distToSlider1) <= abs(distToSlider2) && distToSlider2 <= 0) {
                                    value.setFirst(value.lerpWith(percentage).coerceIn(value.minimum, slider2))
                                } else value.setLast(value.lerpWith(percentage).coerceIn(slider1, value.maximum))

                                // Keep changing this slider until mouse is unpressed.
                                sliderValueHeld = value

                                // Stop rendering and interacting only when this event was triggered by a mouse click.
                                if (mouseButton == 0) return true
                            }

                            drawRect(minX, yPos + 2, maxX, yPos + 24, Int.MIN_VALUE)
                            drawRect(minX + 4, yPos + 18, maxX - 4, yPos + 19, Int.MAX_VALUE)

                            val displayValue1 = value.get().start
                            val displayValue2 = value.get().endInclusive

                            val sliderValue1 =
                                minX - 4 + (moduleElement.settingsWidth - 12) * (displayValue1 - value.minimum) / (value.maximum - value.minimum)
                            val sliderValue2 =
                                minX - 4 + (moduleElement.settingsWidth - 12) * (displayValue2 - value.minimum) / (value.maximum - value.minimum)

                            drawRect(8f + sliderValue1, yPos + 15f, sliderValue1 + 11f, yPos + 21f, guiColor)
                            drawRect(8f + sliderValue2, yPos + 15f, sliderValue2 + 11f, yPos + 21f, guiColor)

                            font35.drawString(text, minX + 2, yPos + 4, Color.WHITE.rgb)

                            yPos += 22
                        }

                        is FontValue -> {
                            val displayString = value.displayName
                            moduleElement.settingsWidth = font35.getStringWidth(displayString) + 8

                            if (mouseButton != null && mouseX in minX..maxX && mouseY in yPos + 4..yPos + 12) {
                                // Cycle to next font when left-clicked, previous when right-clicked.
                                if (mouseButton == 0) value.next()
                                else value.previous()
                                clickSound()
                                return true
                            }

                            drawRect(minX, yPos + 2, maxX, yPos + 14, Int.MIN_VALUE)

                            font35.drawString(displayString, minX + 2, yPos + 4, Color.WHITE.rgb)

                            yPos += 11
                        }

                        is ColorValue -> {
                            val currentColor = if (value.rainbow) {
                                value.get()
                            } else {
                                value.get()
                            }

                            val previewSize = 10
                            val startX = moduleElement.x + moduleElement.width + 4
                            val previewY1 = yPos + 2
                            val previewX2 = startX + previewSize
                            val previewY2 = previewY1 + previewSize

                            val hueSliderWidth = 7
                            val hueSliderHeight = 50
                            val colorPickerWidth = 75
                            val colorPickerHeight = 50

                            if (mouseButton == 0 && mouseX in startX..previewX2 && mouseY in previewY1..previewY2) {
                                value.showPicker = !value.showPicker
                                clickSound()
                                return true
                            }

                            val startY = previewY1 + previewSize + 12

                            drawRect(
                                startX,
                                previewY1,
                                maxX,
                                (if (!value.showPicker) startY + colorPickerHeight + 2 else previewY2) + 2,
                                Int.MIN_VALUE
                            )

                            drawRect(startX, previewY1, previewX2, previewY2, currentColor.rgb)

                            if (!value.showPicker) {
                                if (!value.rainbow) {
                                    val hueSliderColor = value.hueSliderColor

                                    var (hue, saturation, brightness) = Color.RGBtoHSB(
                                        hueSliderColor.red, hueSliderColor.green, hueSliderColor.blue, null
                                    )
                                    var (currHue, currSaturation, currBrightness) = Color.RGBtoHSB(
                                        currentColor.red, currentColor.green, currentColor.blue, null
                                    )

                                    val display = "Color"

                                    drawRect(
                                        startX - 2,
                                        startY - 12,
                                        startX,
                                        startY + colorPickerHeight + 2,
                                        Color(30, 30, 30, 160).rgb
                                    )

                                    font35.drawStringWithShadow(display, startX + 1F, startY - 10F, Color.WHITE.rgb)

                                    // Color Picker
                                    value.updateTextureCache(
                                        id = 0,
                                        hue = hue,
                                        width = colorPickerWidth,
                                        height = colorPickerHeight,
                                        generateImage = { image, _ ->
                                            for (px in 0 until colorPickerWidth) {
                                                for (py in 0 until colorPickerHeight) {
                                                    val localS = px / colorPickerWidth.toFloat()
                                                    val localB = 1.0f - (py / colorPickerHeight.toFloat())
                                                    val rgb = Color.HSBtoRGB(hue, localS, localB)
                                                    image.setRGB(px, py, rgb)
                                                }
                                            }
                                        },
                                        drawAt = { id ->
                                            drawTexture(id, startX, startY, colorPickerWidth, colorPickerHeight)
                                        })

                                    val markerX = startX + (currSaturation * colorPickerWidth).toInt()
                                    val markerY = startY + ((1.0f - currBrightness) * colorPickerHeight).toInt()

                                    RenderUtils.drawBorder(
                                        markerX - 2f, markerY - 2f, markerX + 3f, markerY + 3f, 1.0f, Color.WHITE.rgb
                                    )

                                    val hueSliderX = startX + colorPickerWidth + 5

                                    // Hue slider
                                    value.updateTextureCache(
                                        id = 1,
                                        hue = hue,
                                        width = hueSliderWidth,
                                        height = hueSliderHeight,
                                        generateImage = { image, _ ->
                                            for (y in 0 until hueSliderHeight) {
                                                for (x in 0 until hueSliderWidth) {
                                                    val localHue = y / hueSliderHeight.toFloat()
                                                    val rgb = Color.HSBtoRGB(localHue, 1.0f, 1.0f)
                                                    image.setRGB(x, y, rgb)
                                                }
                                            }
                                        },
                                        drawAt = { id ->
                                            drawTexture(id, hueSliderX, startY, hueSliderWidth, hueSliderHeight)
                                        })

                                    val hueMarkerY = startY + (hue * hueSliderHeight)

                                    RenderUtils.drawBorder(
                                        hueSliderX.toFloat() - 1,
                                        hueMarkerY - 1f,
                                        hueSliderX + hueSliderWidth + 1f,
                                        (hueMarkerY + 1).coerceAtMost((startY + hueSliderHeight).toFloat()) + 1,
                                        2f,
                                        Color.WHITE.rgb,
                                    )

                                    val inColorPicker =
                                        mouseX in startX until startX + colorPickerWidth && mouseY in startY until startY + colorPickerHeight
                                    val inHueSlider =
                                        mouseX in hueSliderX - 1..hueSliderX + hueSliderWidth + 1 && mouseY in startY until startY + hueSliderHeight

                                    if ((mouseButton == 0 || sliderValueHeld == value) && (inColorPicker || inHueSlider)) {
                                        if (inColorPicker) {
                                            val newS = ((mouseX - startX) / colorPickerWidth.toFloat()).coerceIn(0f, 1f)
                                            val newB =
                                                (1.0f - (mouseY - startY) / colorPickerHeight.toFloat()).coerceIn(
                                                    0f, 1f
                                                )
                                            saturation = newS
                                            brightness = newB
                                        }

                                        var finalColor = Color(Color.HSBtoRGB(hue, saturation, brightness))

                                        if (inHueSlider) {
                                            currHue = ((mouseY - startY) / hueSliderHeight.toFloat()).coerceIn(0f, 1f)

                                            finalColor = Color(Color.HSBtoRGB(currHue, currSaturation, currBrightness))
                                        }

                                        sliderValueHeld = value

                                        if (inHueSlider) {
                                            value.hueSliderColor = finalColor
                                        }

                                        value.set(finalColor, false)

                                        // TODO: put it on style class
                                        with(WaitTickUtils) {
                                            if (!hasScheduled(value)) {
                                                conditionalSchedule(value, 10) {
                                                    (sliderValueHeld == null).also { if (it) saveConfig(valuesConfig) }
                                                }
                                            }
                                        }

                                        if (mouseButton == 0) {
                                            return true
                                        }
                                    }

                                    val previewSubSize = 6

                                    drawRect(
                                        startX + 75,
                                        startY - 10,
                                        startX + 75 + previewSubSize,
                                        startY - 10 + previewSubSize,
                                        currentColor.rgb
                                    )

                                    yPos += colorPickerHeight - previewSize + 24
                                }
                            }
                            yPos += 12
                        }

                        else -> {
                            val text = value.name + "§f: §c" + value.get()

                            moduleElement.settingsWidth = font35.getStringWidth(text) + 8

                            drawRect(minX, yPos + 2, maxX, yPos + 14, Int.MIN_VALUE)

                            font35.drawString(text, minX + 2, yPos + 4, Color.WHITE.rgb)

                            yPos += 12
                        }
                    }
                }

                moduleElement.settingsHeight = yPos - moduleElement.y - 4

                if (moduleElement.settingsWidth > 0 && yPos > moduleElement.y + 4) {
                    if (mouseButton != null && mouseX in minX..maxX && mouseY in moduleElement.y + 6..yPos + 2) return true

                    drawBorderedRect(minX, moduleElement.y + 6, maxX, yPos + 2, 1, Color.GRAY.rgb, 0)
                }
            }
        }
        return false
    }
}