import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    `maven-publish`
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions.apply {
    jvmTarget = "1.6"
}

dependencies {
    implementation("org.jetbrains.kotlin", "kotlin-stdlib", "1.4.10")
    implementation("org.soot-oss", "soot", "4.2.1")
    implementation(project(":api"))
    implementation(project(":util"))
    testImplementation("junit", "junit", "4.13")
    testImplementation("org.jetbrains.kotlin", "kotlin-test-junit")
}

publishing {
    publications {
        create<MavenPublication>("core") {
            from(components["java"])
        }
    }
}

tasks.test {
    doFirst {
        File(buildDir, "lfgOutput").mkdirs()
        val index = File(buildDir, "lfgOutput/index.html")
        index.writeText("<ul>\n")
    }
    doLast {
        val index = File(buildDir, "lfgOutput/index.html")
        index.writeText("</ul>\n")
    }
    useJUnit {
        maxParallelForks = Runtime.getRuntime().availableProcessors()
    }
}
