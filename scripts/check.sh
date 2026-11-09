#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")/.."
(
  cd plugin
  ./mvnw --batch-mode --no-transfer-progress verify
)
python3 .github/scripts/test-release-notes.py
(
  cd site
  pnpm validate
)
