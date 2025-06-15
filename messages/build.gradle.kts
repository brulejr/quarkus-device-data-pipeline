plugins {
    kotlin("jvm")
    kotlin("plugin.allopen")
    id("io.quarkus")
}

dependencies {
    api("io.quarkus:quarkus-kotlin")
    api("io.quarkus:quarkus-jackson")
}
