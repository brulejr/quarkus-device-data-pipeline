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
    implementation("io.quarkus:quarkus-hibernate-validator")
    implementation("io.quarkus:quarkus-messaging-mqtt")
    implementation("io.quarkus:quarkus-messaging-rabbitmq")
    implementation("io.quarkus:quarkus-scheduler")

    // DL4J (CPU)
    implementation("org.deeplearning4j:deeplearning4j-core:1.0.0-M2.1")
    implementation("org.nd4j:nd4j-native-platform:1.0.0-M2.1")

    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("io.rest-assured:rest-assured")
}

jib {
    from {
        image = "eclipse-temurin:21-jdk-alpine"
    }
    to {
        image = "brulejr/recommendation-ms"
        tags = setOf("latest", project.version.toString())
    }
    container {
        creationTime = "USE_CURRENT_TIMESTAMP"
        ports = listOf("3043")
        workingDirectory = "/app"
        environment = mapOf(
            "SPRING_PROFILES_ACTIVE" to "prod",
            "JAVA_OPTS" to "-Xms512m -Xmx1024m"
        )
        entrypoint = listOf("java", "-jar", "/app/quarkus-run.jar")
    }
    extraDirectories {
        paths {
            path {
                setFrom(file("build/quarkus-app").toPath())
                into = "/app"
            }
        }
    }
}
