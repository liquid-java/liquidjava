#!/usr/bin/env bash

set -euo pipefail

if [ $# -lt 1 ] || [ $# -gt 2 ]; then
    echo "Usage: $0 <api|verifier> [version]"
    exit 1
fi

MODULE=$1
VERSION=${2:-}

if [ "$MODULE" != "api" ] && [ "$MODULE" != "verifier" ]; then
    echo "Invalid module: $MODULE"
    echo "Usage: $0 <api|verifier> [version]"
    exit 1
fi

MODULE_DIR="liquidjava-$MODULE"
POM="$MODULE_DIR/pom.xml"

if [ -z "$VERSION" ]; then
    CURRENT_VERSION=$(perl -0ne "print \$1 if m#<artifactId>\\Q$MODULE_DIR\\E</artifactId>\\s*<version>([^<]+)</version>#" "$POM")

    if [ -z "$CURRENT_VERSION" ]; then
        echo "Could not read current version from $POM"
        exit 1
    fi

    if ! [[ "$CURRENT_VERSION" =~ ^[0-9]+(\.[0-9]+){1,2}$ ]]; then
        echo "Cannot automatically bump non-numeric version: $CURRENT_VERSION"
        echo "Pass the release version explicitly."
        exit 1
    fi

    IFS=. read -ra VERSION_PARTS <<<"$CURRENT_VERSION"
    LAST_INDEX=$((${#VERSION_PARTS[@]} - 1))
    VERSION_PARTS[$LAST_INDEX]=$((${VERSION_PARTS[$LAST_INDEX]} + 1))
    VERSION=$(IFS=.; echo "${VERSION_PARTS[*]}")

    echo "Bumping $MODULE_DIR from $CURRENT_VERSION to $VERSION"
fi

if ! [[ "$VERSION" =~ ^[0-9]+(\.[0-9]+){1,2}(-[A-Za-z0-9.-]+)?$ ]]; then
    echo "Invalid version: $VERSION"
    echo "Expected format: 1.2.3"
    exit 1
fi

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

./mvnw -f "$POM" -B --fail-fast -Dgpg.skip=true -Dmaven.deploy.skip=true clean verify

perl -0pi -e "s#(<artifactId>$MODULE_DIR</artifactId>\\s*<version>)[^<]+(</version>)#\${1}$VERSION\${2}#" "$POM"

if git diff --quiet -- "$POM"; then
    echo "$POM is already at version $VERSION"
    exit 1
fi

git add "$POM"
git commit -m "Release $MODULE_DIR $VERSION"
git tag "$TAG"

git push origin main
git push origin "$TAG"

echo "Created and pushed $TAG. GitHub Actions will publish $MODULE_DIR to Maven Central."
