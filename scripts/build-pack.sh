#!/usr/bin/env bash
# Assembles the modpack with packwiz:
#   1. resolves every slug in modlist.txt from Modrinth (with dependencies),
#   2. bundles the freshly built Warfront jar,
#   3. exports a Modrinth .mrpack you can import into Prism Launcher, the Modrinth App, ATLauncher, etc.
#
# Requirements: packwiz on PATH (go install github.com/packwiz/packwiz@latest) and a built
# Warfront jar (cd mods/warfront && ./gradlew build).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$ROOT/build"
WORK="$OUT/pack"
rm -rf "$WORK" && mkdir -p "$OUT" && cp -r "$ROOT/pack" "$WORK"
cd "$WORK"

failed=()
while IFS= read -r line; do
  slug="${line%%#*}"; slug="$(echo "$slug" | xargs)"
  [ -z "$slug" ] && continue
  echo "::group::add $slug"
  if packwiz -y modrinth add "$slug"; then
    echo "added $slug"
  else
    echo "WARN: could not add $slug"
    failed+=("$slug")
  fi
  echo "::endgroup::"
done < "$ROOT/modlist.txt"

JAR="$(ls "$ROOT"/mods/warfront/build/libs/warfront-*.jar 2>/dev/null | grep -v -- '-sources' | head -n1 || true)"
if [ -n "$JAR" ]; then
  mkdir -p mods
  cp "$JAR" mods/
  echo "bundled $(basename "$JAR")"
else
  echo "WARN: no Warfront jar found; build it first (mods/warfront: ./gradlew build)"
fi

packwiz refresh
packwiz modrinth export -o "$OUT/Warfront-Age-of-Banners.mrpack"

# Keep the resolved metadata so it can be committed back into pack/ if desired.
rm -rf "$OUT/resolved" && mkdir -p "$OUT/resolved" && cp -r "$WORK"/mods "$WORK"/index.toml "$WORK"/pack.toml "$OUT/resolved/" 2>/dev/null || true

{
  echo "# Pack build report"
  echo
  echo "Mods resolved: $(ls "$WORK"/mods/*.pw.toml 2>/dev/null | wc -l) (including dependencies)"
  if [ ${#failed[@]} -gt 0 ]; then
    echo
    echo "Slugs that could not be added (no NeoForge 1.21.1 build or wrong slug):"
    for s in "${failed[@]}"; do echo "- $s"; done
  fi
} | tee "$OUT/pack-report.md"
