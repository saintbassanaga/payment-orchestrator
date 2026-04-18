plugins {
    id("payorch.java-library")
    id("payorch.java-quality")
    id("payorch.java-publish")
}

description = "PayOrch webhook support — signature verification and routing"

dependencies {
    api(project(":payment-core"))
}