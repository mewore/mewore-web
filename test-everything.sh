#!/bin/bash

./gradlew --parallel rootpage:checkDisabledLintRules :rootpage:lint :rabbitpage:lint \
  imagediary:spotbugsMain imagediary:test :rootpage:package
exit $?
