plugins {
    kotlin("jvm")
    `maven-publish`
}

dependencies {
    implementation("org.jetbrains.kotlin", "kotlin-stdlib", "1.4.10")
    implementation("org.slf4j", "slf4j-jdk14", "1.7.28")
}

publishing {
    publications {
        create<MavenPublication>("util") {
            from(components["java"])
        }
    }
}
