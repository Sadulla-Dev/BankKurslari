// Standalone build (not part of the app build): renders the real shared Compose UI on the JVM to PNG
// for store screenshots. See ShotsKt (classpath via the `cp` task) and basemap.js for the map.
pluginManagement { repositories { gradlePluginPortal(); mavenCentral(); maven("https://maven.google.com") } }
dependencyResolutionManagement { repositories { mavenCentral(); maven("https://maven.google.com") } }
rootProject.name = "app-shots"
