plugins {
    id("net.neoforged.moddev") version "1.0.15"
    id("org.jetbrains.kotlin.jvm") version "2.3.0"
}

version = project.extra["mod_version"] as String
group = project.extra["mod_group_id"] as String
val modId = project.extra["mod_id"] as String

base {
    archivesName.set(modId)
}

java.toolchain.languageVersion = JavaLanguageVersion.of(21)
kotlin.jvmToolchain(21)

neoForge {
    version = project.extra["neoforge_version"] as String

    mods {
        create(modId) {
            sourceSet(sourceSets.main.get())
        }
    }

    runs {
        create("client") {
            client()
            systemProperty("guideDev.ae2guide.sources", file("src/main/resources/assets/appliedcreate/ae2guide").absolutePath)
            systemProperty("guideDev.ae2guide.sourcesNamespace", "appliedcreate")
        }
        create("server") {
            server()
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
        name = "Create Mod Maven"
        url = uri("https://maven.createmod.net")
    }
    maven {
        name = "ModMaven"
        url = uri("https://modmaven.dev")
    }
}

dependencies {
    implementation("thedarkcolour:kotlinforforge-neoforge:${project.extra["kotlin_for_forge_version"]}")

    implementation("org.appliedenergistics:appliedenergistics2:${project.extra["ae2_version"]}")
    implementation("org.appliedenergistics:guideme:${project.extra["guideme_version"]}")

    compileOnly("com.simibubi.create:create-${project.extra["minecraft_version"]}:${project.extra["create_version"]}")
    compileOnly("dev.engine-room.flywheel:flywheel-neoforge-${project.extra["minecraft_version"]}:${project.extra["flywheel_version"]}")
    compileOnly("net.createmod.ponder:ponder-neoforge:${project.extra["ponder_version"]}")
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
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.withType<ProcessResources>().configureEach {
    val replaceProperties = mapOf(
        "mod_id" to modId,
        "mod_name" to project.extra["mod_name"],
        "mod_license" to project.extra["mod_license"],
        "mod_version" to project.version,
        "mod_authors" to project.extra["mod_authors"],
        "mod_description" to project.extra["mod_description"],
        "neoforge_version" to project.extra["neoforge_version"],
        "minecraft_version" to project.extra["minecraft_version"],
        "loader_version_range" to project.extra["loader_version_range"],
        "neo_version_range" to project.extra["neo_version_range"],
        "minecraft_version_range" to project.extra["minecraft_version_range"],
        "create_version_range" to project.extra["create_version_range"],
        "ae2_version_range" to project.extra["ae2_version_range"]
    )

    inputs.properties(replaceProperties)

    filesMatching("META-INF/neoforge.mods.toml") {
        expand(replaceProperties)
    }
}
