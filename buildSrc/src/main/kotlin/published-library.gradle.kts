import dev.detekt.gradle.extensions.FailOnSeverity
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
  id("multiplatform-module")
  id("org.jetbrains.dokka")
  id("com.vanniktech.maven.publish")
  id("org.jetbrains.kotlinx.kover")
  id("dev.detekt")
}

group = "dev.sargunv.kotlin-dsv"

version = providers.gradleProperty("kotlinDsvVersion").get()

kotlin {
  explicitApi()
  compilerOptions {
    freeCompilerArgs.add(
      // Will be the default soon: https://youtrack.jetbrains.com/issue/KT-11914
      "-Xconsistent-data-class-copy-visibility"
    )
  }
  @OptIn(ExperimentalAbiValidation::class) abiValidation()
}

detekt {
  source.setFrom("src/commonMain/kotlin")
  config.setFrom(rootProject.file("detekt.yml"))
  failOnSeverity = FailOnSeverity.Warning
  basePath.set(rootProject.rootDir)
}

dokka {
  dokkaSourceSets {
    configureEach {
      includes.from("MODULE.md")
      sourceLink {
        // Dokka appends the source path with a leading slash.
        val sourceRef = providers.gradleProperty("kotlinDsvSourceRef").get()
        remoteUrl("https://github.com/sargunv/kotlin-dsv/tree/$sourceRef")
        localDirectory = rootDir
      }
      externalDocumentationLinks {
        create("kotlinx-serialization") { url("https://kotlinlang.org/api/kotlinx.serialization/") }
        create("kotlinx-io") { url("https://kotlinlang.org/api/kotlinx-io/") }
      }
    }
  }
}

mavenPublishing {
  publishToMavenCentral(automaticRelease = true)
  if (providers.gradleProperty("signingInMemoryKey").isPresent) {
    signAllPublications()
  }

  pom {
    url = "https://github.com/sargunv/kotlin-dsv/"

    scm {
      url = "https://github.com/sargunv/kotlin-dsv"
      connection = "scm:git:git://github.com/sargunv/kotlin-dsv.git"
      developerConnection = "scm:git:git://github.com/sargunv/kotlin-dsv.git"
    }

    licenses {
      license {
        name = "Apache-2.0"
        url = "https://opensource.org/licenses/Apache-2.0"
        distribution = "repo"
      }
    }

    developers {
      developer {
        id = "sargunv"
        name = "Sargun Vohra"
      }
    }
  }
}
