plugins {
    id("net.minecraftforge.gradle") version "6.0.+"
    id("org.parchmentmc.librarian.forgegradle") version "1.+"
    id("org.jetbrains.kotlin.jvm") version "1.9.25"
}

version = project.extra["mod_version"] as String
group = project.extra["mod_group_id"] as String
val modId = project.extra["mod_id"] as String

base {
    archivesName.set(modId)
}

java.toolchain.languageVersion = JavaLanguageVersion.of(17)
kotlin.jvmToolchain(17)

minecraft {
    mappings("parchment", project.extra["parchment_mappings_version"] as String)

    runs {
        create("client") {
            workingDirectory(project.file("run"))
            property("forge.logging.markers", "REGISTRIES")
            property("forge.logging.console.level", "debug")
            property("guideDev.ae2guide.sources", file("src/main/resources/assets/appliedcreate/ae2guide").absolutePath)
            property("guideDev.ae2guide.sourcesNamespace", "appliedcreate")

            mods {
                create(modId) {
                    source(sourceSets.main.get())
                }
            }
        }

        create("server") {
            workingDirectory(project.file("run"))
            property("forge.logging.markers", "REGISTRIES")
            property("forge.logging.console.level", "debug")
            args("--nogui")

            mods {
                create(modId) {
                    source(sourceSets.main.get())
                }
            }
        }
    }
}

repositories {
    mavenLocal()
    mavenCentral()
    maven {
        name = "Kotlin for Forge"
        url = uri("https://thedarkcolour.github.io/KotlinForForge/")
        content { includeGroup("thedarkcolour") }
    }
    maven {
        name = "Modrinth"
        url = uri("https://api.modrinth.com/maven")
        content { includeGroup("maven.modrinth") }
    }
    maven {
        name = "Create Maven"
        url = uri("https://maven.tterrag.com/")
    }
    maven {
        name = "Create Mod Maven"
        url = uri("https://maven.createmod.net")
    }
    maven {
        name = "ModMaven"
        url = uri("https://modmaven.dev")
    }
}

dependencies {
    minecraft("net.minecraftforge:forge:${project.extra["minecraft_version"]}-${project.extra["forge_version"]}")

    implementation("thedarkcolour:kotlinforforge:${project.extra["kotlin_for_forge_version"]}")

    compileOnly(fg.deobf("com.simibubi.create:create-${project.extra["minecraft_version"]}:${project.extra["create_version"]}:slim"))

    compileOnly(fg.deobf("dev.engine-room.flywheel:flywheel-forge-${project.extra["minecraft_version"]}:1.0.0-beta-195"))

    // Ponder is needed as compile-only dependency because Create's MechanicalCrafterBlockEntity
    // extends VirtualBlockEntity from the Ponder library
    compileOnly(fg.deobf("net.createmod.ponder:Ponder-Forge-${project.extra["minecraft_version"]}:${project.extra["ponder_version"]}"))

    compileOnly(fg.deobf("net.createmod.catnip:Catnip-Forge-${project.extra["minecraft_version"]}:0.8.42"))

    compileOnly(fg.deobf("appeng:appliedenergistics2-forge:${project.extra["ae2_version"]}"))
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(
            "Specification-Title" to modId,
            "Specification-Vendor" to project.extra["mod_authors"],
            "Specification-Version" to "1",
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version,
            "Implementation-Vendor" to project.extra["mod_authors"]
        )
    }
    finalizedBy("reobfJar")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}
