"""Offline tests for the release task; never call GitHub."""

import importlib.util
import unittest
from pathlib import Path
from unittest.mock import patch

spec = importlib.util.spec_from_file_location("release_task", Path(__file__).with_name("release-task.py"))
release = importlib.util.module_from_spec(spec)
spec.loader.exec_module(release)


class ReleaseTaskTests(unittest.TestCase):
    def test_dispatch_waits_until_no_ci_run_is_unfinished(self):
        order = []
        pending = iter([[7], [7], []])

        def fake_running():
            order.append("poll")
            return next(pending)

        with (
            patch.object(release, "running_ci", side_effect=fake_running),
            patch.object(release.time, "sleep"),
            patch.object(release, "run", side_effect=lambda *a, **k: order.append("dispatch") or ""),
        ):
            release.release(follow=False)
        self.assertEqual(order, ["poll", "poll", "poll", "dispatch"])

    def test_without_follow_dispatches_and_does_not_watch(self):
        with patch.object(release, "wait_for_ci"), patch.object(release, "run") as run:
            release.release(follow=False)
        self.assertEqual([call.args[:2] for call in run.call_args_list], [("gh", "workflow")])

    def test_follow_watches_the_dispatched_run(self):
        with (
            patch.object(release, "wait_for_ci"),
            patch.object(release, "dispatch", return_value=42),
            patch.object(release, "run") as run,
        ):
            release.release(follow=True)
        self.assertEqual(run.call_args.args, ("gh", "run", "watch", "42", "--exit-status"))


if __name__ == "__main__":
    unittest.main()
