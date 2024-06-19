plugins {
    id("com.github.node-gradle.node") version "3.0.0-rc5"
}

group = "moe.mewore.web"
version = "0.0.1-SNAPSHOT"

node {
    version.set("16.14.0")
    npmVersion.set("8.5.2")
    download.set(true)
}

val commonRootSourceFiles = listOf("build.gradle.kts", "package.json", "tsconfig.json")

val lint = tasks.create<com.github.gradle.node.npm.task.NpmTask>("lint") {
    setDependsOn(listOf(tasks.npmInstall))
    inputs.dir("src")
    inputs.files(commonRootSourceFiles)
    inputs.files(".eslintrc.json")
    outputs.upToDateWhen { true }
    args.set(listOf("run", "lint"))
    description = "Lints the frontend code."
}

tasks.create<SourceTask>("checkDisabledLintRules") {
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

val buildProd = tasks.create<com.github.gradle.node.npm.task.NpmTask>("buildProd") {
    val templateFolderInputTree: ConfigurableFileTree = fileTree("template")
    templateFolderInputTree.exclude("js")
    setDependsOn(listOf(tasks.npmInstall))
    inputs.dir("src")
    inputs.dir(templateFolderInputTree)
    inputs.files(commonRootSourceFiles)
    outputs.dir("template/js")
    args.set(listOf("run", "build-prod"))
    description = "Builds the the frontend in PRODUCTION mode after ensuring the NPM dependencies are present."
}

tasks.create("build") {
    setDependsOn(listOf(buildProd))
}

tasks.create("buildAll") {
    setDependsOn(listOf(buildProd))
}

tasks.create<com.github.gradle.node.npm.task.NpmTask>("buildWatch") {
    setDependsOn(listOf(tasks.npmInstall))
    args.set(listOf("run", "build-watch"))
    description = "Builds the the frontend after ensuring the NPM dependencies are present and watches for changes."
}
