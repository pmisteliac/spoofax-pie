rootProject.name = "spoofax.root"

pluginManagement {
  repositories {
    maven("https://artifacts.metaborg.org/content/groups/public/")
  }
}

// Only include composite builds when this is the root project (it has no parent). Otherwise, the parent project
// (devenv) will include these composite builds, as IntelliJ does not support nested composite builds.
if(gradle.parent == null) {
  // We split the build up into one main composite build in the 'core' directory, because it builds Gradle plugins,
  // which we want to test. Gradle plugins are not directly available in a multi-project build, therefore a separate
  // composite build is required.
  includeBuild("core")
  // Included builds listed below can use the Gradle plugins built in 'core'.
  includeBuild("example/tiger/spoofaxcore")
  includeBuild("example/tiger/generated")
  includeBuild("example/tiger/manual")
  includeBuild("example/mod")
  includeBuild("example/sdf3")

  includeBuild("example/refactoring/renaming.nabl")
  includeBuild("example/refactoring/renaming.statix")
  includeBuild("example/refactoring/tiger.nabl")
  includeBuild("example/refactoring/tiger.statix")
}
