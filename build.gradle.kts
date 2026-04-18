plugins {
    alias(libs.plugins.nexus.publish)
}

allprojects {
    group   = "io.payorch"
    version = "0.1.0-SNAPSHOT"
}

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
            username = System.getenv("SONATYPE_USERNAME") ?: ""
            password = System.getenv("SONATYPE_PASSWORD") ?: ""
        }
    }
}