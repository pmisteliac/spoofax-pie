plugins {
  id("org.metaborg.spoofax.gradle.langspec")
  id("de.set.ecj") // Use ECJ to speed up compilation of Stratego's generated Java files.
  `maven-publish`
}

dependencies {
  api(platform("org.metaborg:spoofax.depconstraints:$version"))
  implementation("org.metaborg:org.strategoxt.strj")
  implementation("org.metaborg:refactoring.common")
}

spoofax {
  createPublication = true
}

ecj {
  toolVersion = "3.20.0"
}
