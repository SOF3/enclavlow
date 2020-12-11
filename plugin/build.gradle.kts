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

dependencies {
    implementation("org.jetbrains.kotlin", "kotlin-stdlib", "1.4.10")
    implementation("org.jetbrains.kotlin", "kotlin-reflect", "1.4.10")
    implementation(project(":core"))
    implementation(project(":util"))
}
