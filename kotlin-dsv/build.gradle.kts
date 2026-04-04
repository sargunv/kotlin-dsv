plugins { id("published-library") }

kotlin {
  sourceSets {
    commonMain.dependencies {
      implementation(libs.jetbrains.annotations)
      api(libs.kotlinx.serialization.core)
      api(libs.kotlinx.io.core)
    }
    commonTest.dependencies { implementation(libs.kotlinx.serialization.json) }

    create("fsTest").apply {
      dependsOn(commonTest.get())
      jvmTest.get().dependsOn(this)
      macosArm64Test.get().dependsOn(this)
      macosX64Test.get().dependsOn(this)
      linuxArm64Test.get().dependsOn(this)
      linuxX64Test.get().dependsOn(this)
      mingwX64Test.get().dependsOn(this)
    }
  }
}

mavenPublishing {
  pom {
    name = "Kotlin DSV"
    description = "Kotlin Multiplatform utilities for working with delimiter-separated values"
  }
}
