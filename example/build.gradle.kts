repositories {
    jcenter()
}

buildscript {
    repositories {
        mavenLocal()
        jcenter()
    }
    dependencies {
        // classpath(files("../plugin/build/classes/kotlin/main"))
    }
}

plugins {
    kotlin("jvm") version "1.4.21"
    id("io.github.sof3.enclavlow.plugin") version "1.0-SNAPSHOT"
}

// apply {
    // plugin(io.github.sof3.enclavlow.plugin.Main::class.java)
// }

dependencies {
    implementation("org.jetbrains.kotlin", "kotlin-stdlib", "1.4.10")
    implementation("org.jetbrains.kotlin", "kotlin-reflect", "1.4.10")
}
