"""Exercise release-note extraction using tags in disposable local repositories."""

import os
from pathlib import Path
import subprocess
import tempfile
import unittest

SCRIPT = Path(__file__).with_name("read-release-notes.sh").resolve()


class ReleaseNotesTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="streamguard-release-test-")
        self.addCleanup(self.temp.cleanup)
        self.env = {**os.environ, "GIT_CONFIG_GLOBAL": os.devnull, "GIT_CONFIG_NOSYSTEM": "1"}
        self.root = Path(self.temp.name)
        self.git("init", "-q")
        self.git("-c", "user.name=Test", "-c", "user.email=test@example.invalid",
                 "commit", "--allow-empty", "-qm", "fixture")
        self.output = self.root / "notes.md"

    def git(self, *args, **kwargs):
        return subprocess.run(["git", *args], cwd=self.root, env=self.env, check=True,
                              capture_output=True, **kwargs)

    def annotated(self, message):
        # verbatim permits deliberately empty annotations for negative coverage.
        self.git("-c", "user.name=Test", "-c", "user.email=test@example.invalid",
                 "tag", "-a", "plugin-v1.0.0", "--cleanup=verbatim", "-F", "-",
                 input=message.encode())

    def extract(self):
        return subprocess.run(["bash", str(SCRIPT), "plugin-v1.0.0", str(self.output)],
                              cwd=self.root, env=self.env, capture_output=True, text=True)

    def test_preserves_multiline_markdown_and_delimiter_like_text(self):
        notes = "Release title\n\n## Changes\n\n- Fix behavior\nEOF\nbody<<EOF\n"
        self.annotated(notes)
        result = self.extract()
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(self.output.read_text(), notes)

    def test_rejects_lightweight_tag(self):
        self.git("tag", "plugin-v1.0.0")
        result = self.extract()
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("annotated tag", result.stderr)

    def test_rejects_empty_and_whitespace_annotations(self):
        for message in ("", " \n\t\n"):
            with self.subTest(message=message):
                self.annotated(message)
                result = self.extract()
                self.assertNotEqual(result.returncode, 0)
                self.assertIn("non-empty", result.stderr)
                self.git("tag", "-d", "plugin-v1.0.0")

    def test_signed_annotation_publishes_only_message(self):
        notes = "Release notes\n\n- Fix behavior\n\n"
        signature = "-----BEGIN PGP SIGNATURE-----\nfixture\n-----END PGP SIGNATURE-----\n"
        self.annotated(notes + signature)
        result = self.extract()
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(self.output.read_text(), notes)

    def test_rejects_signed_empty_annotation(self):
        self.annotated("-----BEGIN PGP SIGNATURE-----\nfixture\n-----END PGP SIGNATURE-----\n")
        result = self.extract()
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("non-empty", result.stderr)

    def test_rejects_missing_tag(self):
        result = self.extract()
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("annotated tag", result.stderr)


class PluginReleaseVersionTest(unittest.TestCase):
    def version(self, tag, pom="1.2.3"):
        return subprocess.run(
            ["bash", str(SCRIPT.with_name("plugin-release-version.sh")), tag, pom],
            capture_output=True, text=True,
        )

    def test_accepts_component_version(self):
        result = self.version("plugin-v1.2.3")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(result.stdout, "1.2.3\n")

    def test_rejects_other_components_historical_refs_and_non_numeric_versions(self):
        for tag in ("v1.2.3", "site-v1.2.3", "main", "plugin-v01.2.3",
                    "plugin-v1.2.3-beta.1", "plugin-v1.2.3-extra", "plugin-v1.2"):
            with self.subTest(tag=tag):
                result = self.version(tag)
                self.assertNotEqual(result.returncode, 0)
                self.assertIn("plugin-v", result.stderr)

    def test_rejects_maven_version_mismatch(self):
        result = self.version("plugin-v1.2.4")
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("does not match", result.stderr)


if __name__ == "__main__":
    unittest.main()
