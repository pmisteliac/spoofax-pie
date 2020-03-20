rootProject.name = "spoofax.example.renaming.nabl"

pluginManagement {
  repositories {
    maven("https://artifacts.metaborg.org/content/groups/public/")
  }
}

// Only include composite builds when this is the root project (it has no parent), for example when running Gradle tasks
// from the command-line. Otherwise, the parent project (spoofax) will include these composite builds.
if(gradle.parent == null) {
  includeBuild("../../../core")
}

include("tiger.nabl.spoofaxcore")
include("tiger.nabl")
include("tiger.nabl.spoofax")
include("tiger.nabl.eclipse.externaldeps")
include("tiger.nabl.eclipse")
