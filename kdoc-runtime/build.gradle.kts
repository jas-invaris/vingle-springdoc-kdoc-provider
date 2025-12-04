plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    `maven-publish`
}

internal object Versions {
    const val KotlinxSerialization = "1.9.0"
    const val Junit = "6.0.1"
    const val SpringDoc = "2.8.14"
}

dependencies {
    // JSON for reading documentation data
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${Versions.KotlinxSerialization}")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")

    testImplementation(kotlin("test"))
    testImplementation("org.springdoc:springdoc-openapi-starter-common:${Versions.SpringDoc}")
    testImplementation(platform("org.junit:junit-bom:${Versions.Junit}"))
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}