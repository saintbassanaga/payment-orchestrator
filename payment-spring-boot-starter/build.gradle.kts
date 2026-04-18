plugins {
    id("payorch.java-library")
    id("payorch.java-quality")
    id("payorch.java-publish")
}

description = "PayOrch Spring Boot starter — auto-configuration"

dependencies {
    api(project(":payment-core"))
    implementation(libs.spring.boot.autoconfigure)
    compileOnly(libs.spring.boot.starter)
    testImplementation(libs.spring.boot.starter.test)
}