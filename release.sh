#!/usr/bin/env bash

set -euo pipefail

if [ $# -ne 2 ]; then
    echo "Usage: $0 <api|verifier> <version>"
    exit 1
fi

MODULE=$1
VERSION=$2

if [ "$MODULE" != "api" ] && [ "$MODULE" != "verifier" ]; then
    echo "Invalid module: $MODULE"
    echo "Usage: $0 <api|verifier> <version>"
    exit 1
fi

if ! [[ "$VERSION" =~ ^[0-9]+(\.[0-9]+){1,2}(-[A-Za-z0-9.-]+)?$ ]]; then
    echo "Invalid version: $VERSION"
    echo "Expected format: 1.2.3"
    exit 1
fi

MODULE_DIR="liquidjava-$MODULE"
POM="$MODULE_DIR/pom.xml"

if [ "$MODULE" = "api" ]; then
    TAG="api-v$VERSION"
else
    TAG="v$VERSION"
fi

CURRENT_BRANCH=$(git branch --show-current)
if [ "$CURRENT_BRANCH" != "main" ]; then
    echo "Release must be run from main. Current branch: $CURRENT_BRANCH"
    exit 1
fi

if [ -n "$(git status --porcelain)" ]; then
    echo "Worktree must be clean before releasing."
    git status --short
    exit 1
fi

if git rev-parse "$TAG" >/dev/null 2>&1; then
    echo "Tag already exists: $TAG"
    exit 1
fi

perl -0pi -e "s#(<artifactId>$MODULE_DIR</artifactId>\\s*<version>)[^<]+(</version>)#\${1}$VERSION\${2}#" "$POM"

if git diff --quiet -- "$POM"; then
    echo "$POM is already at version $VERSION"
    exit 1
fi

./mvnw -f "$POM" -B --fail-fast -Dgpg.skip=true -Dmaven.deploy.skip=true clean verify

git add "$POM"
git commit -m "Release $MODULE_DIR $VERSION"
git tag "$TAG"

git push origin main
git push origin "$TAG"

echo "Created and pushed $TAG. GitHub Actions will publish $MODULE_DIR to Maven Central."
