plugins {
  id("org.metaborg.gradle.config.java-library")
}

dependencies {
  api(platform(project(":spoofax.depconstraints")))

  api(project(":common"))
  api(project(":completions.common"))
  api(project(":statix.common"))
  api("org.metaborg:statix.solver")
  api("org.metaborg:statix.generator")
  implementation("one.util:streamex")


  compileOnly("org.checkerframework:checker-qual-android")

  testImplementation(platform(project(":spoofax.depconstraints")))
  testCompileOnly("org.checkerframework:checker-qual-android")
  testImplementation("nl.jqno.equalsverifier:equalsverifier")

}
