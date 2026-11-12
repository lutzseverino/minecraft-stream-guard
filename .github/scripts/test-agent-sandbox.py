"""Exercise the sandbox verifier's input contract using real local Git commits.

Docker and the agent-layer download are external boundaries, replaced here by
fixtures. The check-program fixture also substitutes container paths, identity
and component tools. These tests do not establish real sandbox acceptance.
"""

import os
from pathlib import Path
import subprocess
import tempfile
import unittest

SCRIPT = Path(__file__).resolve().parents[2] / "docs/development/verify-agent-sandbox.sh"
ORIGIN = "https://github.com/lutzseverino/minecraft-stream-guard.git"


class SandboxInputsTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="streamguard-sandbox-test-")
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.source = self.root / "source"
        self.source.mkdir()
        self.scratch = self.root / "scratch"
        self.scratch.mkdir()
        self.bin = self.root / "bin"
        self.bin.mkdir()
        self.builds = self.root / "build-inputs"
        self.downloads = self.root / "downloads"
        self.env = {
            **os.environ,
            "PATH": f"{self.bin}{os.pathsep}{os.environ['PATH']}",
            "TMPDIR": str(self.scratch),
            "GIT_CONFIG_GLOBAL": os.devnull,
            "GIT_CONFIG_NOSYSTEM": "1",
            "GIT_CONFIG_COUNT": "1",
            "GIT_CONFIG_KEY_0": f"url.{self.source.as_uri()}.insteadOf",
            "GIT_CONFIG_VALUE_0": ORIGIN,
            "TEST_BUILDS": str(self.builds),
            "TEST_DOWNLOADS": str(self.downloads),
            "TEST_CONTAINER_HOME": str(self.root / "container-home"),
            "TEST_CONTAINER_TMP": str(self.root / "container-tmp"),
            "TEST_RUN_CHECKS": "0",
        }
        self.git("init", "-q")
        self.stub("curl", '''#!/usr/bin/env bash
set -eu
printf 'download\\n' >> "$TEST_DOWNLOADS"
while [[ $# -gt 0 ]]; do
  if [[ $1 == -o ]]; then printf 'agent-layer fixture\\n' > "$2"; exit 0; fi
  shift
done
exit 2
''')
        self.stub("docker", '''#!/usr/bin/env bash
set -eu
case "$1" in
  build) cat >> "$TEST_BUILDS" ;;
  image)
    if [[ $2 == inspect ]]; then
      case "$4" in
        *Config.User*) echo agent ;;
        *) echo sha256:fixture ;;
      esac
    fi ;;
  run)
    if [[ $TEST_RUN_CHECKS == 1 ]]; then
      python3 "$TEST_CONTAINER_RUNNER"
    else
      cat > /dev/null
    fi ;;
  rm) ;;
  *) exit 2 ;;
esac
''')

    def stub(self, name, content):
        path = self.bin / name
        path.write_text(content)
        path.chmod(0o755)

    def git(self, *args):
        return subprocess.run(["git", *args], cwd=self.source, env=self.env,
                              capture_output=True, text=True, check=True).stdout.strip()

    def verify(self, inputs):
        for name, content in inputs.items():
            path = self.source / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(content)
        self.git("add", ".")
        self.git("-c", "user.name=Test", "-c", "user.email=test@example.invalid",
                 "commit", "--allow-empty", "-qm", "fixture")
        head = self.git("rev-parse", "HEAD")
        self.env["TEST_HEAD"] = head
        result = subprocess.run(["bash", str(SCRIPT), head], cwd=self.root, env=self.env,
                                capture_output=True, text=True)
        self.assertEqual(list(self.scratch.iterdir()), [], "Verifier scratch was not removed")
        return result

    def test_canonical_input_builds_without_legacy_input(self):
        result = self.verify({".scriptorium/Dockerfile": "FROM canonical-only\n"})
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn("FROM canonical-only\n", self.builds.read_text())

    def test_missing_input_fails_before_download_or_build(self):
        result = self.verify({})
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("Missing toolchain Dockerfile", result.stderr)
        self.assertIn(".scriptorium/Dockerfile", result.stderr)
        self.assertIn(".factory/Dockerfile", result.stderr)
        self.assertFalse(self.downloads.exists())
        self.assertFalse(self.builds.exists())

    def test_legacy_input_keeps_pre_update_revision_runnable(self):
        result = self.verify({".factory/Dockerfile": "FROM legacy-only\n"})
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn("FROM legacy-only\n", self.builds.read_text())

    def test_canonical_input_wins_when_both_layouts_exist(self):
        result = self.verify({
            ".scriptorium/Dockerfile": "FROM canonical-preferred\n",
            ".factory/Dockerfile": "FROM legacy-ignored\n",
        })
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn("FROM canonical-preferred\n", self.builds.read_text())
        self.assertNotIn("legacy-ignored", self.builds.read_text())

    def test_check_program_supports_revisions_before_and_after_caller_tests(self):
        # Execute the complete container program with fixture tools and paths.
        runner = self.root / "run-container.py"
        runner.write_text('''import os
from pathlib import Path
import subprocess
import sys

home = Path(os.environ["TEST_CONTAINER_HOME"]) / os.environ["TEST_HEAD"]
home.mkdir(parents=True)
program = sys.stdin.read().replace("/home/agent", str(home))
program = program.replace("/var/tmp/streamguard-checks", os.environ["TEST_CONTAINER_TMP"])
raise SystemExit(subprocess.run(["bash", "-s", "--", os.environ["TEST_HEAD"],
                                 "sha256:fixture", "agent-layer-fixture"],
                                input=program, text=True).returncode)
''')
        self.env["TEST_CONTAINER_RUNNER"] = str(runner)
        self.env["TEST_RUN_CHECKS"] = "1"
        self.stub("id", '''#!/usr/bin/env bash
case "$1" in
  -u|-g) echo 1000 ;;
  -un) echo agent ;;
  *) echo 'uid=1000(agent) gid=1000(agent)' ;;
esac
''')
        for tool in ("node", "java", "npm", "pnpm"):
            self.stub(tool, "#!/usr/bin/env bash\nexit 0\n")
        inputs = {
            ".factory/Dockerfile": "FROM legacy-only\n",
            "plugin/mvnw": "#!/usr/bin/env bash\nexit 0\n",
            ".github/scripts/test-release-notes.py": "pass\n",
            ".repo-standards/runtime/node_modules/.bin/repo-standards":
                "#!/usr/bin/env bash\nexit 0\n",
            "site/package.json": "{}\n",
        }
        for name, content in inputs.items():
            path = self.source / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(content)
            if content.startswith("#!"):
                path.chmod(0o755)
        for has_caller_tests in (False, True):
            with self.subTest(has_caller_tests=has_caller_tests):
                if has_caller_tests:
                    inputs[".github/scripts/test-agent-sandbox.py"] = (
                        "print('caller-tests-executed')\n"
                    )
                result = self.verify(inputs)
                self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
                self.assertIn("overall: 0", result.stdout)
                self.assertEqual("caller-tests-executed" in result.stdout, has_caller_tests)


if __name__ == "__main__":
    unittest.main()
