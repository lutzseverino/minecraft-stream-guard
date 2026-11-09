#!/usr/bin/env bash
set -euo pipefail

tag="${1:?release tag required}"
pom_version="${2:?Maven project version required}"
if [[ ! "$tag" =~ ^plugin-v(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)$ ]]; then
  echo "Release tag must be an exact numeric Romantic Version, such as plugin-v1.0.0." >&2
  exit 1
fi
version="${tag#plugin-v}"
if [[ "$version" != "$pom_version" ]]; then
  echo "Tag $version does not match plugin/pom.xml $pom_version." >&2
  exit 1
fi
printf '%s\n' "$version"
