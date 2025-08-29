plugins {
    kotlin("jvm")
    kotlin("plugin.allopen")
}

dependencies {
    api("io.quarkus:quarkus-junit5")
    api("io.rest-assured:rest-assured")
    api("io.mockk:mockk:1.13.12")
    api("org.assertj:assertj-core:3.26.0")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-test")
}
