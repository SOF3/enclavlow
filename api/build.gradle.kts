plugins {
    kotlin("jvm")
    `maven-publish`
}

dependencies {
    implementation("org.jetbrains.kotlin", "kotlin-stdlib", "1.4.10")
}

publishing {
    publications {
        create<MavenPublication>("api") {
            from(components["java"])
        }
    }
}
