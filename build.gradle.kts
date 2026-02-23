plugins {
    kotlin("jvm") version "2.1.20"
    id("org.springframework.boot") version "3.5.11"
    id("io.spring.dependency-management") version "1.1.6"
    kotlin("plugin.spring") version "2.1.20"
}

group = "org.willwolfram18"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencyManagement {
    imports {
        mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
    }
}

dependencies {
    val fhirVersion = "8.8.0"

    // HAPI FHIR
    api("ca.uhn.hapi.fhir:hapi-fhir-client:$fhirVersion")
    api("ca.uhn.hapi.fhir:hapi-fhir-structures-r4:$fhirVersion")

    // Spring Boot
    api("org.springframework.boot:spring-boot-starter")
    api("org.springframework.boot:spring-boot-starter-actuator")


    // Micrometer for observability
    api("io.micrometer:micrometer-tracing")

    api("org.aspectj:aspectjweaver")

    // Temporary for debugging
    api("org.springframework.boot:spring-boot-starter-web")
    api("io.micrometer:micrometer-registry-prometheus")

    // Testing
    testImplementation(kotlin("test"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    // Items for enabling tracing export during test https://docs.spring.io/spring-boot/3.5/reference/actuator/tracing.html#actuator.micrometer-tracing.tracer-implementations.otel-zipkin
    implementation("io.micrometer:micrometer-tracing-bridge-otel")
    implementation("io.opentelemetry:opentelemetry-exporter-zipkin")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
//    testImplementation("io.micrometer:micrometer-tracing-bridge-otel")
//    testImplementation("io.opentelemetry:opentelemetry-exporter-zipkin")
//    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}