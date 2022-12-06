plugins {
    id("java")
    application
}

group = "moe.mewore.web"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    annotationProcessor("org.projectlombok:lombok:1.18.24")
    compileOnly("org.projectlombok:lombok:1.18.24")
    compileOnly("org.checkerframework:checker-qual:3.25.0")

    implementation("org.apache.logging.log4j:log4j-api:2.17.2")
    implementation("org.apache.logging.log4j:log4j-core:2.17.2")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.17.2")
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "moe.mewore.e2e.SpringApplicationVerifier"
    }

    val sourcesMain = sourceSets.main.get()
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) } + sourcesMain.output)
    duplicatesStrategy = DuplicatesStrategy.WARN
}

application {
    mainClass.set("moe.mewore.e2e.SpringApplicationVerifier")
}
