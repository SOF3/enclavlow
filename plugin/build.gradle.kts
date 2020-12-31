plugins {
    kotlin("jvm")
    `java-gradle-plugin`
    `maven-publish`
}

gradlePlugin {
    plugins {
        create("enclavlow-plugin") {
            id = "io.github.sof3.enclavlow.plugin"
            implementationClass = "io.github.sof3.enclavlow.plugin.Main"
        }
    }
}

repositories {
    jcenter()
}

dependencies {
    implementation("org.jetbrains.kotlin", "kotlin-stdlib", "1.4.10")
    implementation("org.jetbrains.kotlin", "kotlin-reflect", "1.4.10")
    implementation("org.jetbrains.kotlinx", "kotlinx-html-jvm", "0.7.1")
    implementation(project(":core"))
    implementation(project(":util"))
}
