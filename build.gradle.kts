plugins {
	id("fabric-loom") version "1.15.5"
	`maven-publish`
}

group = "net.fullbrightnoshading"
version = "1.0.0"

repositories {
	maven("https://maven.fabricmc.net/")
	maven("https://maven.terraformersmc.com/releases/")
	maven("https://maven.shedaniel.me/")
	mavenCentral()
}

base {
	archivesName.set("fullbright-no-shading")
}

dependencies {
	minecraft("com.mojang:minecraft:1.21.11")
	mappings("net.fabricmc:yarn:1.21.11+build.4:v2")
	modImplementation("net.fabricmc:fabric-loader:0.18.4")
	modImplementation("net.fabricmc.fabric-api:fabric-api:0.141.2+1.21.11")
	modImplementation("com.terraformersmc:modmenu:17.0.0-alpha.1")
	modImplementation("me.shedaniel.cloth:cloth-config-fabric:21.11.151") {
		exclude(group = "net.fabricmc.fabric-api")
	}
	include("me.shedaniel.cloth:cloth-config-fabric:21.11.151")
}

tasks.processResources {
	inputs.property("version", project.version)
	inputs.property("loader_version", project.property("loader_version").toString())
	inputs.property("minecraft_dependency", project.property("minecraft_dependency").toString())

	filesMatching("fabric.mod.json") {
		expand(
			"version" to project.version,
			"loader_version" to project.property("loader_version").toString(),
			"minecraft_dependency" to project.property("minecraft_dependency").toString()
		)
	}
}

java {
	toolchain {
		languageVersion.set(JavaLanguageVersion.of(21))
	}
}
