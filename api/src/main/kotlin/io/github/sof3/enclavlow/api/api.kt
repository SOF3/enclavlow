/**
 * This class receives function calls from generated code.
 *
 * @internal This class is semver-exempt. Do not call directly.
 */
@file:JvmName("Enclavlow")
@file:Suppress("unused")

package io.github.sof3.enclavlow.api

/**
 * A blackbox that marks a value security-sensitive
 *
 * @param source the value to coerce into sensitive data
 * @return the same value as `source`, but now always treated as a sensitive value
 */
fun byteSourceMarker(source: Byte) = source
/**
 * A blackbox that marks a value security-sensitive
 *
 * @param source the value to coerce into sensitive data
 * @return the same value as `source`, but now always treated as a sensitive value
 */
fun shortSourceMarker(source: Short) = source
/**
 * A blackbox that marks a value security-sensitive
 *
 * @param source the value to coerce into sensitive data
 * @return the same value as `source`, but now always treated as a sensitive value
 */
fun intSourceMarker(source: Int) = source
/**
 * A blackbox that marks a value security-sensitive
 *
 * @param source the value to coerce into sensitive data
 * @return the same value as `source`, but now always treated as a sensitive value
 */
fun longSourceMarker(source: Long) = source
/**
 * A blackbox that marks a value security-sensitive
 *
 * @param source the value to coerce into sensitive data
 * @return the same value as `source`, but now always treated as a sensitive value
 */
fun floatSourceMarker(source: Float) = source
/**
 * A blackbox that marks a value security-sensitive
 *
 * @param source the value to coerce into sensitive data
 * @return the same value as `source`, but now always treated as a sensitive value
 */
fun doubleSourceMarker(source: Double) = source
/**
 * A blackbox that marks a value security-sensitive
 *
 * @param source the value to coerce into sensitive data
 * @return the same value as `source`, but now always treated as a sensitive value
 */
fun charSourceMarker(source: Char) = source
/**
 * A blackbox that marks a value security-sensitive
 *
 * @param source the value to coerce into sensitive data
 * @return the same value as `source`, but now always treated as a sensitive value
 */
fun booleanSourceMarker(source: Boolean) = source

/**
 * A blackbox that marks a value security-sensitive
 *
 * @param source the value to coerce into sensitive data
 * @return the same value as `source`, but now always treated as a sensitive value
 */
fun <T> sourceMarker(source: T) = source

/**
 * A blackbox that marks a value security-insensitive.
 *
 * @param sink the value to coerce int oinsensitive data
 * @return the same value as `sink`, but now always treated as an insensitive value
 */
fun byteSinkMarker(sink: Byte) = sink
/**
 * A blackbox that marks a value security-insensitive.
 *
 * @param sink the value to coerce int oinsensitive data
 * @return the same value as `sink`, but now always treated as an insensitive value
 */
fun shortSinkMarker(sink: Short) = sink
/**
 * A blackbox that marks a value security-insensitive.
 *
 * @param sink the value to coerce int oinsensitive data
 * @return the same value as `sink`, but now always treated as an insensitive value
 */
fun intSinkMarker(sink: Int) = sink
/**
 * A blackbox that marks a value security-insensitive.
 *
 * @param sink the value to coerce int oinsensitive data
 * @return the same value as `sink`, but now always treated as an insensitive value
 */
fun longSinkMarker(sink: Long) = sink
/**
 * A blackbox that marks a value security-insensitive.
 *
 * @param sink the value to coerce int oinsensitive data
 * @return the same value as `sink`, but now always treated as an insensitive value
 */
fun floatSinkMarker(sink: Float) = sink
/**
 * A blackbox that marks a value security-insensitive.
 *
 * @param sink the value to coerce int oinsensitive data
 * @return the same value as `sink`, but now always treated as an insensitive value
 */
fun doubleSinkMarker(sink: Double) = sink
/**
 * A blackbox that marks a value security-insensitive.
 *
 * @param sink the value to coerce int oinsensitive data
 * @return the same value as `sink`, but now always treated as an insensitive value
 */
fun charSinkMarker(sink: Char) = sink
/**
 * A blackbox that marks a value security-insensitive.
 *
 * @param sink the value to coerce int oinsensitive data
 * @return the same value as `sink`, but now always treated as an insensitive value
 */
fun booleanSinkMarker(sink: Boolean) = sink

/**
 * A blackbox that marks a value security-insensitive.
 *
 * @param sink the value to coerce into insensitive data
 * @return the same value as `sink`, but now always treated as an insensitive value
 */
fun <T> sinkMarker(sink: T) = sink
