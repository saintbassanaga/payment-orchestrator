plugins {
    alias(libs.plugins.nexus.publish) apply false
}

allprojects {
    group   = "io.payorch"
    version = "0.1.0-SNAPSHOT"
}