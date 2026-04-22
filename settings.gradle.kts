pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        mavenCentral()
    }
}

rootProject.name = "payment-orchestrator"

include(
    "payment-bom",
    "payment-core",
    "payment-http-support",
    "payment-webhook-support",
    "payment-phone-support",
    "payment-provider-pawapay",
    "payment-provider-cinetpay",
    "payment-test-support",
    "payment-spring-boot-starter"
)