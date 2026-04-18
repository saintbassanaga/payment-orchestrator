plugins {
    `maven-publish`
    signing
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            pom {
                name = project.name
                description = project.description ?: project.name
                url = "https://github.com/payorch/payorch"
                licenses {
                    license {
                        name = "Apache License, Version 2.0"
                        url  = "https://www.apache.org/licenses/LICENSE-2.0"
                    }
                }
                developers {
                    developer {
                        id    = "payorch"
                        name  = "PayOrch Contributors"
                        email = "contact@payorch.io"
                    }
                }
                scm {
                    connection          = "scm:git:git://github.com/payorch/payorch.git"
                    developerConnection = "scm:git:ssh://github.com/payorch/payorch.git"
                    url                 = "https://github.com/payorch/payorch"
                }
            }
        }
    }
}

signing {
    val gpgKey        = System.getenv("GPG_PRIVATE_KEY")
    val gpgPassphrase = System.getenv("GPG_PASSPHRASE")
    if (gpgKey != null && gpgPassphrase != null) {
        useInMemoryPgpKeys(gpgKey, gpgPassphrase)
    }
    isRequired = !version.toString().endsWith("-SNAPSHOT")
    sign(publishing.publications["maven"])
}