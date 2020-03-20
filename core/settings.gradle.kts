rootProject.name = "spoofax.core.root"

pluginManagement {
  repositories {
    maven("https://artifacts.metaborg.org/content/groups/public/")
  }
}

include("spoofax.depconstraints")

include("common")

include("completions.common")
include("jsglr.common")
include("jsglr1.common")
include("jsglr2.common")
include("esv.common")
include("stratego.common")
include("constraint.common")
include("nabl2.common")
include("statix.common")
include("spoofax2.common")
include("refactoring.common")

include("spoofax.core")
include("spoofax.cli")
include("spoofax.intellij")
include("spoofax.eclipse")
include("spoofax.eclipse.externaldeps")
include("spoofax.compiler")
include("spoofax.compiler.interfaces")
include("spoofax.compiler.gradle")
