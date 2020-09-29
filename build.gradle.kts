apply(plugin = "maven")

plugins {
    kotlin("jvm") version "1.4.0"
}

group = "com.github.theforbiddenai"
version = "1.4.8"

repositories {
    jcenter()
}

dependencies {
    implementation("org.jsoup:jsoup:1.13.1")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.0")
    testImplementation("io.mockk:mockk:1.10.0")
    testImplementation("org.assertj:assertj-core:3.17.2")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.0")

}

tasks {
    test {
        useJUnitPlatform()
    }
}