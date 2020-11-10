import com.vanniktech.dependency.graph.generator.DependencyGraphGeneratorExtension
import com.vanniktech.dependency.graph.generator.DependencyGraphGeneratorExtension.Generator
import guru.nidi.graphviz.attribute.Color
import guru.nidi.graphviz.attribute.Style
import guru.nidi.graphviz.model.MutableNode
import kotlin.random.Random

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
    id("com.vanniktech.dependency.graph.generator") version "0.5.0"
}

configure<DependencyGraphGeneratorExtension> {
    fun colorNode(node: MutableNode, name: String): MutableNode {
        val random = Random(name.hashCode())
        val r = random.nextInt(256)
        val g = random.nextInt(256)
        val b = random.nextInt(256)
        node.add(Style.FILLED, Color.rgb((r shl 16) or (g shl 8) or b))
        val light = 0.299 * r + 0.587 * g + 0.114 * b
        if (light <= 152.0) {
            node.add("fontcolor", "#ffffff")
        }

        return node
    }
    generators = listOf(Generator(
        dependencyNode = { node, dependency -> colorNode(node, dependency.moduleGroup) },
        projectNode = { node, project -> colorNode(node, project.group.toString()) }
    ))
}
