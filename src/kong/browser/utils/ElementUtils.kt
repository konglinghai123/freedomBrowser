package kong.browser.utils

import org.jdom.Element
import java.util.*
import kotlin.reflect.KProperty

operator fun Element.getValue(thisRef: Any?, property: KProperty<*>): String {
    return this.getAttribute(property.name).value.toString()
}

operator fun Element.setValue(thisRef: Any?, property: KProperty<*>, value: String) {
    this.setAttribute(property.name, value)
}

fun xmlSafeUUID(): String {
    return "x${UUID.randomUUID().toString()}"
}
