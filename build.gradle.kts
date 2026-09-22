plugins {
    kotlin("jvm") version "2.3.21"
    kotlin("plugin.spring") version "2.3.21"
    kotlin("kapt") version "2.3.21"
    id("org.springframework.boot") version "4.1.0"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("plugin.jpa") version "2.3.21"
}

group = "org.kjs"
version = "0.0.1-SNAPSHOT"
description = "stockNews"

val querydslVersion = "5.1.0"
val bucket4jVersion = "8.14.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-batch")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.security:spring-security-crypto")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("tools.jackson.module:jackson-module-kotlin")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.1.0")
    implementation("com.querydsl:querydsl-jpa:$querydslVersion:jakarta")
    // IP 단위 요청 제한(토큰 버킷) - 버킷 보관은 Caffeine(TTL/최대 개수 제한)이 담당한다.
    implementation("com.bucket4j:bucket4j_jdk17-core:$bucket4jVersion")
    implementation("com.github.ben-manes.caffeine:caffeine")
    // Anthropic 공식 Java SDK - ClaudeChatClient(LlmClient 구현)가 쓴다. Kotlin은 Java SDK를 그대로 쓴다.
    implementation("com.anthropic:anthropic-java:2.34.0")
    kapt("com.querydsl:querydsl-apt:$querydslVersion:jakarta")
    kapt("jakarta.persistence:jakarta.persistence-api")
    kapt("jakarta.annotation:jakarta.annotation-api")
    runtimeOnly("org.mariadb.jdbc:mariadb-java-client")
    testImplementation("org.springframework.boot:spring-boot-starter-batch-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

kapt {
    arguments {
        arg("querydsl.entityAccessors", "true")
    }
}

sourceSets {
    main {
        java.srcDir("build/generated/source/kapt/main")
    }
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
    useJUnitPlatform()
    if (file("src/main/resources/application-local.yml").exists()) {
        systemProperty("spring.profiles.active", "local")
    }
}
