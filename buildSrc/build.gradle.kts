plugins { `kotlin-dsl` }

repositories {
  mavenCentral()
  gradlePluginPortal()
}

dependencies {
  implementation(libs.gradle.kotlin)
  implementation(libs.gradle.kotlin.serialization)
  implementation(libs.gradle.dokka)
  implementation(libs.gradle.publish)
  implementation(libs.gradle.benchmark)
  implementation(libs.gradle.kover)
  implementation(libs.gradle.palantir.git.version)
  implementation(libs.gradle.mkdocs.build)
  implementation(libs.gradle.detekt)
}

kotlin { jvmToolchain(21) }
