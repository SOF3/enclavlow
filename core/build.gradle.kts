plugins {
    kotlin("jvm")
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
