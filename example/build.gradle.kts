repositories {
    mavenLocal()
    jcenter()
}

buildscript {
    repositories {
        mavenLocal()
        jcenter()
    }
}

plugins {
    kotlin("jvm") version "1.4.21"
    id("io.github.sof3.enclavlow.plugin") version "1.0-SNAPSHOT"
}

dependencies {
    implementation("io.github.sof3.enclavlow", "api", "1.0-SNAPSHOT")
}
