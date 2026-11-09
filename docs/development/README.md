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
installed standards checks. Combined standards and sandbox adoption is separate
work under [#21](https://github.com/lutzseverino/minecraft-stream-guard/issues/21).
The retained `.factory/Dockerfile` is the historical plugin toolchain image;
its existing Actions check remains, and does not prove combined acceptance.

## Documents

- [Plugin architecture](architecture.md): plugin dependency direction and composition.
- [Site architecture](../../site/docs/development/architecture/README.md): site modules and feed consumption.
- [Documentation Templates](templates/README.md): documentation templates.
