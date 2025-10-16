plugins { id("published-library") }

kotlin {
  sourceSets {
    commonMain.dependencies {
      implementation(libs.jetbrains.annotations)
      api(libs.kotlinx.serialization.core)
      api(libs.kotlinx.io.core)
    }
    commonTest.dependencies { implementation(libs.kotlinx.serialization.json) }

    val fsTest by creating { dependsOn(commonTest.get()) }

    jvmTest { dependsOn(fsTest) }
    macosX64Test { dependsOn(fsTest) }
    macosArm64Test { dependsOn(fsTest) }
    linuxX64Test { dependsOn(fsTest) }
    linuxArm64Test { dependsOn(fsTest) }
    mingwX64Test { dependsOn(fsTest) }
  }
}

mavenPublishing {
  pom {
    name = "Kotlin DSV"
    description = "Kotlin Multiplatform utilities for working with delimiter-separated values"
  }
}
