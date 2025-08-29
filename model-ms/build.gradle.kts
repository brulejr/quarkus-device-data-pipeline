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
    implementation("io.quarkus:quarkus-messaging-rabbitmq")
    implementation("io.quarkus:quarkus-rest")
    implementation("io.quarkus:quarkus-rest-jackson")
    implementation("io.quarkus:quarkus-hibernate-validator")

    testImplementation(project(":common-test"))
}

jib {
    from {
        image = "eclipse-temurin:21-jdk-alpine"
    }
    to {
        image = "brulejr/model-ms"
        tags = setOf("latest", project.version.toString())
    }
    container {
        creationTime = "USE_CURRENT_TIMESTAMP"
        ports = listOf("3042")
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
