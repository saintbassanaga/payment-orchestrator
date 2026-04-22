plugins {
    id("payorch.java-library")
    id("payorch.java-quality")
    id("payorch.java-publish")
}

description = "PayOrch Phone Support — phone number parsing and mobile operator detection"

dependencies {
    api(project(":payment-core"))
    implementation(libs.libphonenumber)
    implementation(libs.libphonenumber.carrier)
}
