#!/usr/bin/env bash
set -euo pipefail

ref="refs/tags/${1:?release tag required}"
notes_file="${2:?notes output file required}"
if [[ "$(git cat-file -t "$ref" 2>/dev/null || true)" != tag ]]; then
  echo "Release tag must be an annotated tag with non-empty release notes as its message." >&2
  exit 1
fi

# Git identifies the signature; exclude it from the annotation's prose.
signature=$(git for-each-ref --format='%(contents:signature)' "$ref")
signature_header=${signature%%$'\n'*}
git cat-file tag "$ref" | sed '1,/^$/d' | awk -v header="$signature_header" '
  header != "" && $0 == header { in_signature = 1 }
  !in_signature { print }
' > "$notes_file"
if ! grep -q '[^[:space:]]' "$notes_file"; then
  echo "Annotated release tag must have a non-empty release notes message." >&2
  exit 1
fi
