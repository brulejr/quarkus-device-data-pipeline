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

    api("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.0")

    testImplementation("io.quarkus:quarkus-junit5")
}
