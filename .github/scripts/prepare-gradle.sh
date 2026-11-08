#!/usr/bin/env bash
set -euo pipefail

# Prefer the checked-in wrapper. A distribution CDN outage must not change the pin.
if ./gradlew --version; then
  printf 'GRADLE_COMMAND=./gradlew\n' >> "$GITHUB_ENV"
  exit 0
fi

properties=gradle/wrapper/gradle-wrapper.properties
version=$(sed -n 's/^distributionUrl=.*gradle-\([0-9][0-9.]*\)-bin.zip$/\1/p' "$properties")
checksum=$(sed -n 's/^distributionSha256Sum=//p' "$properties")
[[ "$version" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]
[[ "$checksum" =~ ^[a-f0-9]{64}$ ]]

echo "Wrapper download unavailable; checking Gradle's official GitHub release mirror."
asset_id=$(gh api "repos/gradle/gradle-distributions/releases/tags/v$version" \
  --jq ".assets[] | select(.name == \"gradle-$version-bin.zip\") | .id")
[[ "$asset_id" =~ ^[0-9]+$ ]]
archive="$RUNNER_TEMP/gradle-$version-bin.zip"
gh api "repos/gradle/gradle-distributions/releases/assets/$asset_id" \
  --header 'Accept: application/octet-stream' > "$archive"
printf '%s  %s\n' "$checksum" "$archive" | sha256sum --check --strict
unzip -q "$archive" -d "$RUNNER_TEMP"
executable="$RUNNER_TEMP/gradle-$version/bin/gradle"
test -x "$executable"
"$executable" --version
printf 'GRADLE_COMMAND=%s\n' "$executable" >> "$GITHUB_ENV"
