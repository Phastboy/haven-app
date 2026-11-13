#!/usr/bin/env python3
"""Publish complete verified artifacts before update.json announces readiness."""
import argparse
import hashlib
import importlib.util
import json
import os
import pathlib
import re
import subprocess
import tempfile

spec = importlib.util.spec_from_file_location("release_metadata", pathlib.Path(__file__).with_name("release-metadata.py"))
metadata_module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(metadata_module)
REPOSITORY = "Phastboy/haven-app"


def gh(*args):
    return subprocess.check_output(["gh", *args], text=True)


def read_metadata(release):
    if not re.fullmatch(r"v\d+\.\d+\.\d+", release["tag_name"]):
        return None
    assets = [a for a in release["assets"] if a["name"] == "update.json"]
    if not assets:
        return None
    if len(assets) != 1:
        raise ValueError("Ambiguous update metadata")
    with tempfile.TemporaryDirectory() as directory:
        gh("release", "download", release["tag_name"], "--repo", REPOSITORY,
           "--pattern", "update.json", "--dir", directory)
        return json.loads((pathlib.Path(directory) / "update.json").read_text())


def check_progression(current, previous):
    if previous and current["versionCode"] <= previous["versionCode"]:
        raise ValueError("New stable releases require an increasing versionCode")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--tag", required=True)
    parser.add_argument("--assets", type=pathlib.Path, required=True)
    args = parser.parse_args()
    if os.environ.get("GITHUB_REPOSITORY") != REPOSITORY:
        raise ValueError("Unexpected publication repository")
    current = json.loads((args.assets / "update.json").read_text())
    code, version = metadata_module.identity((args.assets / "manifest.txt").read_text(),
                                              (args.assets / "apk-provenance.txt").read_text(), args.tag)
    if (current["versionCode"], current["versionName"], current["apkName"]) != (code, version, f"haven-v{version}.apk"):
        raise ValueError("Metadata does not match the verified APK identity")
    apk = args.assets / current["apkName"]
    with apk.open("rb") as stream:
        digest = hashlib.file_digest(stream, "sha256").hexdigest()
    if apk.stat().st_size != current["size"] or digest != current["sha256"]:
        raise ValueError("APK changed after verification")
    release = json.loads(gh("api", f"repos/{REPOSITORY}/releases/tags/{args.tag}"))
    if release["draft"] or release["prerelease"]:
        raise ValueError("Public download publication requires a stable published release")
    existing = read_metadata(release)
    if existing:
        for key in ["versionCode", "versionName", "apkName", "sha256", "size"]:
            if current[key] != existing[key]:
                raise ValueError("Refusing to replace an advertised APK with different bytes or identity")
    releases = json.loads(gh("api", f"repos/{REPOSITORY}/releases?per_page=100"))
    previous = sorted((r for r in releases if r["tag_name"] != args.tag and
                       not r["draft"] and not r["prerelease"] and r.get("published_at")),
                      key=lambda r: r["published_at"], reverse=True)
    baseline_found = False
    for candidate in previous:
        metadata = read_metadata(candidate)
        if metadata:
            check_progression(current, metadata)
            baseline_found = True
            break
    if previous and not baseline_found:
        raise ValueError("Previous release metadata is missing; resolve version progression evidence")
    artifacts = [apk, args.assets / "SHA256SUMS", args.assets / "apk-provenance.txt", args.assets / "manifest.txt"]
    gh("release", "upload", args.tag, "--repo", REPOSITORY,
       *(str(path) for path in artifacts), "--clobber")
    gh("release", "upload", args.tag, "--repo", REPOSITORY,
       str(args.assets / "update.json"), "--clobber")


if __name__ == "__main__":
    main()
