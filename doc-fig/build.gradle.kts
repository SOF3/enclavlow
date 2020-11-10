plugins {
    application
    java
    kotlin("jvm")
}

val compileKotlin: org.jetbrains.kotlin.gradle.tasks.KotlinCompile by tasks
compileKotlin.kotlinOptions.apply {
    jvmTarget = "1.6"
}

dependencies {
    implementation("org.jetbrains.kotlin", "kotlin-stdlib", "1.4.10")
    implementation("org.jetbrains.kotlin", "kotlin-reflect", "1.4.10")
    implementation(project(":api"))
    implementation(project(":core"))
    implementation("org.soot-oss", "soot", "4.2.1")
}

application {
    mainClassName = "io.github.sof3.enclavlow.docfig.MainKt"
}
