#!/bin/bash

./gradlew --parallel rootpage:checkDisabledLintRules backend:bootJar :rootpage:lint :rabbitpage:lint \
  backend:spotbugsMain backend:test imagediary:spotbugsMain imagediary:test :rootpage:package
exit $?
