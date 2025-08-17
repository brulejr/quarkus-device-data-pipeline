plugins {
    kotlin("jvm")
    kotlin("plugin.allopen")
    id("io.quarkus")
}

dependencies {

    api("io.quarkus:quarkus-kotlin")
    api("io.quarkus:quarkus-rest") // core RESTEasy Reactive stack
    api("io.quarkus:quarkus-rest-jackson") // ✅ Jackson integration
    api("io.quarkus:quarkus-mongodb-panache-kotlin")

    api("com.fasterxml.jackson.module:jackson-module-kotlin")

    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("io.rest-assured:rest-assured")
    testImplementation("io.mockk:mockk:1.13.12")
    testImplementation("org.assertj:assertj-core:3.26.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
}
