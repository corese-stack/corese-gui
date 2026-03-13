import json
import os
import subprocess
import unittest
from pathlib import Path


SCRIPT_PATH = (
    Path(__file__).resolve().parents[1] / ".github" / "scripts" / "resolve_docs_context.py"
)


def run_script(env_overrides: dict[str, str]) -> dict[str, str]:
    env = {
        **os.environ,
        "GITHUB_REPOSITORY": "corese-stack/corese-gui",
        "GITHUB_EVENT_NAME": "workflow_dispatch",
        "GITHUB_REF_NAME": "main",
        "DEV_PRERELEASE_REF": "dev-prerelease",
        "MINIMAL_VERSION": "5.0.0",
        "GITHUB_RELEASES_JSON": "[]",
        **env_overrides,
    }
    result = subprocess.run(
        ["python3", str(SCRIPT_PATH)],
        check=True,
        capture_output=True,
        text=True,
        env=env,
    )
    output: dict[str, str] = {}
    for line in result.stdout.splitlines():
        key, value = line.split("=", 1)
        output[key] = value
    return output


class ResolveDocsContextTests(unittest.TestCase):
    def test_manual_dispatch_uses_selected_ref(self):
        context = run_script(
            {
                "GITHUB_EVENT_NAME": "workflow_dispatch",
                "GITHUB_REF_NAME": "release/5.0.0",
                "GITHUB_RELEASES_JSON": json.dumps(
                    [
                        {"tag_name": "v5.0.0", "draft": False, "prerelease": False},
                        {"tag_name": "v5.1.0", "draft": False, "prerelease": False},
                    ]
                ),
            }
        )

        self.assertEqual(context["source_ref"], "release/5.0.0")
        self.assertEqual(context["published_stable_tags"], "v5.1.0,v5.0.0")
        self.assertEqual(context["smv_tag_whitelist"], r"^(dev\-prerelease|v5\.1\.0|v5\.0\.0)$")

    def test_workflow_run_uses_source_commit_for_versioned_build(self):
        context = run_script(
            {
                "GITHUB_EVENT_NAME": "workflow_run",
                "GITHUB_REF_NAME": "main",
                "WORKFLOW_RUN_HEAD_SHA": "abc123def456",
            }
        )

        self.assertEqual(context["source_ref"], "abc123def456")
        self.assertEqual(context["published_stable_tags"], "")
        self.assertEqual(context["smv_tag_whitelist"], r"^(dev\-prerelease)$")

    def test_release_published_includes_current_tag_even_before_api_refresh(self):
        context = run_script(
            {
                "GITHUB_EVENT_NAME": "release",
                "GITHUB_REF_NAME": "v5.0.0",
                "RELEASE_TAG_NAME": "v5.0.0",
                "GITHUB_RELEASES_JSON": json.dumps(
                    [
                        {"tag_name": "v4.6.1", "draft": False, "prerelease": False},
                        {"tag_name": "v5.0.0", "draft": True, "prerelease": False},
                    ]
                ),
            }
        )

        self.assertEqual(context["source_ref"], "v5.0.0")
        self.assertEqual(context["published_stable_tags"], "v5.0.0")
        self.assertEqual(context["smv_tag_whitelist"], r"^(dev\-prerelease|v5\.0\.0)$")


if __name__ == "__main__":
    unittest.main()
