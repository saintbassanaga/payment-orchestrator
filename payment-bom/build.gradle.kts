plugins {
    `java-platform`
    `maven-publish`
    signing
}

description = "PayOrch Bill of Materials"

javaPlatform {
    allowDependencies()
}

dependencies {
    api(platform(libs.junit.bom))

    constraints {
        api("io.payorch:payment-core:${project.version}")
        api("io.payorch:payment-http-support:${project.version}")
        api("io.payorch:payment-webhook-support:${project.version}")
        api("io.payorch:payment-provider-pawapay:${project.version}")
        api("io.payorch:payment-provider-cinetpay:${project.version}")
        api("io.payorch:payment-provider-monetbill:${project.version}")
        api("io.payorch:payment-test-support:${project.version}")
        api("io.payorch:payment-spring-boot-starter:${project.version}")
        api(libs.okhttp)
        api(libs.okhttp.logging)
        api(libs.jackson.databind)
        api(libs.jackson.datatype.jsr310)
        api(libs.autoservice)
        api(libs.autoservice.annotations)
        api(libs.mockito.core)
        api(libs.mockito.junit.jupiter)
        api(libs.assertj.core)
        api(libs.wiremock)
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["javaPlatform"])
            pom {
                name        = "PayOrch BOM"
                description = project.description ?: project.name
                url         = "https://github.com/payorch/payorch"
                licenses {
                    license {
                        name = "Apache License, Version 2.0"
                        url  = "https://www.apache.org/licenses/LICENSE-2.0"
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
    isRequired = !version.toString().endsWith("-SNAPSHOT")
    sign(publishing.publications["maven"])
}