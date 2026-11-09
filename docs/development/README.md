# Development

Build, test, and maintain both StreamGuard components.

## Setup and validation

Use Python 3, JDK 21, a POSIX shell, curl or wget, unzip, Node.js 24 or 26,
and pnpm 11.28.9. The plugin retains its Maven 3.9.16 wrapper and Java 17
bytecode target; the site retains its own pnpm lockfile and Vite toolchain.

Install site dependencies and run the canonical combined check from the
repository root:

```sh
pnpm --dir site install --frozen-lockfile
bash scripts/check.sh
```

The root check invokes these component commands in order:

```sh
(cd plugin && ./mvnw --batch-mode --no-transfer-progress verify)
python3 .github/scripts/test-release-notes.py
(cd site && pnpm validate)
```

Maven enforces dependency convergence, Spotless formatting, compiler warnings
as errors, JUnit/ArchUnit tests, and jar shading. Its output is
`plugin/target/StreamGuard-<version>.jar`. The Python tests exercise component
release/version routing and annotated release-note extraction in disposable
repositories. The [site development guide](../../site/docs/development/README.md)
describes its formatter, lint, TypeScript, tests, architecture, documentation,
registry integrity and static build checks. Site output is `site/dist/`.

CI runs the plugin checks with JDK 21 and site validation on both Node.js 24
and 26. Maven caches use `plugin/pom.xml`; site caches use
`site/pnpm-lock.yaml`. Component commands run in their respective directories.
No deployment is performed by these checks.

Restore the delivery runtime with
`npm ci --ignore-scripts --prefix .repo-standards/runtime` and run
`.repo-standards/runtime/node_modules/.bin/repo-standards check --json` for
installed standards checks.

The root `.factory/Dockerfile` provides Node.js 24, pnpm 11.28.9, JDK 21,
Python 3 and Maven-wrapper prerequisites system-wide. It builds without a
repository context; the factory adds its agent layer and unprivileged user.
Project dependencies install in each fresh checkout.

To verify the combined sandbox on a Mac with Docker and Git, run:

```sh
bash docs/development/verify-agent-sandbox.sh <full-commit-sha>
```

The script fetches that exact revision into a fresh directory, builds its
contextless toolchain and the pinned factory agent layer, then clones the
revision inside the resulting image as `agent`. It runs every canonical plugin
and site check, plus installed standards checks, and prints the head, image
identity, user, tool versions, working directories and exit outcomes. It removes
its temporary checkout, container and image tags on exit. Keep the result on
the pull request; source checks do not establish live factory delivery.

## Documents

- [Plugin architecture](architecture.md): plugin dependency direction and composition.
- [Sandbox verification](verify-agent-sandbox.sh): build both image layers and validate a fresh checkout as the unprivileged agent.
- [Documentation Templates](templates/README.md): documentation templates.
