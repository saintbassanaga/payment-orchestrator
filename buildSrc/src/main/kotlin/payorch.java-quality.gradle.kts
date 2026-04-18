plugins {
    checkstyle
    id("com.github.spotbugs")
    jacoco
}

checkstyle {
    toolVersion = "10.17.0"
    configFile  = rootProject.file("config/checkstyle/checkstyle.xml")
    isIgnoreFailures = false
}

spotbugs {
    toolVersion.set("4.8.5")
    excludeFilter.set(rootProject.file("config/spotbugs/exclude.xml"))
    ignoreFailures.set(false)
}

tasks.withType<com.github.spotbugs.snom.SpotBugsTask>().configureEach {
    reports.create("html") { enabled = true }
    reports.create("xml")  { enabled = false }
}

jacoco {
    toolVersion = "0.8.12"
}

tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.named("test"))
    reports {
        xml.required = true
        html.required = true
    }
}