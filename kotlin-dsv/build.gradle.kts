plugins { id("published-library") }

kotlin {
  sourceSets {
    commonMain.dependencies {
      implementation(libs.jetbrains.annotations)
      api(libs.kotlinx.serialization.core)
      api(libs.kotlinx.io.core)
    }
    commonTest.dependencies { implementation(libs.kotlinx.serialization.json) }

    // fsTest source set for filesystem-based tests
    val fsTest by creating {
      dependsOn(commonTest.get())
    }

    // Configure fsTest for specific native targets that support filesystem access
    jvmTest { dependsOn(fsTest) }
    targets.filter { it.name.startsWith("macos") || it.name.startsWith("linux") || it.name.startsWith("mingw") }
      .forEach { target ->
        target.compilations.getByName("test").defaultSourceSet.dependsOn(fsTest)
      }
  }
}

mavenPublishing {
  pom {
    name = "Kotlin DSV"
    description = "Kotlin Multiplatform utilities for working with delimiter-separated values"
  }
}
