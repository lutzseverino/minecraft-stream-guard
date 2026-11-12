#!/usr/bin/env bash
# Run on a Mac with Docker Desktop and Git; no credentials or host configuration.
set -euo pipefail
head_sha=${1:?Usage: bash verify-agent-sandbox.sh FULL_PR_HEAD_SHA}
case "$head_sha" in
  *[!0-9a-f]*|'') echo 'Expected a full lowercase Git commit SHA' >&2; exit 2 ;;
esac
[[ ${#head_sha} == 40 ]] || exit 2
scratch=$(mktemp -d "${TMPDIR:-/var/tmp}/minecraft-stream-guard-sandbox.XXXXXX")
export TMPDIR="$scratch"
image="minecraft-stream-guard-sandbox:${head_sha}"
toolchain="${image}-toolchain"
container="minecraft-stream-guard-check-${head_sha:0:12}-$$"
cleanup() {
  docker rm -f "$container" >/dev/null 2>&1 || true
  docker image rm "$image" "$toolchain" >/dev/null 2>&1 || true
  rm -rf "$scratch"
}
trap cleanup EXIT

# Fetch exactly the reviewed commit into a new checkout, without submodules.
git init -q "$scratch/source"
git -C "$scratch/source" remote add origin https://github.com/lutzseverino/minecraft-stream-guard.git
git -C "$scratch/source" fetch --depth 1 origin "$head_sha"
git -C "$scratch/source" checkout --detach FETCH_HEAD
test "$(git -C "$scratch/source" rev-parse HEAD)" = "$head_sha"

# Prefer the canonical input while supporting the pre-update source layout.
dockerfile="$scratch/source/.scriptorium/Dockerfile"
if [[ ! -f "$dockerfile" ]]; then
  dockerfile="$scratch/source/.factory/Dockerfile"
fi
if [[ ! -f "$dockerfile" ]]; then
  echo 'Missing toolchain Dockerfile: expected .scriptorium/Dockerfile or pre-update .factory/Dockerfile' >&2
  exit 2
fi

# Use the published factory's real agent layer, pinned to its immutable release
# commit. Neither Docker build receives the checkout as a build context.
layer_revision=008d3594c249e74b4fa8b21488b6bb124c602228
curl -fsSL "https://raw.githubusercontent.com/lutzseverino/scriptorium/${layer_revision}/src/agent-layer.Dockerfile" \
  -o "$scratch/agent-layer.Dockerfile"
docker build --tag "$toolchain" - < "$dockerfile"
docker build --tag "$image" --build-arg "TOOLCHAIN_IMAGE=$toolchain" \
  --build-arg "AGENT_UID=$(id -u)" --build-arg "AGENT_GID=$(id -g)" \
  - < "$scratch/agent-layer.Dockerfile"
image_digest=$(docker image inspect --format '{{.Id}}' "$image")
test "$(docker image inspect --format '{{.Config.User}}' "$image")" = agent

# Clone inside the image, so checks run on a fresh Linux-owned tree, not a
# privileged host-mounted checkout. No provider credentials enter the container.
docker run -i --cpus 4 --name "$container" --entrypoint bash "$image" -s -- "$head_sha" "$image_digest" "$layer_revision" <<'CHECKS'
set -uo pipefail
head_sha=$1
image_digest=$2
layer_revision=$3
test "$(id -u)" -ne 0 && test "$(id -un)" = agent || exit 2
printf 'identity: '; id
printf 'HOME: %s\n' "$HOME"
node --version; npm --version; java -version
pnpm --version; python3 --version
mkdir -p /var/tmp/streamguard-checks
export TMPDIR=/var/tmp/streamguard-checks
cd /home/agent
git init -q checkout && cd checkout
git remote add origin https://github.com/lutzseverino/minecraft-stream-guard.git
git fetch --depth 1 origin "$head_sha" && git checkout --detach FETCH_HEAD || exit 2
test "$(git rev-parse HEAD)" = "$head_sha" || exit 2
names=(); outcomes=(); failed=0
check() {
  local name=$1 directory=$2
  shift 2
  printf '\nCHECK %s cwd=%s command=' "$name" "$PWD/$directory"
  printf '%q ' "$@"; printf '\n'
  (cd "$directory" && "$@")
  local result=$?
  names+=("$name"); outcomes+=("$result")
  if [[ $result != 0 ]]; then failed=1; fi
}
# Historical revisions predate this caller test; run their own check list.
if [[ -f .github/scripts/test-agent-sandbox.py ]]; then
  check sandbox-inputs . python3 .github/scripts/test-agent-sandbox.py
fi
check plugin-maven-version plugin ./mvnw --version
check plugin-verify plugin ./mvnw --batch-mode --no-transfer-progress verify
check plugin-release-notes . python3 .github/scripts/test-release-notes.py
check site-install site pnpm install --frozen-lockfile
check site-validate site pnpm validate
check standards-runtime . npm ci --ignore-scripts --prefix .repo-standards/runtime
check standards . .repo-standards/runtime/node_modules/.bin/repo-standards check --json
check clean-tree . git diff --exit-code
printf '\nBEGIN STREAMGUARD SANDBOX RESULT\nhead: %s\nimage: %s\nagent-layer: %s\n' "$head_sha" "$image_digest" "$layer_revision"
printf 'identity: '; id
for ((i=0; i<${#names[@]}; i++)); do
  printf '%s: exit %s\n' "${names[$i]}" "${outcomes[$i]}"
done
printf 'overall: %s\nEND STREAMGUARD SANDBOX RESULT\n' "$failed"
exit "$failed"
CHECKS
