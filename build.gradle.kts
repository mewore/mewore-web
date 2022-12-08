plugins {
    id("base")
    id("java")
    id("com.dorongold.task-tree").version("2.1.0")
}

group = "moe.mewore.web"
version = "0.0.1-SNAPSHOT"

repositories {
    maven { setUrl("https://jitpack.io") }
}

configurations.create("toCopy")
dependencies {
    configurations.getByName("toCopy")("com.github.mewore.e2e:e2e:0.1.1")
}

val e2eDir = file("tools")
val e2eName = "e2e.jar"
val e2eFile = e2eDir.resolve(e2eName)
val downloadE2eTask = tasks.create<Copy>("downloadE2e") {
    from(files(configurations.getByName("toCopy")))
    into(e2eDir)
    rename { e2eName }
    outputs.files(e2eFile)
}

tasks.jar {
    val backendJarTask = tasks.getByPath("backend:bootJar")
    val jarTasksToMerge = listOf(backendJarTask, tasks.getByPath("frontend:jar"))
    setDependsOn(setOf(jarTasksToMerge))
    inputs.files(jarTasksToMerge)

    entryCompression = ZipEntryCompression.STORED

    doFirst("Read .jar files") {
        val zipTrees = mutableListOf<FileTree>()
        for (taskToMerge in jarTasksToMerge) {
            zipTrees.addAll(taskToMerge.outputs.files.files.filter { file -> file.name.endsWith(".jar") }
                .map { jarFile -> zipTree(jarFile) })
        }
        from(zipTrees)

        val manifestFile = backendJarTask.outputs.files.files.filter { file -> file.name.endsWith(".jar") }
            .flatMap { archive -> zipTree(archive).files.filter { jarFile -> jarFile.name == "MANIFEST.MF" } }.single()
        println("Using the manifest file from the .jar output of task ${backendJarTask.path}: $manifestFile")
        manifest = manifest.from(manifestFile)
    }
}

tasks.create<JavaExec>("e2eRun") {
    dependsOn.add(tasks.jar)
    dependsOn.add(downloadE2eTask)
    classpath = files(e2eFile)

    environment["E2E: /"] = "hi im mewore"
    environment["E2E: /rabbits"] = "bnuy"

    if ((System.getenv("LOG_FILE") ?: "").length + (System.getenv("PORT") ?: "").length == 0) {
        doFirst("Set the E2E test .jar arguments") {
            // For some reason running this in the configuration phase makes the :backend:spotbugsMain configuration fail
            args = listOf(files(tasks.jar).asPath, "--spring.profiles.active=common,e2e")
        }
    }
}

/**
 * Displays the execution times of the executed tasks, sorted by their start and with a "time window" view of their
 * execution periods.
 *
 * Based on the answers at https://stackoverflow.com/questions/13031538/track-execution-time-per-task-in-gradle-script
 */
class TaskExecutionTimePreview : TaskExecutionListener, BuildListener {
    private var startTime: Long = -1
    private val taskStarts: MutableMap<String, Long> = mutableMapOf()
    private val taskEnds: MutableMap<String, Long> = mutableMapOf()
    private val taskStates: MutableMap<String, TaskState> = mutableMapOf()

    private fun getTime(): Long {
        return TimeUnit.MILLISECONDS.convert(System.nanoTime() - startTime, TimeUnit.NANOSECONDS)
    }

    override fun beforeExecute(task: Task) {
        taskStarts[task.path] = getTime()
    }

    override fun afterExecute(task: Task, taskState: TaskState) {
        if (!taskState.executed || taskState.skipped || taskState.upToDate) {
            taskStarts.remove(task.path)
        } else {
            taskEnds[task.path] = getTime()
            taskStates[task.path] = taskState
        }
    }

    override fun buildFinished(result: BuildResult) {
        println("Task time:")
        val minStart = taskStarts.values.minOrNull()
        val maxEnd = taskEnds.values.maxOrNull()
        if (minStart == null || maxEnd == null) {
            return
        }

        val length = kotlin.math.max(maxEnd - minStart, 1)
        val resolution = kotlin.math.max(20, kotlin.math.min(20 + (taskStarts.entries.size / 5) * 5, 80))
        val shades = listOf(" ", "░", "▒", "▓", "█")

        val lines = mutableListOf<String>()
        for (entry in taskStarts.entries.sortedBy { it.value }) {
            val task = entry.key
            val taskState = taskStates[task]
            val start = entry.value
            val end = taskEnds[task]
            if (end == null || end < start || taskState == null) {
                println("(WARNING: Task $task has an incorrect end time or state!)")
                continue
            }
            val startNormalized = (start.toDouble() - minStart) / length
            val endNormalized = (end.toDouble() - minStart) / length
            val characters = (0 until resolution).map { i ->
                val iNormalizedFrom = i.toDouble() / resolution
                val iNormalizedTo = (i + 1).toDouble() / resolution
                val coverage = (kotlin.math.min(iNormalizedTo, endNormalized) - kotlin.math.max(
                    iNormalizedFrom, startNormalized
                )) * resolution
                shades[if (coverage > 0) kotlin.math.min((coverage * shades.size).toInt(), shades.size - 1) else 0]
            }.toMutableList()
            if (characters.count { it == " " } == characters.size) {
                characters[kotlin.math.max(
                    0, kotlin.math.min(resolution - 1, ((startNormalized + endNormalized) / 2 * resolution).toInt())
                )] = shades[1]
            }

            var formattedTime = "${kotlin.math.round((end - start) / 100.0) / 10.0}"
            if (!formattedTime.contains(".")) {
                formattedTime += ".0"
            }
            val prefix = (if (end - start <= 50) "" else "$formattedTime s").padStart(12)
            val taskSign = if (taskState.failure == null) " " else "x"
            val line = "$prefix |${characters.joinToString("")}| $taskSign $task"
            lines.add(if (taskState.failure == null) line else "<span style=\"color: indianred\">$line</span>")
            println(line)
        }
        val reportDir = result.gradle?.rootProject?.buildDir?.resolve("reports/task-durations")
        if (reportDir != null && (reportDir.exists() || reportDir.mkdirs())) {
            val file = reportDir.resolve("index.html")
            file.writeText("<pre>" + lines.joinToString("\n") + "\n</pre>\n")
        }
    }

    override fun projectsEvaluated(gradle: Gradle) {
    }

    override fun projectsLoaded(gradle: Gradle) {
    }

    override fun settingsEvaluated(settings: Settings) {
    }
}

gradle.addListener(TaskExecutionTimePreview())
