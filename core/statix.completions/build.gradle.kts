plugins {
  id("org.metaborg.gradle.config.java-library")
  id("org.metaborg.gradle.config.junit-testing")
}

dependencies {
  api(platform(project(":spoofax.depconstraints")))
  testCompileOnly(platform(project(":spoofax.depconstraints")))
  testAnnotationProcessor(platform(project(":spoofax.depconstraints")))

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
  testImplementation("org.metaborg:log.backend.slf4j")
  testImplementation("org.slf4j:slf4j-simple:1.7.10")

  testAnnotationProcessor("org.immutables:value")
//  testAnnotationProcessor("org.immutables:serial")
  testCompileOnly("org.immutables:value")
//  testCompileOnly("org.immutables:serial")
  testCompileOnly("javax.annotation:javax.annotation-api")


}
