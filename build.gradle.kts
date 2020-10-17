group = "io.github.sof3"
version = "1.0-SNAPSHOT"

allprojects {
    repositories {
        mavenCentral()
        maven("https://repo1.maven.org/maven2")
    }
}

subprojects {
    group = "io.github.sof3"
    version = "1.0-SNAPSHOT"
}

buildscript {
    repositories {
        jcenter()
    }
}

plugins {
    kotlin("jvm") version "1.4.10"
}
