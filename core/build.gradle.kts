import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions.apply {
    jvmTarget = "1.8"
}

dependencies {
    implementation("org.jetbrains.kotlin", "kotlin-stdlib", "1.4.10")
    implementation("org.soot-oss", "soot", "4.2.1")
    testImplementation("junit", "junit", "4.13")
    testImplementation("org.jetbrains.kotlin", "kotlin-test-junit")
    testCompileOnly(project(":api"))
}

tasks.test {
    useJUnit {
        setForkEvery(1)
    }
}
