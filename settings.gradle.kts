pluginManagement {
	repositories {
		maven("https://maven.fabricmc.net/")
		gradlePluginPortal()
		mavenCentral()
	}
}

dependencyResolutionManagement {
	repositories {
		maven("https://maven.fabricmc.net/")
		maven("https://maven.terraformersmc.com/releases/")
		mavenCentral()
	}
}

rootProject.name = "brightness-plus"
