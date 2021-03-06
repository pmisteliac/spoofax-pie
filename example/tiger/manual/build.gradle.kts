plugins {
  id("org.metaborg.gradle.config.root-project") version "0.3.19"
  id("org.metaborg.gitonium") version "0.1.2"

  // Set versions for plugins to use, only applying them in subprojects (apply false here).
  id("de.set.ecj") version "1.4.1" apply false
  id("org.metaborg.coronium.bundle") version "0.2.1" apply false
  id("org.metaborg.coronium.embedding") version "0.2.1" apply false
  id("biz.aQute.bnd.builder") version "4.3.1" apply false
  id("com.palantir.graal") version "0.6.0" apply false
  id("org.jetbrains.intellij") version "0.4.15" apply false
}

subprojects {
  metaborg {
    configureSubProject()
  }
}

gitonium {
  // Disable snapshot dependency checks for releases, until we depend on a stable version of MetaBorg artifacts.
  checkSnapshotDependenciesInRelease = false
}
