plugins {
    `maven-publish`
    signing
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url  = uri("https://maven.pkg.github.com/saintbassanaga/payorch")
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: ""
                password = System.getenv("GITHUB_TOKEN") ?: ""
            }
        }
    }
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            pom {
                name = project.name
                description = project.description ?: project.name
                url = "https://github.com/saintbassanaga/payorch"
                licenses {
                    license {
                        name = "Apache License, Version 2.0"
                        url  = "https://www.apache.org/licenses/LICENSE-2.0"
                    }
                }
                developers {
                    developer {
                        id    = "saintbassanaga"
                        name  = "Saint Bassanaga"
                        email = "saintbassanaga01@gmail.com"
                    }
                }
                scm {
                    connection          = "scm:git:git://github.com/saintbassanaga/payorch.git"
                    developerConnection = "scm:git:ssh://github.com/saintbassanaga/payorch.git"
                    url                 = "https://github.com/saintbassanaga/payorch"
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