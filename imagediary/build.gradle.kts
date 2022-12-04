plugins {
    id("java")
    id("jacoco")
    id("com.github.spotbugs") version "4.7.3"
}

//targetCompatibility = JavaVersion.VERSION_15
//sourceCompatibility = JavaVersion.VERSION_15

group = "moe.mewore.web"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    annotationProcessor("org.projectlombok:lombok:1.18.24")
    compileOnly("org.projectlombok:lombok:1.18.24")
    compileOnly("org.checkerframework:checker-qual:3.25.0")
    implementation("javax.validation:validation-api:2.0.1.Final")
    implementation("javax.annotation:javax.annotation-api:1.3.2")
    runtimeOnly("com.google.code.findbugs:jsr305:3.0.2")

    testAnnotationProcessor("org.projectlombok:lombok:1.18.24")
    testCompileOnly("org.projectlombok:lombok:1.18.24")
    testCompileOnly("org.checkerframework:checker-qual:3.25.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.0")
    testImplementation("org.mockito:mockito-core:3.11.2")
    testImplementation("org.mockito:mockito-junit-jupiter:3.6.28")
    testRuntimeOnly("com.google.code.findbugs:jsr305:3.0.2")
}

tasks.spotbugsMain {
    reports.create("html")
    excludeFilter.fileValue(projectDir.toPath().resolve("spotbugs-exclude.xml").toFile())
}

tasks.test {
    useJUnitPlatform()
    finalizedBy("jacocoTestReport")
}

jacoco {
    toolVersion = "0.8.6"
}

tasks.jacocoTestReport {
    dependsOn.add(tasks.test)
    reports.xml.required.set(true)

//    afterEvaluate {
//        classDirectories.setFrom(files(classDirectories.files.filter {
//            it.name != "Application.class"
//                    && !it.name.endsWith("Constants.class")
//                    && !it.name.endsWith("Entity.class")
//                    && !it.path.contains("/services/util/")
//        }))
//    }
}
