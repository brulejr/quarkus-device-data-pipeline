plugins {
    kotlin("jvm")
    kotlin("plugin.allopen")
    id("io.quarkus")
    id("com.google.cloud.tools.jib")
}

dependencies {
    implementation(project(":common-core"))
    implementation(project(":messages"))

    implementation("io.quarkus:quarkus-arc")
    implementation("io.quarkus:quarkus-config-yaml")
    implementation("io.quarkus:quarkus-messaging-mqtt")
    implementation("io.quarkus:quarkus-messaging-rabbitmq")

    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("io.rest-assured:rest-assured")
}

jib {
    from {
        image = "eclipse-temurin:21-jdk-alpine"
    }
    container {
        creationTime = "USE_CURRENT_TIMESTAMP"
        entrypoint = listOf("/usr/local/bin/entrypoint.sh")
        ports = listOf("3041")
        workingDirectory = "/app"
        environment = mapOf(
            "SPRING_PROFILES_ACTIVE" to "prod",
            "JAVA_OPTS" to "-Xms512m -Xmx1024m"
        )

    }
    to {
        image = "brulejr/ingester-ms"
        tags = setOf("latest")
    }
}