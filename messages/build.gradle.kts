plugins {
    kotlin("jvm")
    kotlin("plugin.allopen")
    id("io.quarkus")
}

dependencies {
    implementation(project(":common-core"))
}
