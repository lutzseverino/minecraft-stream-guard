# Development

Build, test, and maintain StreamGuard.

## Setup and validation

Use Python 3 for release-note tests, JDK 21, a POSIX shell, curl or wget, unzip, and the committed Maven wrapper (Maven 3.9.16). The build emits Java 17-compatible bytecode. Node.js 24 is required for the installed delivery scripts, not for plugin compilation.

From the repository root:

```sh
./mvnw --batch-mode --no-transfer-progress verify
python3 .github/scripts/test-release-notes.py
```

This canonical check enforces dependency convergence, Spotless formatting, compilation with warnings as errors, JUnit tests including ArchUnit architecture checks, and jar shading. Output is `target/StreamGuard-<version>.jar`. CI runs Maven with JDK 21 and the Python release-note tests. Those tests exercise annotation extraction and rejection of invalid release tags in disposable local repositories.

Restore the delivery runtime with `npm ci --ignore-scripts --prefix .repo-standards/runtime` and run `.repo-standards/runtime/node_modules/.bin/repo-standards check --json` for installed standards checks. The factory installs agent tools separately from the contextless toolchain image.

## Documents

- [Architecture](architecture.md): Architecture guidance.
- [Documentation Templates](templates/README.md): Documentation Templates guidance.
