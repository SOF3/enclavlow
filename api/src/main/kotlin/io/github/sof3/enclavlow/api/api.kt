package io.github.sof3.enclavlow.api

@Target(AnnotationTarget.LOCAL_VARIABLE, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.BINARY)
annotation class Source

@Target(AnnotationTarget.LOCAL_VARIABLE, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class Sink
