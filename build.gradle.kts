import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    idea
    java
    kotlin("jvm") version "2.2.0"
    id("fabric-loom") version "1.15-SNAPSHOT"
}

val baseGroup = project.properties["mod.group"].toString()
val mcVersion = project.properties["minecraft_version"].toString()
val modId = project.properties["mod.id"].toString()
val modName = project.properties["mod.name"].toString()
val modVersion = project.properties["mod.version"].toString()

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

sourceSets.main {
    kotlin.destinationDirectory.set(java.destinationDirectory)
}

loom {
    mixin {
        useLegacyMixinAp.set(true)
        defaultRefmapName.set("mixins.$modId.refmap.json")
    }
    runConfigs {
        named("client") {
            property("mixin.debug", "true")
        }
        remove(named("server").get())
    }
}

// Standalone embed config — does NOT extend implementation to avoid
// Gson/Loom configuration-phase conflicts (Gson 2.9.1 module access issue).
val embed: Configuration by configurations.creating

// Helper: add to both embed (for jar bundling) and implementation (for compilation).
fun DependencyHandler.bundled(notation: Any) {
    add("embed", notation)
    add("implementation", notation)
}

repositories {
    mavenCentral()
    maven("https://repo.spongepowered.org/maven/")
    maven("https://repo.essential.gg/repository/maven-public")
    maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1")
    maven("https://maven.deftu.dev/releases")
}

dependencies {
    minecraft("com.mojang:minecraft:${project.properties["minecraft_version"]}")
    mappings("net.fabricmc:yarn:${project.properties["yarn_mappings"]}:v2")
    modImplementation("net.fabricmc:fabric-loader:${project.properties["loader_version"]}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${project.properties["fabric_version"]}")
    modImplementation("net.fabricmc:fabric-language-kotlin:${project.properties["fabric_kotlin_version"]}")

    bundled("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.10.2")
    bundled("com.squareup.okhttp3:okhttp-jvm:5.2.1")

    // TODO: Find correct elementa version for Fabric 1.21.11 at repo.essential.gg
    // bundled("gg.essential:elementa:710")

    // TODO: Find correct universalcraft version for Fabric 1.21.11 at repo.essential.gg
    // bundled("gg.essential:universalcraft-1.21.11-fabric:330")

    // TODO: Check https://maven.deftu.dev/releases for vexel-1.21.11-fabric
    // bundled("xyz.meowing:vexel-1.21.11-fabric:110")

    modRuntimeOnly("me.djtheredstoner:DevAuth-fabric:1.2.1")
}

tasks.compileJava {
    dependsOn(tasks.processResources)
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        freeCompilerArgs.add("-Xlambdas=class")
        languageVersion.set(KotlinVersion.KOTLIN_2_0)
    }
}

tasks.register("generateLists") {
    val srcDir = file("src/main/kotlin/xyz/meowing/zen")
    val featureOutput = file("build/generated/resources/features.list")
    val commandOutput = file("build/generated/resources/commands.list")

    val moduleRegex = Regex("@Zen\\.Module\\s*(?:\\n|\\s)*(?:object|class)\\s+(\\w+)")
    val commandRegex = Regex("@Zen\\.Command\\s*(?:\\n|\\s)*(?:object|class)\\s+(\\w+)")
    val pkgRegex = Regex("package\\s+([\\w.]+)")

    inputs.dir(srcDir).optional(true)
    outputs.files(featureOutput, commandOutput)

    doLast {
        val featureClasses = mutableListOf<String>()
        val commandClasses = mutableListOf<String>()

        if (!srcDir.exists()) return@doLast

        srcDir.walkTopDown().forEach { file ->
            if (file.isFile && file.extension in listOf("kt", "java")) {
                val text = file.readText()
                val pkg = pkgRegex.find(text)?.groupValues?.get(1) ?: return@forEach

                moduleRegex.findAll(text).forEach { match ->
                    featureClasses += "${pkg}.${match.groupValues[1]}"
                }

                commandRegex.findAll(text).forEach { match ->
                    commandClasses += "${pkg}.${match.groupValues[1]}"
                }
            }
        }

        featureOutput.parentFile.mkdirs()
        commandOutput.parentFile.mkdirs()
        featureOutput.writeText(featureClasses.joinToString("\n"))
        commandOutput.writeText(commandClasses.joinToString("\n"))
    }
}

tasks.processResources {
    inputs.property("mod_version", modVersion)
    inputs.property("mc_version", mcVersion)
    inputs.property("mod_id", modId)
    inputs.property("mod_group", baseGroup)

    filesMatching(listOf("fabric.mod.json", "mixins.$modId.json")) {
        expand(inputs.properties)
    }

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    dependsOn("generateLists")
    from("build/generated/resources")
}

tasks.jar {
    from(embed.map { if (it.isDirectory) it else zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    archiveBaseName.set("zen-1.21.11-fabric")
    archiveClassifier.set("dev")
}

tasks.remapJar {
    archiveClassifier.set("")
    archiveBaseName.set("zen-1.21.11-fabric-${modVersion}")
}

tasks.assemble.get().dependsOn(tasks.remapJar)
