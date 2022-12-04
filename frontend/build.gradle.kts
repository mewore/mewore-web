plugins {
    id("java")
    id("com.github.node-gradle.node") version "3.0.0-rc5"
}

group = "moe.mewore.web"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

node {
    version.set("16.14.0")
    npmVersion.set("8.5.2")
    download.set(true)
}

val commonRootSourceFiles = listOf("build.gradle.kts", "package.json", "tsconfig.json")

val frontendLint = tasks.create<com.github.gradle.node.npm.task.NpmTask>("frontendLint") {
    setDependsOn(listOf(tasks.npmInstall))
    inputs.dir("src")
    inputs.files(commonRootSourceFiles)
    inputs.files(".eslintrc.json")
    outputs.upToDateWhen { true }
    args.set(listOf("run", "lint"))
    description = "Lints the frontend code."
}

tasks.create<SourceTask>("frontendCheckDisabledLintRules") {
    source(listOf("src", "test").map { projectDir.resolve(it) })
    include(listOf("js", "ts", "jsx", "tsx", "vue").map { "**/*.$it" })

    outputs.upToDateWhen { true }
    val rulesThatShouldNotBeDisabled = setOf("no-debugger", "no-console", "jest/no-focused-tests")
    description =
        "Ensures that the following ESLint rules have not been disabled: " + rulesThatShouldNotBeDisabled.joinToString(
            ", "
        )

    val disablePattern = Regex("eslint-disable(-next-line)?\\s+(\\S*)")

    doLast("Check for commonly disabled ESLint rules") {
        val disabledRuleLocations = mutableListOf<String>()

        source.forEach { file ->
            var line = 1
            file.forEachLine(Charsets.UTF_8) {
                val match = disablePattern.find(it)
                val rule = match?.groups?.get(2)?.value
                if (match != null && rulesThatShouldNotBeDisabled.contains(rule)) {
                    disabledRuleLocations.add("${file.absolutePath}:$line:${match.range.first + 1}")
                }
                line++
            }
        }
        if (disabledRuleLocations.isNotEmpty()) {
            error("Found possibly disabled rules at:\n${disabledRuleLocations.joinToString("\n") { "\t- $it\n\t\t(file://$it)" }}")
        }
    }
}

tasks.create<com.github.gradle.node.npm.task.NpmTask>("frontendBuild") {
    setDependsOn(listOf(tasks.npmInstall))
    inputs.dir("src")
    inputs.dir("public")
    inputs.files(commonRootSourceFiles)
    outputs.dir("dist")
    args.set(listOf("run", "build"))
    description = "Builds the the frontend after ensuring the NPM dependencies are present."
}

val frontendBuildProd = tasks.create<com.github.gradle.node.npm.task.NpmTask>("frontendBuildProd") {
    setDependsOn(listOf(tasks.npmInstall))
    inputs.dir("src")
    inputs.dir("public")
    inputs.files(commonRootSourceFiles)
    outputs.dir("dist")
    args.set(listOf("run", "build-prod"))
    description = "Builds the the frontend in PRODUCTION mode after ensuring the NPM dependencies are present."
}

tasks.create<com.github.gradle.node.npm.task.NpmTask>("frontendBuildWatch") {
    setDependsOn(listOf(tasks.npmInstall))
    args.set(listOf("run", "build-watch"))
    description = "Builds the the frontend after ensuring the NPM dependencies are present and watches for changes."
}

val frontendTest = tasks.create<com.github.gradle.node.npm.task.NpmTask>("frontendTest") {
    setDependsOn(listOf(tasks.npmInstall))
    inputs.dir("src")
//    inputs.dir("tests")
    inputs.files(commonRootSourceFiles)
    outputs.dir("tests/coverage")
    outputs.upToDateWhen { true }
    args.set(listOf("run", "test:unit:coverage"))
    description = "Runs the frontend unit tests."
}

tasks.jar {
    setDependsOn(listOf(frontendBuildProd))
    inputs.dir("dist")
    outputs.dir("build")
    from("dist")
    into("static")
}
