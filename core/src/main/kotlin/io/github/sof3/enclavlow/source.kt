package io.github.sof3.enclavlow

sealed class Source {
    abstract val name: String
}

/**
 * Data source from `this`
 */
object ThisSource : Source() {
    override val name: String
        get() = "this"
}

/**
 * Data source from a parameter
 */
class ParamSource(private val index: Int) : Source() {
    override val name: String
        get() = "param$index"
}

/**
 * Data source from a variable explicitly marked as `@Source`
 */
class VariableSource(override val name: String) : Source()
