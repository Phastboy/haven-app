import importlib.util
import hashlib
import json
import pathlib
import tempfile
import unittest
from unittest.mock import patch


def load(name):
    spec = importlib.util.spec_from_file_location(name.replace("-", "_"), pathlib.Path(__file__).with_name(name + ".py"))
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


metadata = load("release-metadata")
publication = load("publish-release")


class ReleaseMetadataTests(unittest.TestCase):
    manifest = "package: name='com.aistudio.haven.xvykpt' versionCode='17' versionName='0.0.7' platformBuildVersionName='x'\n"
    provenance = f"Signer #1 certificate SHA-256 digest: {metadata.SIGNER}\n"

    def test_identity_tag_and_signer(self):
        self.assertEqual(metadata.identity(self.manifest, self.provenance, "v0.0.7"), (17, "0.0.7"))
        for manifest, provenance, tag in [
            (self.manifest, self.provenance, "v0.0.6"),
            (self.manifest + "application-debuggable\n", self.provenance, "v0.0.7"),
            (self.manifest.replace(metadata.PACKAGE, "other.package"), self.provenance, "v0.0.7"),
            (self.manifest, "Signer #1 certificate SHA-256 digest: " + "0" * 64, "v0.0.7"),
        ]:
            with self.assertRaises(ValueError):
                metadata.identity(manifest, provenance, tag)

    def test_version_progression(self):
        publication.check_progression({"versionCode": 17}, {"versionCode": 16})
        for code in [15, 16]:
            with self.assertRaises(ValueError):
                publication.check_progression({"versionCode": code}, {"versionCode": 16})

    def test_actual_bytes_drive_metadata(self):
        with tempfile.TemporaryDirectory() as directory:
            root = pathlib.Path(directory)
            apk = root / "app-release.apk"
            apk.write_bytes(b"synthetic fixture, not an installable APK")
            result = metadata.generate(apk, self.manifest, self.provenance, root / "assets", "a" * 40)
            self.assertEqual(result["apkName"], "haven-v0.0.7.apk")
            self.assertEqual(result["size"], apk.stat().st_size)
            self.assertEqual((root / "assets" / result["apkName"]).read_bytes(), apk.read_bytes())
            self.assertEqual(result["packageName"], metadata.PACKAGE)
            self.assertTrue((root / "assets" / "SHA256SUMS").read_text().startswith(result["sha256"]))


class PublicationTests(unittest.TestCase):
    def publish(self, fail_upload=False, existing_changed=False, previous_code=16,
                missing_newest=False, existing_ready=False, repair=False, wrong_asset=False):
        with tempfile.TemporaryDirectory() as directory:
            root = pathlib.Path(directory)
            apk = root / "app.apk"
            apk.write_bytes(b"synthetic APK publication fixture")
            assets = root / "assets"
            current = metadata.generate(apk, ReleaseMetadataTests.manifest,
                                        ReleaseMetadataTests.provenance, assets, "a" * 40)
            (assets / "manifest.txt").write_text(ReleaseMetadataTests.manifest)
            (assets / "apk-provenance.txt").write_text(ReleaseMetadataTests.provenance)
            release = dict(tag_name="v0.0.7", draft=False, prerelease=False,
                           published_at="2026-11-14T12:00:00Z", assets=[])
            previous = dict(tag_name="v0.0.6", draft=False, prerelease=False,
                            published_at="2026-11-12T15:10:49Z", assets=[])
            older = dict(previous, tag_name="v0.0.5", published_at="2026-11-10T12:00:00Z")
            if existing_ready:
                for name in [current["apkName"], "SHA256SUMS", "apk-provenance.txt", "manifest.txt"]:
                    if repair and name == "manifest.txt":
                        continue
                    data = (assets / name).read_bytes()
                    release["assets"].append(dict(name=name, state="uploaded", size=len(data),
                        digest="sha256:" + hashlib.sha256(data).hexdigest(),
                        browser_download_url=f"https://github.com/{publication.REPOSITORY}/releases/download/v0.0.7/{name}"))
                if wrong_asset:
                    release["assets"][0]["digest"] = "sha256:" + "0" * 64
            calls = []

            def command(*args):
                if args[0] == "api":
                    return json.dumps(release if args[1].endswith("/v0.0.7") else [release, previous, older])
                self.assertEqual(args[:2], ("release", "upload"))
                calls.append(args)
                if fail_upload:
                    raise RuntimeError("upload failed")
                return ""

            def previous_metadata(value):
                if value["tag_name"] == "v0.0.6":
                    return None if missing_newest else dict(versionCode=previous_code)
                if value["tag_name"] == "v0.0.5":
                    return dict(versionCode=15)
                if existing_changed:
                    return dict(current, sha256="0" * 64)
                return current if existing_ready else None

            with patch.object(publication, "gh", command), patch.object(publication, "read_metadata", previous_metadata), \
                 patch.object(publication, "read_asset", lambda release, name, limit: (assets / name).read_text()), \
                 patch.dict(publication.os.environ, GITHUB_REPOSITORY=publication.REPOSITORY), \
                 patch("sys.argv", ["publish-release.py", "--tag", "v0.0.7", "--assets", str(assets)]):
                if fail_upload or existing_changed or wrong_asset or ((missing_newest or previous_code >= 17) and not existing_ready):
                    with self.assertRaises((ValueError, RuntimeError)):
                        publication.main()
                else:
                    publication.main()
            return calls

    def test_ready_marker_is_last(self):
        calls = self.publish()
        self.assertEqual(len(calls), 2)
        self.assertFalse(any(arg.endswith("update.json") for arg in calls[0]))
        self.assertTrue(any(arg.endswith("update.json") for arg in calls[1]))

    def test_failed_upload_does_not_publish_marker(self):
        calls = self.publish(fail_upload=True)
        self.assertEqual(len(calls), 1)
        self.assertFalse(any(arg.endswith("update.json") for arg in calls[0]))

    def test_immutable_identity_and_downgrades_fail_before_upload(self):
        self.assertEqual(self.publish(existing_changed=True), [])
        self.assertEqual(self.publish(previous_code=17), [])

    def test_missing_newest_metadata_cannot_use_an_older_baseline(self):
        self.assertEqual(self.publish(missing_newest=True), [])

    def test_matching_complete_publication_is_a_noop(self):
        self.assertEqual(self.publish(existing_ready=True), [])

    def test_repair_only_uploads_missing_evidence_without_clobber(self):
        calls = self.publish(existing_ready=True, repair=True)
        self.assertEqual(len(calls), 1)
        self.assertTrue(calls[0][-1].endswith("manifest.txt"))
        self.assertNotIn("--clobber", calls[0])
        self.assertFalse(any(arg.endswith(".apk") or arg.endswith("update.json") for arg in calls[0]))

    def test_changed_existing_asset_fails_before_repair(self):
        self.assertEqual(self.publish(existing_ready=True, wrong_asset=True), [])


if __name__ == "__main__":
    unittest.main()
