plugins {
    id("payorch.java-library")
    id("payorch.java-quality")
    id("payorch.java-publish")
}

description = "PayOrch test support — mock providers and base SPI test classes"

dependencies {
    api(project(":payment-core"))
    api(platform(libs.junit.bom))
    api(libs.junit.jupiter)
    api(libs.mockito.core)
    api(libs.mockito.junit.jupiter)
    api(libs.assertj.core)
}