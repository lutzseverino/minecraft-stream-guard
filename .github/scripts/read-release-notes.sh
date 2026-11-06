#!/usr/bin/env bash
set -euo pipefail

ref="refs/tags/${1:?release tag required}"
notes_file="${2:?notes output file required}"
if [[ "$(git cat-file -t "$ref" 2>/dev/null || true)" != tag ]]; then
  echo "Release tag must be an annotated tag with non-empty release notes as its message." >&2
  exit 1
fi

# Read the annotation, excluding the tag object's headers.
git cat-file tag "$ref" | sed '1,/^$/d' > "$notes_file"
if ! grep -q '[^[:space:]]' "$notes_file"; then
  echo "Annotated release tag must have a non-empty release notes message." >&2
  exit 1
fi
