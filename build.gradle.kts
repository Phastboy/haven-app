buildscript {
  dependencies {
    // Keep AGP's built-in Kotlin compiler aligned with the Compose compiler plugin.
    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.21")
  }
}

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.google.devtools.ksp) apply false
}
