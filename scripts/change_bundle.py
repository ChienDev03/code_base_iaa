#!/usr/bin/env python3
"""Rename the Android bundle: namespace, applicationId, package folders, and references.

Usage:
    python3 scripts/change_bundle.py --new-package com.your.app [--app-name "Your App"] [--dry-run]

The current package is auto-detected from app/build.gradle.kts (`namespace = "..."`).
Operations:
  * Rewrite `namespace`/`applicationId` in app/build.gradle.kts.
  * Move Kotlin sources from <old>/ to <new>/ under app/src/{main,test,androidTest}/java.
  * Replace package / import references across .kt, .kts, .xml, .pro, .md, .sh
    (both dotted form `com.old.pkg` and path form `com/old/pkg`).
  * Optionally replace the default `app_name` fallback inside app/build.gradle.kts
    and sync `rootProject.name` in settings.gradle.kts with a slug of that app name.
"""
from __future__ import annotations
import argparse
import re
import shutil
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP_GRADLE = ROOT / "app/build.gradle.kts"
SETTINGS_GRADLE = ROOT / "settings.gradle.kts"
SOURCE_ROOTS = [ROOT / "app/src/main/java", ROOT / "app/src/test/java", ROOT / "app/src/androidTest/java"]
SCAN_SUFFIXES = {".kt", ".kts", ".xml", ".pro", ".md", ".sh"}
EXCLUDED_DIRS = {"build", ".gradle", ".idea", "node_modules", ".git"}
PACKAGE_RE = re.compile(r"^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$")


def slugify(name: str) -> str:
    slug = re.sub(r"[^a-z0-9]+", "-", name.strip().lower()).strip("-")
    return slug or "app"


def detect_current_package() -> str:
    text = APP_GRADLE.read_text()
    match = re.search(r'namespace\s*=\s*"([^"]+)"', text)
    if not match:
        sys.exit("Could not detect current namespace in app/build.gradle.kts")
    return match.group(1)


def scan_files() -> list[Path]:
    files: list[Path] = []
    for path in ROOT.rglob("*"):
        if not path.is_file() or path.suffix not in SCAN_SUFFIXES:
            continue
        if set(path.parts) & EXCLUDED_DIRS:
            continue
        files.append(path)
    return files


def plan_folder_moves(old_pkg: str, new_pkg: str) -> list[tuple[Path, Path]]:
    old_rel = Path(*old_pkg.split("."))
    new_rel = Path(*new_pkg.split("."))
    moves: list[tuple[Path, Path]] = []
    for base in SOURCE_ROOTS:
        src = base / old_rel
        if src.is_dir():
            moves.append((src, base / new_rel))
    return moves


def apply_moves(moves: list[tuple[Path, Path]], dry_run: bool) -> None:
    for src, dst in moves:
        print(f"[move] {src.relative_to(ROOT)} -> {dst.relative_to(ROOT)}")
        if dry_run:
            continue
        dst.parent.mkdir(parents=True, exist_ok=True)
        if dst.exists():
            sys.exit(f"Destination already exists: {dst}")
        shutil.move(str(src), str(dst))
        # Clean up empty parent directories left behind.
        parent = src.parent
        while parent != ROOT and parent.is_dir() and not any(parent.iterdir()):
            parent.rmdir()
            parent = parent.parent


def rewrite_contents(old_pkg: str, new_pkg: str, new_app_name: str | None, dry_run: bool) -> int:
    dotted_pattern = re.compile(r"\b" + re.escape(old_pkg) + r"\b")
    old_path = old_pkg.replace(".", "/")
    new_path = new_pkg.replace(".", "/")
    path_pattern = re.compile(r"(?<![A-Za-z0-9_/])" + re.escape(old_path) + r"(?![A-Za-z0-9_])")
    root_slug = slugify(new_app_name) if new_app_name else None
    changed = 0
    for file in scan_files():
        try:
            source = file.read_text()
        except UnicodeDecodeError:
            continue
        updated = dotted_pattern.sub(new_pkg, source)
        updated = path_pattern.sub(new_path, updated)
        if file == APP_GRADLE and new_app_name is not None:
            updated = re.sub(
                r'(resValue\("string", "app_name", config\.value\("APP_NAME", )(.+?)(\)\))',
                lambda m: m.group(1) + f'"{new_app_name}"' + m.group(3),
                updated,
            )
        if file == SETTINGS_GRADLE and root_slug is not None:
            updated = re.sub(
                r'(rootProject\.name\s*=\s*)"[^"]*"',
                lambda m: m.group(1) + f'"{root_slug}"',
                updated,
            )
        if updated != source:
            changed += 1
            print(f"[edit] {file.relative_to(ROOT)}")
            if not dry_run:
                file.write_text(updated)
    return changed


def main() -> None:
    parser = argparse.ArgumentParser(description="Rename Android application bundle in-place.")
    parser.add_argument("--new-package", required=True, help="New package, e.g. com.your.app")
    parser.add_argument("--app-name", help="Replace default APP_NAME fallback inside app/build.gradle.kts")
    parser.add_argument("--dry-run", action="store_true", help="Show planned changes without writing")
    args = parser.parse_args()

    new_pkg = args.new_package.strip()
    if not PACKAGE_RE.match(new_pkg):
        sys.exit(f"Invalid package name: {new_pkg!r} (expect dot-separated lowercase segments)")

    old_pkg = detect_current_package()
    if old_pkg == new_pkg:
        sys.exit(f"New package equals current ({old_pkg}); nothing to do.")

    print(f"Current package: {old_pkg}")
    print(f"New package:     {new_pkg}")
    if args.app_name:
        print(f"New app_name:    {args.app_name}")
        print(f"New root name:   {slugify(args.app_name)}")
    if args.dry_run:
        print("(dry-run: no files will be modified)")

    moves = plan_folder_moves(old_pkg, new_pkg)
    apply_moves(moves, args.dry_run)
    changed = rewrite_contents(old_pkg, new_pkg, args.app_name, args.dry_run)

    print(f"\nDone. {changed} file(s) updated, {len(moves)} folder move(s).")
    if not args.dry_run:
        print("Next: ./gradlew clean && ./gradlew :app:assembleDevDebug to verify.")


if __name__ == "__main__":
    main()
