pluginManagement {
    repositories {
        google() // ¡QUITAR el bloque 'content' de aquí!
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "OrganizacionPersonal"
include(":app")