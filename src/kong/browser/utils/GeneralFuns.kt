package kong.browser.utils

inline fun <T> T?.whenNotNull(op: (T) -> Unit) {
    if (this != null) op(this)
}

inline fun <T> Array<T>.whenNotEmpty(op: (Array<T>) -> Unit) {
    if (this.size > 0) op(this)
}

inline fun <T> Array<T>.whenSingleton(op: (T) -> Unit) {
    if (this.size == 1) op(this[0])
}

