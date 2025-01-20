/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.config

import com.google.gson.JsonElement
import net.ccbluex.liquidbounce.file.FileManager.saveConfig
import net.ccbluex.liquidbounce.file.FileManager.valuesConfig
import net.ccbluex.liquidbounce.utils.client.ClientUtils.LOGGER
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

private typealias OnChangeInterceptor<T> = (old: T, new: T) -> T
private typealias OnChangedHandler<T> = (new: T) -> Unit

sealed class Value<T>(
    val name: String,
    protected var value: T,
    val suffix: String? = null,
    protected var default: T = value,
) : ReadWriteProperty<Any?, T> {

    /**
     * Whether this value should be excluded from public configuration (text config)
     */
    var subjective: Boolean = false
        private set

    fun subjective() = apply { subjective = true }

    var excluded: Boolean = false
        private set

    fun exclude() = apply { excluded = true }

    fun excludeWhen(condition: Boolean) = apply {
        if (condition) {
            excluded = true
        }
    }

    fun setAndUpdateDefault(new: T): Boolean {
        default = new

        return set(new)
    }

    fun set(newValue: T, saveImmediately: Boolean = true): Boolean {
        if (newValue == value || excluded) {
            return false
        }

        val oldValue = value

        try {
            var handledValue = validate(newValue)
            onChangeInterceptors.forEach { handledValue = it(oldValue, handledValue) }

            if (handledValue == oldValue) {
                return false
            }

            changeValue(handledValue)
            onChangedListeners.forEach { it.invoke(handledValue) }

            if (saveImmediately) {
                saveConfig(valuesConfig)
            }
            return true
        } catch (e: Exception) {
            LOGGER.error("[ValueSystem ($name)]: ${e.javaClass.name} (${e.message}) [$oldValue >> $newValue]")
            return false
        }
    }

    /**
     * Excludes the chosen option [value] from the config system.
     *
     * [state] the value it will be set to before it is excluded.
     */
    fun excludeWithState(state: T = value) {
        setAndUpdateDefault(state)

        excluded = true
    }

    fun get() = value

    fun changeValue(newValue: T) {
        value = newValue
    }

    // Serializations: JSON/Text

    abstract fun toJson(): JsonElement?
    open fun toText(): String = value.toString()

    protected abstract fun fromJsonF(element: JsonElement): T?
    protected abstract fun fromTextF(text: String): T?

    fun fromJson(element: JsonElement) {
        val result = fromJsonF(element) ?: return
        changeValue(result)

        onChangedListeners.forEach { it.invoke(result) }
    }

    fun fromText(text: String) {
        val result = fromTextF(text) ?: return
        changeValue(result)

        onChangedListeners.forEach { it.invoke(result) }
    }

    // Serializations END

    private var onChangeInterceptors: Array<OnChangeInterceptor<T>> = emptyArray()
    private var onChangedListeners: Array<OnChangedHandler<T>> = emptyArray()

    fun onChange(interceptor: OnChangeInterceptor<T>) = apply {
        onChangeInterceptors += interceptor
    }

    fun onChanged(handler: OnChangedHandler<T>) = apply {
        this.onChangedListeners += handler
    }

    private var supportCondition = { true }

    fun isSupported() = supportCondition.invoke()

    fun setSupport(condition: (Boolean) -> Boolean) = apply {
        val oldCondition = supportCondition
        supportCondition = { condition(oldCondition.invoke()) }
    }

    /**
     * Make the value able to set.
     */
    open fun validate(newValue: T): T = newValue

    // Support for delegating values using the `by` keyword.
    override operator fun getValue(thisRef: Any?, property: KProperty<*>) = value
    override operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        set(value)
    }

    fun shouldRender() = isSupported() && !excluded

    fun resetValue() = set(default)
}
