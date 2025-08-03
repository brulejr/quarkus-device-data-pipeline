plugins {
    kotlin("jvm")
    kotlin("plugin.allopen")
    id("io.quarkus")
}

dependencies {
    implementation("io.quarkus:quarkus-rest-jackson")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.3")
}
