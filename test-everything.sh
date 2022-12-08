#!/bin/bash

./gradlew --parallel frontend:frontendCheckDisabledLintRules backend:bootJar jar frontend:frontendTest \
  backend:spotbugsMain backend:test imagediary:spotbugsMain imagediary:test e2eRun
exit $?
