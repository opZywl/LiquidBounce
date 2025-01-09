/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.clickgui.style.styles

import net.ccbluex.liquidbounce.config.*
import net.ccbluex.liquidbounce.features.module.modules.render.ClickGUI.guiColor
import net.ccbluex.liquidbounce.features.module.modules.render.ClickGUI.scale
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
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.util.StringUtils
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import java.awt.Color
import kotlin.math.abs
import kotlin.math.roundToInt

@SideOnly(Side.CLIENT)
object NullStyle : Style() {
    private fun getNegatedColor() = guiColor.inv()
    override fun drawPanel(mouseX: Int, mouseY: Int, panel: Panel) {
        drawRect(panel.x - 3, panel.y, panel.x + panel.width + 3, panel.y + 19, guiColor)

        if (panel.fade > 0)
            drawBorderedRect(
                panel.x,
                panel.y + 19,
                panel.x + panel.width,
                panel.y + 19 + panel.fade,
                1,
                Int.MIN_VALUE,
                Int.MIN_VALUE
            )

        val xPos = panel.x - (font35.getStringWidth("§f" + StringUtils.stripControlCodes(panel.name)) - 100) / 2
        font35.drawString(panel.name, xPos, panel.y + 7, getNegatedColor())
    }

    override fun drawHoverText(mouseX: Int, mouseY: Int, text: String) {
        val lines = text.lines()

        val width =
            lines.maxOfOrNull { font35.getStringWidth(it) + 14 } ?: return // Makes no sense to render empty lines
        val height = (font35.fontHeight * lines.size) + 3

        // Don't draw hover text beyond window boundaries
        val (scaledWidth, scaledHeight) = ScaledResolution(mc)
        val x = mouseX.clamp(0, (scaledWidth / scale - width).roundToInt())
        val y = mouseY.clamp(0, (scaledHeight / scale - height).roundToInt())

        drawRect(x + 9, y, x + width, y + height, guiColor)
        lines.forEachIndexed { index, text ->
            font35.drawString(text, x + 12, y + 3 + (font35.fontHeight) * index, getNegatedColor())
        }
    }

    override fun drawButtonElement(mouseX: Int, mouseY: Int, buttonElement: ButtonElement) {
        val xPos = buttonElement.x - (font35.getStringWidth(buttonElement.displayName) - 100) / 2
        font35.drawString(buttonElement.displayName, xPos, buttonElement.y + 6, buttonElement.color)
    }

    override fun drawModuleElementAndClick(
        mouseX: Int,
        mouseY: Int,
        moduleElement: ModuleElement,
        mouseButton: Int?
    ): Boolean {
        val xPos = moduleElement.x - (font35.getStringWidth(moduleElement.displayName) - 100) / 2
        font35.drawString(
            moduleElement.displayName,
            xPos,
            moduleElement.y + 6,
            if (moduleElement.module.state) {
                if (moduleElement.module.isActive) guiColor
                // Make inactive modules have alpha set to 100
                else (guiColor and 0x00FFFFFF) or (0x64 shl 24)
            } else Int.MAX_VALUE
        )

        val moduleValues = moduleElement.module.values.filter { it.shouldRender() }
        if (moduleValues.isNotEmpty()) {
            font35.drawString(
                if (moduleElement.showSettings) "-" else "+",
                moduleElement.x + moduleElement.width - 8, moduleElement.y + moduleElement.height / 2, Color.WHITE.rgb
            )

            if (moduleElement.showSettings) {
                var yPos = moduleElement.y + 4

                val minX = moduleElement.x + moduleElement.width + 4
                val maxX = moduleElement.x + moduleElement.width + moduleElement.settingsWidth

                if (moduleElement.settingsWidth > 0 && moduleElement.settingsHeight > 0) drawBorderedRect(
                    minX,
                    yPos,
                    maxX,
                    yPos + moduleElement.settingsHeight,
                    1,
                    0,
                    Int.MIN_VALUE
                )

                for (value in moduleValues) {
                    assumeNonVolatile = value.get() is Number

                    val suffix = value.suffix ?: ""

                    when (value) {
                        is BoolValue -> {
                            val text = value.name

                            moduleElement.settingsWidth = font35.getStringWidth(text) + 8

                            if (mouseButton == 0
                                && mouseX in minX..maxX
                                && mouseY in yPos + 2..yPos + 14
                            ) {
                                value.toggle()
                                clickSound()
                                return true
                            }

                            font35.drawString(
                                text, minX + 2, yPos + 4,
                                if (value.get()) guiColor else Int.MAX_VALUE
                            )

                            yPos += 12
                        }

                        is ListValue -> {
                            val text = value.name

                            moduleElement.settingsWidth = font35.getStringWidth(text) + 16

                            if (mouseButton == 0
                                && mouseX in minX..maxX
                                && mouseY in yPos + 2..yPos + 14
                            ) {
                                value.openList = !value.openList
                                clickSound()
                                return true
                            }

                            font35.drawString("§c$text", minX + 2, yPos + 4, Color.WHITE.rgb)
                            font35.drawString(
                                if (value.openList) "-" else "+",
                                (maxX - if (value.openList) 5 else 6), yPos + 4, Color.WHITE.rgb
                            )

                            yPos += 12

                            for (valueOfList in value.values) {
                                moduleElement.settingsWidth = font35.getStringWidth(valueOfList) + 16

                                if (value.openList) {
                                    if (mouseButton == 0
                                        && mouseX in minX..maxX
                                        && mouseY in yPos + 2..yPos + 14
                                    ) {
                                        value.set(valueOfList)
                                        clickSound()
                                        return true
                                    }

                                    font35.drawString(
                                        ">", minX + 2, yPos + 4,
                                        if (value.get() == valueOfList) guiColor else Int.MAX_VALUE
                                    )
                                    font35.drawString(
                                        valueOfList, minX + 10, yPos + 4,
                                        if (value.get() == valueOfList) guiColor else Int.MAX_VALUE
                                    )

                                    yPos += 12
                                }
                            }
                        }

                        is FloatValue -> {
                            val text = value.name + "§f: §c" + round(value.get()) + " §8${suffix}§c"

                            moduleElement.settingsWidth = font35.getStringWidth(text) + 8

                            if (mouseButton == 0 && mouseX in minX..maxX && mouseY in yPos + 15..yPos + 21 || sliderValueHeld == value) {
                                val percentage = (mouseX - minX - 4) / (maxX - minX - 8).toFloat()
                                value.set(value.lerpWith(percentage).coerceIn(value.range))

                                // Keep changing this slider until mouse is unpressed.
                                sliderValueHeld = value

                                // Stop rendering and interacting only when this event was triggered by a mouse click.
                                if (mouseButton == 0) return true
                            }

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

                            if (mouseButton == 0 && mouseX in minX..maxX && mouseY in yPos + 15..yPos + 21 || sliderValueHeld == value) {
                                val percentage = (mouseX - minX - 4) / (maxX - minX - 8).toFloat()
                                value.set(value.lerpWith(percentage).coerceIn(value.range))

                                // Keep changing this slider until mouse is unpressed.
                                sliderValueHeld = value

                                // Stop rendering and interacting only when this event was triggered by a mouse click.
                                if (mouseButton == 0) return true
                            }

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

                            if (mouseButton == 0 && mouseX in minX..maxX && mouseY in yPos + 15..yPos + 21 || sliderValueHeld == value) {
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

                            if (mouseButton == 0 && mouseX in minX..maxX && mouseY in yPos + 15..yPos + 21 || sliderValueHeld == value) {
                                val slider1Pos =
                                    minX + ((slider1 - value.minimum) / (value.maximum - value.minimum)) * (maxX - minX)
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

                            if (mouseButton != null
                                && mouseX in minX..maxX
                                && mouseY in yPos + 4..yPos + 12
                            ) {
                                // Cycle to next font when left-clicked, previous when right-clicked.
                                if (mouseButton == 0) value.next()
                                else value.previous()
                                clickSound()
                                return true
                            }

                            font35.drawString(displayString, minX + 2, yPos + 4, Color.WHITE.rgb)

                            yPos += 11
                        }

                        is ColorValue -> {
                            val currentColor = if (value.rainbow) {
                                value.get()
                            } else {
                                value.get()
                            }

                            val spacing = 10

                            val startX = moduleElement.x + moduleElement.width + 4
                            val startY = yPos - 1

                            // Color preview
                            val colorPreviewSize = 10
                            val colorPreviewX2 = maxX - colorPreviewSize
                            val colorPreviewX1 = colorPreviewX2 - colorPreviewSize
                            val colorPreviewY1 = startY + 2
                            val colorPreviewY2 = colorPreviewY1 + colorPreviewSize

                            // Text
                            val textX = startX + 2F
                            val textY = startY + 4F

                            // Sliders
                            val hueSliderWidth = 7
                            val hueSliderHeight = 50
                            val colorPickerWidth = 75
                            val colorPickerHeight = 50

                            val colorPickerStartX = textX.toInt()
                            val colorPickerEndX = colorPickerStartX + colorPickerWidth
                            val colorPickerStartY = colorPreviewY2 + spacing / 3
                            val colorPickerEndY = colorPickerStartY + colorPickerHeight

                            val hueSliderStartY = colorPickerStartY
                            val hueSliderEndY = colorPickerStartY + hueSliderHeight

                            if (mouseButton == 0 && mouseX in colorPreviewX1..colorPreviewX2 && mouseY in colorPreviewY1..colorPreviewY2) {
                                value.showPicker = !value.showPicker
                                clickSound()
                                return true
                            }

                            val display = "Color: ${"#%08X".format(currentColor.rgb)}"

                            moduleElement.settingsWidth = font35.getStringWidth(display) + 8

                            font35.drawStringWithShadow(display, textX, textY, Color.WHITE.rgb)

                            drawRect(colorPreviewX1, colorPreviewY1, colorPreviewX2, colorPreviewY2, currentColor.rgb)

                            // Rainbow rectangle next to it?
                            // Maybe with a blue border to indicate which one is chosen?

                            if (value.showPicker) {
                                if (!value.rainbow) {
                                    val hueSliderColor = value.hueSliderColor

                                    var (hue, saturation, brightness) = Color.RGBtoHSB(
                                        hueSliderColor.red, hueSliderColor.green, hueSliderColor.blue, null
                                    )
                                    var (currHue, currSaturation, currBrightness) = Color.RGBtoHSB(
                                        currentColor.red, currentColor.green, currentColor.blue, null
                                    )

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
                                            drawTexture(
                                                id,
                                                colorPickerStartX,
                                                colorPickerStartY,
                                                colorPickerWidth,
                                                colorPickerHeight
                                            )
                                        })

                                    val markerX = colorPickerStartX + (currSaturation * colorPickerWidth).toInt()
                                    val markerY =
                                        colorPickerStartY + ((1.0f - currBrightness) * colorPickerHeight).toInt()

                                    RenderUtils.drawBorder(
                                        markerX - 2f, markerY - 2f, markerX + 3f, markerY + 3f, 1.5f, Color.WHITE.rgb
                                    )

                                    val hueSliderX = colorPickerEndX + 5

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
                                            drawTexture(
                                                id,
                                                hueSliderX,
                                                colorPickerStartY,
                                                hueSliderWidth,
                                                hueSliderHeight
                                            )
                                        })

                                    val hueMarkerY = hueSliderStartY + (hue * hueSliderHeight)

                                    RenderUtils.drawBorder(
                                        hueSliderX.toFloat() - 1,
                                        hueMarkerY - 1f,
                                        hueSliderX + hueSliderWidth + 1f,
                                        (hueMarkerY + 1).coerceAtMost((hueSliderEndY).toFloat()) + 1,
                                        1.5f,
                                        Color.WHITE.rgb,
                                    )

                                    val inColorPicker =
                                        mouseX in colorPickerStartX until colorPickerEndX && mouseY in colorPickerStartY until colorPickerEndY
                                    val inHueSlider =
                                        mouseX in hueSliderX - 1..hueSliderX + hueSliderWidth + 1 && mouseY in hueSliderStartY until hueSliderEndY

                                    if ((mouseButton == 0 || sliderValueHeld == value) && (inColorPicker || inHueSlider)) {
                                        if (inColorPicker) {
                                            val newS =
                                                ((mouseX - colorPickerStartX) / colorPickerWidth.toFloat()).coerceIn(
                                                    0f,
                                                    1f
                                                )
                                            val newB =
                                                (1.0f - (mouseY - colorPickerStartY) / colorPickerHeight.toFloat()).coerceIn(
                                                    0f,
                                                    1f
                                                )
                                            saturation = newS
                                            brightness = newB
                                        }

                                        var finalColor = Color(Color.HSBtoRGB(hue, saturation, brightness))

                                        if (inHueSlider) {
                                            currHue = ((mouseY - hueSliderStartY) / hueSliderHeight.toFloat()).coerceIn(
                                                0f,
                                                1f
                                            )

                                            finalColor = Color(Color.HSBtoRGB(currHue, currSaturation, currBrightness))
                                        }

                                        sliderValueHeld = value

                                        if (inHueSlider) {
                                            value.hueSliderColor = finalColor
                                        }

                                        value.setAndSaveValueOnButtonRelease(finalColor)

                                        if (mouseButton == 0) {
                                            return true
                                        }
                                    }
                                    yPos += colorPickerHeight - colorPreviewSize + spacing + 6
                                }
                            }
                            yPos += spacing
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
                    if (mouseButton != null && mouseX in minX..maxX && mouseY in moduleElement.y + 6..yPos + 2) {
                        return true
                    }
                }
            }
        }
        return false
    }
}