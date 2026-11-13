#!/usr/bin/env python3
"""Publish complete verified artifacts before update.json announces readiness."""
import argparse
import datetime
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


def asset_for(release, name):
    assets = [asset for asset in release["assets"] if asset["name"] == name]
    if not assets:
        return None
    expected = f"https://github.com/{REPOSITORY}/releases/download/{release['tag_name']}/{name}"
    if len(assets) != 1 or assets[0].get("state") != "uploaded" or assets[0].get("browser_download_url") != expected:
        raise ValueError("Ambiguous, incomplete or foreign release asset")
    return assets[0]


def recover_starters(release, names):
    """Remove only empty uploads unchanged beyond the workflow's 30m timeout."""
    cutoff = datetime.datetime.now(datetime.timezone.utc) - datetime.timedelta(hours=1)
    for name in names:
        matches = [a for a in release["assets"] if a["name"] == name]
        if len(matches) != 1:
            continue  # Duplicates remain a validation error, never a cleanup target.
        asset = matches[0]
        if asset.get("state") != "starter":
            continue
        updated = datetime.datetime.fromisoformat(asset["updated_at"].replace("Z", "+00:00"))
        if asset.get("size") != 0 or updated >= cutoff:
            raise ValueError("Starter upload is not confirmed abandoned and empty")
        endpoint = f"repos/{REPOSITORY}/releases/assets/{int(asset['id'])}"
        latest = json.loads(gh("api", endpoint))
        for key in ["id", "name", "state", "size", "updated_at", "browser_download_url"]:
            if latest.get(key) != asset.get(key):
                raise ValueError("Starter upload changed during recovery; retry later")
        if latest.get("browser_download_url") != f"https://github.com/{REPOSITORY}/releases/download/{release['tag_name']}/{name}":
            raise ValueError("Foreign starter asset")
        gh("api", "--method", "DELETE", endpoint)
        release["assets"].remove(asset)


def read_asset(release, name, limit):
    asset = asset_for(release, name)
    if asset is None or not 0 < asset["size"] <= limit:
        raise ValueError("Missing or oversized release evidence")
    with tempfile.TemporaryDirectory() as directory:
        gh("release", "download", release["tag_name"], "--repo", REPOSITORY,
           "--pattern", name, "--dir", directory)
        body = (pathlib.Path(directory) / name).read_bytes()
    if len(body) != asset["size"] or asset.get("digest") != "sha256:" + hashlib.sha256(body).hexdigest():
        raise ValueError("Existing evidence changed during verification")
    return body.decode("utf-8")


def read_metadata(release):
    if not re.fullmatch(r"v\d+\.\d+\.\d+", release["tag_name"]) or asset_for(release, "update.json") is None:
        return None
    return json.loads(read_asset(release, "update.json", 16 * 1024))


def verified_assets(release, current, local):
    names = [current["apkName"], "SHA256SUMS", "apk-provenance.txt", "manifest.txt"]
    present = {name for name in names if asset_for(release, name) is not None}
    if current["apkName"] in present:
        apk = asset_for(release, current["apkName"])
        if apk["size"] != current["size"] or apk.get("digest") != "sha256:" + current["sha256"]:
            raise ValueError("Existing APK is not the verified candidate; refusing replacement")
    manifest = (local / "manifest.txt").read_text()
    provenance = (local / "apk-provenance.txt").read_text()
    if "manifest.txt" in present:
        manifest = read_asset(release, "manifest.txt", 64 * 1024)
    if "apk-provenance.txt" in present:
        provenance = read_asset(release, "apk-provenance.txt", 64 * 1024)
    code, version = metadata_module.identity(manifest, provenance, release["tag_name"])
    if (code, version) != (current["versionCode"], current["versionName"]):
        raise ValueError("Existing evidence has a different version")
    if "SHA256SUMS" in present:
        checksum = read_asset(release, "SHA256SUMS", 1024).split()
        if checksum != [current["sha256"], current["apkName"]]:
            raise ValueError("Existing checksum does not match the APK")
    return present


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
    recover_starters(release, [current["apkName"], "SHA256SUMS", "apk-provenance.txt", "manifest.txt", "update.json"])
    existing = read_metadata(release)
    if existing:
        for key in ["versionCode", "versionName", "apkName", "sha256", "size"]:
            if current[key] != existing[key]:
                raise ValueError("Refusing to replace an advertised APK with different bytes or identity")
    present = verified_assets(release, current, args.assets)
    names = [current["apkName"], "SHA256SUMS", "apk-provenance.txt", "manifest.txt"]
    if existing and all(name in present for name in names):
        return  # Complete matching publication is immutable and already ready.
    if not existing:
        releases = json.loads(gh("api", f"repos/{REPOSITORY}/releases?per_page=100"))
        previous = sorted((r for r in releases if r["tag_name"] != args.tag and
                           not r["draft"] and not r["prerelease"] and r.get("published_at") and
                           r["published_at"] < release["published_at"] and
                           re.fullmatch(r"v\d+\.\d+\.\d+", r["tag_name"])),
                          key=lambda r: r["published_at"], reverse=True)
        if previous:
            metadata = read_metadata(previous[0])
            if not metadata:
                raise ValueError("Newest preceding stable release lacks version evidence")
            check_progression(current, metadata)
    missing = [str(args.assets / name) for name in names if name not in present]
    if missing:
        gh("release", "upload", args.tag, "--repo", REPOSITORY, *missing)
    if not existing:
        gh("release", "upload", args.tag, "--repo", REPOSITORY, str(args.assets / "update.json"))



if __name__ == "__main__":
    main()
