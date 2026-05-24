pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/")
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
        mavenCentral()
        gradlePluginPortal()
        maven("https://oss.sonatype.org/content/repositories/snapshots")
        maven("https://repo.spongepowered.org/maven/")
        maven("https://repo.essential.gg/repository/maven-public")
        maven("https://maven.deftu.dev/releases")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version("0.6.0")
}

rootProject.name = extra["mod.name"].toString()
