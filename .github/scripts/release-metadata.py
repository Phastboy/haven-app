#!/usr/bin/env python3
"""Build public download metadata from APK identity/provenance, without version pins."""
import argparse
import hashlib
import json
import pathlib
import re
import shutil

PACKAGE = "com.aistudio.haven.xvykpt"
SIGNER = "b4d1a5ec0068c63d5549e6223d86d90dc0377dc56bc72d34fa63f938bd2db61e"


def identity(manifest, provenance, tag=None):
    match = re.search(r"^package: name='([^']+)' versionCode='(\d+)' versionName='([^']+)'", manifest, re.M)
    if not match or match[1] != PACKAGE or "application-debuggable" in manifest:
        raise ValueError("Wrong APK package or debuggable build")
    code, version = int(match[2]), match[3]
    if code <= 0 or not re.fullmatch(r"\d+\.\d+\.\d+", version):
        raise ValueError("Invalid release version")
    if tag is not None and tag != f"v{version}":
        raise ValueError("Release tag must match APK versionName")
    signers = re.findall(r"certificate SHA-256 digest:\s*([a-fA-F0-9]+)", provenance)
    if not signers or any(s.lower() != SIGNER for s in signers):
        raise ValueError("Release signing identity changed")
    return code, version


def generate(apk, manifest, provenance, output, source_sha):
    code, version = identity(manifest, provenance)
    if not re.fullmatch(r"[a-f0-9]{40}", source_sha):
        raise ValueError("Missing source identity")
    size = apk.stat().st_size
    if not 0 < size <= 150 * 1024 * 1024:
        raise ValueError("APK size outside supported download bounds")
    output.mkdir(parents=True, exist_ok=True)
    name = f"haven-v{version}.apk"
    target = output / name
    shutil.copyfile(apk, target)
    with target.open("rb") as stream:
        sha256 = hashlib.file_digest(stream, "sha256").hexdigest()
    metadata = dict(versionCode=code, versionName=version, apkName=name, size=size,
                    sha256=sha256, packageName=PACKAGE, signerSha256=SIGNER,
                    variant="release", sourceSha=source_sha)
    (output / "update.json").write_text(json.dumps(metadata) + "\n")
    (output / "SHA256SUMS").write_text(f"{sha256}  {name}\n")
    return metadata


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--apk", type=pathlib.Path, required=True)
    parser.add_argument("--output", type=pathlib.Path, required=True)
    parser.add_argument("--source-sha", required=True)
    args = parser.parse_args()
    generate(args.apk, (args.output / "manifest.txt").read_text(),
             (args.output / "apk-provenance.txt").read_text(), args.output, args.source_sha)


if __name__ == "__main__":
    main()
