/*
 * This file was generated by the Gradle 'init' task.
 *
 * The settings file is used to specify which projects to include in your build.
 *
 * Detailed information about configuring a multi-project build in Gradle can be found
 * in the user manual at https://docs.gradle.org/7.2/userguide/multi_project_builds.html
 */

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
//    resolutionStrategy {
//        eachPlugin {
//            if (requested.id.id == "cz.habarta.typescript-generator") {
//                useModule("cz.habarta.typescript-generator:typescript-generator-gradle-plugin:${requested.version ?: "+"}")
//            }
//        }
//    }
}

rootProject.name = "mewore-web"
include("imagediary")
include("rootpage")
include("rabbit-generator")
include("rabbitpage")
