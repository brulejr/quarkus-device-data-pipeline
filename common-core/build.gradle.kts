plugins {
    kotlin("jvm")
    kotlin("plugin.allopen")
    id("io.quarkus")
}

dependencies {
    api("io.quarkus:quarkus-kotlin")
    api("io.quarkus:quarkus-jackson")
    api("io.quarkus:quarkus-rest")
    api("io.quarkus:quarkus-mongodb-panache-kotlin")

    api("com.fasterxml.jackson.module:jackson-module-kotlin")

    testImplementation("io.quarkus:quarkus-junit5")
}
