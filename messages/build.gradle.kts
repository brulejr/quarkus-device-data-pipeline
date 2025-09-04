plugins {
    kotlin("jvm")
    kotlin("plugin.allopen")
    id("io.quarkus")
}

dependencies {
    api("io.quarkus:quarkus-rest-jackson")
    api("io.quarkus:quarkus-hibernate-validator")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.3")

    testImplementation(project(":common-test"))
}
