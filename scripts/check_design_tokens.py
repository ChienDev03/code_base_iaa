#!/usr/bin/env python3
"""Cheap base UI policy checks; compilation/device checks remain separate."""
from pathlib import Path
import re
root = Path(__file__).resolve().parents[1]
errors = []
for file in (root / "app/src/main/java").rglob("*.kt"):
    source = file.read_text()
    if "@Composable" not in source:
        continue
    if len(source.splitlines()) >= 500:
        errors.append(str(file) + ": UI file limit")
    for pattern, label in [(r"LocalAppColors|AppSizes\.", "legacy tokens"), (r"\b(?:Button|OutlinedTextField|TextField|AssistChip|FilterChip|InputChip)\(", "unadapted Material component"), (r"\.(?:size|height|width)\(\d+\.dp", "inline geometry")]:
        if re.search(pattern, source): errors.append(str(file) + ": " + label)
    if file.name != "Color.kt" and "Color(0x" in source: errors.append(str(file) + ": raw color")
    if "LazyColumn(" in source and "overscrollEffect = null" not in source: errors.append(str(file) + ": overscroll")
for error in errors: print(error)
raise SystemExit(bool(errors))
