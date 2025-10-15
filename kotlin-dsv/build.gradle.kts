plugins { id("published-library") }

kotlin {
  sourceSets {
    commonMain.dependencies {
      implementation(libs.jetbrains.annotations)
      api(libs.kotlinx.serialization.core)
      api(libs.kotlinx.io.core)
    }
    commonTest.dependencies { implementation(libs.kotlinx.serialization.json) }
  }
}

mavenPublishing {
  pom {
    name = "Kotlin DSV"
    description = "Kotlin Multiplatform utilities for working with delimiter-separated values"
  }
}
