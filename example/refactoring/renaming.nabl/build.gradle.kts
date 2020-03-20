plugins {
  id("org.metaborg.gradle.config.root-project") version "0.3.19"
  id("org.metaborg.gitonium") version "0.1.2"
  id("org.metaborg.spoofax.gradle.langspec") version "0.2.1"
  `maven-publish`
}

spoofax {
  createPublication = true
}
