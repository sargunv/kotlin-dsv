import kotlinx.kover.gradle.plugin.dsl.GroupingEntityType
import ru.vyarus.gradle.plugin.mkdocs.task.MkdocsTask

plugins {
  id("org.jetbrains.kotlinx.kover")
  id("org.jetbrains.dokka")
  id("semver")
  id("ru.vyarus.mkdocs-build")
}

dokka {
  moduleName = "kotlin-dsv"
  dokkaPublications { html { outputDirectory = rootDir.absoluteFile.resolve("docs/api") } }
  pluginsConfiguration { html { footerMessage = "Copyright &copy; 2025 Sargun Vohra" } }
}

mkdocs {
  sourcesDir = "."
  strict = true
  publish {
    docPath = null // single version site
  }
}

tasks.withType<MkdocsTask>().configureEach {
  dependsOn("dokkaGenerateHtml")
  extras.assign(provider { mapOf("project_version" to project.version.toString()) })
}

kover {
  reports {
    total {
      log {
        // default groups by module
        groupBy = GroupingEntityType.PACKAGE
      }
    }
  }
}

dependencies {
  dokka(project(":kotlin-dsv"))
  kover(project(":kotlin-dsv"))
}
