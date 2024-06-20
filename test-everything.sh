#!/bin/bash

tasks_to_run=()
for java_module in imagediary rabbit-generator; do
  tasks_to_run+=("${java_module}:spotbugsMain" "${java_module}:test")
done
for html_module in rootpage rabbitpage dialogue-page; do
  tasks_to_run+=("${html_module}:checkDisabledLintRules" "${html_module}:lint")
done
tasks_to_run+=("rootpage:package")

./gradlew --parallel "${tasks_to_run[@]}"
exit $?
