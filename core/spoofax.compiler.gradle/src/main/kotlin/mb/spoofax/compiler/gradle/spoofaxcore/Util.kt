package mb.spoofax.compiler.gradle.spoofaxcore

import mb.resource.ResourceRuntimeException
import mb.resource.ResourceService
import mb.resource.fs.FSPath
import mb.spoofax.compiler.util.GradleConfiguredDependency
import mb.spoofax.compiler.util.GradleDependency
import mb.spoofax.compiler.util.GradleProject
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.*

fun Project.toSpoofaxCompilerProject(): GradleProject {
  return GradleProject.builder()
    .coordinate(group.toString(), name, version.toString())
    .baseDirectory(FSPath(projectDir))
    .build()
}

fun Project.configureGroup(project: GradleProject) {
  this.group = project.coordinate().groupId()
}

fun Project.configureVersion(project: GradleProject) {
  this.version = project.coordinate().version()
}

fun Project.configureGeneratedSources(compilerProject: GradleProject, resourceService: ResourceService) {
  configure<SourceSetContainer> {
    named("main") {
      java {
        val dir = compilerProject.genSourceSpoofaxJavaDirectory()
        srcDir(resourceService.toLocalFile(dir)
          ?: throw GradleException("Cannot configure java sources directory, directory '$dir' is not on the local filesystem"))
      }
      resources {
        val dir = compilerProject.genSourceSpoofaxResourcesDirectory()
        srcDir(resourceService.toLocalFile(dir)
          ?: throw GradleException("Cannot configure resources directory, directory '$dir' is not on the local filesystem"))
      }
    }
  }
}

fun Project.deleteGenSourceSpoofaxDirectory(compilerProject: GradleProject, resourceService: ResourceService) {
  try {
    val genSourceDir = resourceService.getHierarchicalResource(compilerProject.genSourceSpoofaxDirectory())
    genSourceDir.delete(true)
  } catch(e: ResourceRuntimeException) {
    project.logger.warn("Failed to delete generated sources directory", e)
  }
}

fun GradleConfiguredDependency.addToDependencies(project: Project): Dependency {
  val configurationName = caseOf()
    .api_("api")
    .implementation_("implementation")
    .compileOnly_("compileOnly")
    .runtimeOnly_("runtimeOnly")
    .testImplementation_("testImplementation")
    .testCompileOnly_("testCompileOnly")
    .testRuntimeOnly_("testRuntimeOnly")
    .annotationProcessor_("annotationProcessor")
    .testAnnotationProcessor_("testAnnotationProcessor")
  val dependency = this.dependency.toGradleDependency(project)
  project.dependencies.add(configurationName, dependency)
  return dependency
}

fun GradleDependency.toGradleDependency(project: Project): Dependency {
  return caseOf()
    .project<Dependency> { project.dependencies.project(it) }
    .module { project.dependencies.create(it.groupId(), it.artifactId(), it.version()) }
    .files { project.dependencies.create(project.files(it)) }
}
