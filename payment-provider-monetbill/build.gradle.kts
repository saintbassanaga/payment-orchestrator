plugins {
    id("payorch.java-library")
    id("payorch.java-quality")
    id("payorch.java-publish")
}

description = "PayOrch adapter — MonetBill provider"

dependencies {
    api(project(":payment-core"))
    implementation(project(":payment-http-support"))
    implementation(libs.jackson.databind)
    implementation(libs.jackson.datatype.jsr310)
    compileOnly(libs.autoservice.annotations)
    annotationProcessor(libs.autoservice)
    testImplementation(project(":payment-test-support"))
    testImplementation(libs.wiremock)
}