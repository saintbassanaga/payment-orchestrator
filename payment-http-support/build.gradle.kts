plugins {
    id("payorch.java-library")
    id("payorch.java-quality")
    id("payorch.java-publish")
}

description = "PayOrch HTTP support — OkHttp client and interceptors"

dependencies {
    api(project(":payment-core"))
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    testImplementation(libs.okhttp.mockwebserver)
}