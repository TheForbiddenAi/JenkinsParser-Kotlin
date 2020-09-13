apply(plugin = "maven")

plugins {
    kotlin("jvm") version "1.4.0"
}

group = "com.github.theforbiddenai"
version = "1.4.5"

repositories {
    jcenter()
}

dependencies {
    implementation("org.jsoup:jsoup:1.13.1")
}

