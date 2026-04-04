import com.palantir.gradle.gitversion.VersionDetails

plugins { id("com.palantir.git-version") }

val versionDetailsFn: groovy.lang.Closure<VersionDetails> by extra
val versionDetails = versionDetailsFn()

val semverStage = project.findProperty("semver.stage") as String?
val semverScope = project.findProperty("semver.scope") as String?

val lastTag = (versionDetails.lastTag ?: "v0.0.0").removePrefix("v")
val parts = lastTag.split(".").map { it.toIntOrNull() ?: 0 }

val (major, minor, patch) =
  Triple(parts.getOrElse(0) { 0 }, parts.getOrElse(1) { 0 }, parts.getOrElse(2) { 0 })

version =
  if (versionDetails.isCleanTag) {
    lastTag
  } else {
    when (semverStage) {
      "final" ->
        when (semverScope) {
          "major" -> "${major + 1}.0.0"
          "minor" -> "$major.${minor + 1}.0"
          else -> "$major.$minor.${patch + 1}"
        }
      "snapshot" ->
        when (semverScope) {
          "major" -> "${major + 1}.0.0-SNAPSHOT"
          "minor" -> "$major.${minor + 1}.0-SNAPSHOT"
          else -> "$major.$minor.${patch + 1}-SNAPSHOT"
        }
      else -> {
        val dirty =
          if (!versionDetails.isCleanTag && versionDetails.commitDistance == 0) "+DIRTY" else ""
        "$lastTag.${versionDetails.commitDistance}+${versionDetails.gitHash?.take(7) ?: "unknown"}$dirty"
      }
    }
  }
