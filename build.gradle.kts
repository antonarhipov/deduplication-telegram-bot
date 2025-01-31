plugins {
    kotlin("jvm") version "2.1.10"
    application
}

group = "org.arhan"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

configurations.all {
    resolutionStrategy {
        force("org.slf4j:slf4j-api:1.7.36")
    }
}

dependencies {
    implementation("org.telegram:telegrambots:6.9.7.1") {
        exclude(group = "org.slf4j", module = "slf4j-api")
    }
    implementation("org.telegram:telegrambots-meta:6.9.7.1") {
        exclude(group = "org.slf4j", module = "slf4j-api")
    }
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
    implementation("org.slf4j:slf4j-api:1.7.36")
    implementation("ch.qos.logback:logback-classic:1.2.11")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
    testImplementation("io.mockk:mockk:1.13.5")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("org.arhan.bot.MainKt")
}
