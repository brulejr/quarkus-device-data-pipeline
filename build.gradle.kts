plugins {
    id("io.quarkus") version "3.23.3" apply false
    kotlin("jvm") version "2.1.21" apply false
    kotlin("plugin.allopen") version "2.1.21" apply false
    id("com.google.cloud.tools.jib") version "3.4.1" apply false
}

allprojects {
    group = "io.jrb.labs"
    version = "0.1.0"
    repositories {
        mavenCentral()
    }
}

val quarkusPlatformVersion: String by project

subprojects {

    apply(plugin = "org.jetbrains.kotlin.jvm")

    dependencies {
        "implementation"(enforcedPlatform("io.quarkus:quarkus-bom:$quarkusPlatformVersion"))
        "implementation"("io.quarkus:quarkus-kotlin")
    }

}
