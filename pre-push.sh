#!/bin/bash

REMOTE=$1
#REPO_URL=$2

printError() {
    message=$1
    echo "========================================="
    echo "==> ERROR: ${message}"
    echo "========================================="
}

if [ -n "$(git diff --name-only)" ]; then
    printError "There are uncommitted changes! Commit or stash everything before trying to push."
    exit 1
fi

files_to_push=$(git diff --name-only "${REMOTE}")

if echo "${files_to_push}" | grep -q '.gradle'; then
    echo "Performing 'clean'..."
    ./gradlew backend:clean || exit 2
    ./gradlew clean || exit 2
else
    echo "There are no changed Gradle project files so the 'clean' task will not be performed."
fi

./test-everything.sh
exit $?
