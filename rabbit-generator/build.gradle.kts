plugins {
    id("java")
    id("jacoco")
    id("com.github.spotbugs") version "4.7.3"
}
plugins.apply("java")

java {
    sourceCompatibility = JavaVersion.VERSION_15
    targetCompatibility = JavaVersion.VERSION_15
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
    implementation(project(":imagediary"))
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
}

// TODO: Use an environment variable for generateRabbit to determine its rabbit diary location instead of a program arg
// TODO: Hardcode the location to the template directory; the target directory can be somewhere in 'build/generated/html' here
// TODO: FIND OUT WHY ONLY A DOZEN MONTHS ARE FOUND: 2023-09, 2023-05, 2023-04, 2023-03, 2023-02, 2022-12, 2022-11, 2022-10, 2022-09, 2022-08, 2022-07
tasks.create<JavaExec>("generateRabbit") {
    setDependsOn(listOf(":rabbitpage:buildAll"))
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("moe.mewore.web.rabbit.generator.RabbitGenerator")

    val diaryEnvVarName = "RABBIT_DIARY_DIR"
    val rabbitDiaryDir = environment[diaryEnvVarName]
    val rabbitPageProject = project(":rabbitpage")
    val templateDir = rabbitPageProject.projectDir.resolve("template").path
    println("Template dir: $templateDir")
    val targetDir = buildDir.resolve("generated/html").path
    println("Destination dir: $targetDir")

    inputs.dir(templateDir)
    outputs.dir(targetDir)
    if (rabbitDiaryDir != null) {
        inputs.dir(rabbitDiaryDir)
        args = listOf(rabbitDiaryDir.toString(), templateDir, targetDir)
    }
}

