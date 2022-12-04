import cz.habarta.typescript.generator.JsonLibrary
import cz.habarta.typescript.generator.TypeScriptFileType

plugins {
    id("org.springframework.boot") version "2.4.3"
    id("io.spring.dependency-management") version "1.0.10.RELEASE"
    id("java")
    id("jacoco")
    id("com.github.spotbugs") version "4.7.3"
    id("cz.habarta.typescript-generator") version "3.0.1157"
//    id("cz.habarta.typescript-generator:typescript-generator-gradle-plugin") version "3.0.1157"
//    id("cz.habarta.typescript-generator:typescript-generator-spring") version "3.0.1157"
}

apply(plugin = "cz.habarta.typescript-generator")

//targetCompatibility = JavaVersion.VERSION_15
//sourceCompatibility = JavaVersion.VERSION_15

group = "moe.mewore.web"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

ext["log4j2.version"] = "2.19.0"
dependencies {
    annotationProcessor("org.projectlombok:lombok:1.18.24")
    compileOnly("org.projectlombok:lombok:1.18.24")
    compileOnly("org.checkerframework:checker-qual:3.25.0")
    implementation(project(":imagediary"))
    implementation("org.springframework.boot:spring-boot-starter-web:2.7.5")
    implementation("javax.validation:validation-api:2.0.1.Final")
    implementation("javax.annotation:javax.annotation-api:1.3.2")
    implementation("io.springfox:springfox-boot-starter:3.0.0")
    runtimeOnly("com.google.code.findbugs:jsr305:3.0.2")

    testAnnotationProcessor("org.projectlombok:lombok:1.18.24")
    testCompileOnly("org.projectlombok:lombok:1.18.24")
    testCompileOnly("org.checkerframework:checker-qual:3.25.0")
    testImplementation("org.springframework.boot:spring-boot-starter-test:2.7.5")
    testImplementation("org.mockito:mockito-core:3.11.2")
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

tasks.generateTypeScript {
    // The documentation for the Maven plugin (there may not be one for Gradle, but they"re essentially the same):
    // http://www.habarta.cz/typescript-generator/maven/typescript-generator-maven-plugin/generate-mojo.html

    classPatterns = listOf("io.github.mewore.web.controllers.**")
    classesWithAnnotations = listOf("io.github.mewore.web.models.MessageModel")
    excludeClasses = listOf("org.springframework.core.io.Resource")

    optionalAnnotations = listOf("org.springframework.lang.Nullable")
    nullableAnnotations = listOf("org.checkerframework.checker.nullness.qual.Nullable")

    customTypeMappings = listOf("java.nio.file.Path:string", "java.time.Instant:string", "java.time.Duration:undefined")

    // Make sure the file remains the same if the definitions are the same
    sortDeclarations = true
    sortTypeDeclarations = true
    noFileComment = true

    outputFileType = TypeScriptFileType.implementationFile
    jsonLibrary = JsonLibrary.jackson2
    outputKind = cz.habarta.typescript.generator.TypeScriptOutputKind.module
    generateSpringApplicationClient = true
    val fileExtension = if (outputFileType == TypeScriptFileType.implementationFile) ".ts" else ".d.ts"
    outputs.file(project.buildDir.toPath().resolve("typescript-generator").resolve(project.name + fileExtension))
}

tasks.bootJar {
    dependsOn.add(tasks.spotbugsMain)
    dependsOn.add(tasks.test)
    dependsOn.add(tasks.generateTypeScript)
}
