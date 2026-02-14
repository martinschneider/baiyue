#!/usr/bin/env python3
"""Convert data.yml to mountains.json for the mobile app."""

import json
import sys
import yaml
from yaml.loader import SafeLoader


def convert(yaml_path: str, json_path: str) -> None:
    with open(yaml_path) as f:
        data = yaml.load(f, Loader=SafeLoader)

    mountains = []
    for x in data:
        is_baiyue = x["type"] == "百岳"

        if is_baiyue:
            mountain_type = "BAIYUE"
        elif x.get("id-2017"):
            mountain_type = "XIAOBAIYUE"
        else:
            mountain_type = "XIAOBAIYUE_OLD"

        coords = str(x["location"]).split(",")

        def int_or_none(val):
            """Convert to int if truthy, else None."""
            return int(val) if val else None

        # For xiaobaiyue, strip leading zeros from the id (e.g. "006a" -> "6a", "001" -> "1")
        xiao_id = None
        if not is_baiyue:
            raw_id = str(x.get("id", ""))
            if raw_id:
                xiao_id = raw_id.lstrip("0") or "0"

        mountain = {
            "osmId": x["OSM"],
            "type": mountain_type,
            "id": x.get("id") if is_baiyue else None,
            "xiaobaiyueId": xiao_id,
            "chinese": x["chinese"],
            "english": x["english"],
            "lat": float(coords[0]),
            "lng": float(coords[1]),
            "elevation": float(x["elevation"]),
            "region": x["region"],
            "descriptions": parse_descriptions(x.get("descriptions")),
        }
        mountains.append(mountain)

    with open(json_path, "w", encoding="utf-8") as f:
        json.dump(mountains, f, ensure_ascii=False, indent=2)

    print(f"Converted {len(mountains)} mountains to {json_path}")
    baiyue = sum(1 for m in mountains if m["type"] == "BAIYUE")
    xiao = sum(1 for m in mountains if m["type"] == "XIAOBAIYUE")
    old = sum(1 for m in mountains if m["type"] == "XIAOBAIYUE_OLD")
    print(f"  百岳: {baiyue}, 小百岳: {xiao}, 舊小百岳: {old}")


def parse_descriptions(descriptions_str):
    """Parse description string matching the web app's JS logic.

    Format: "Name1: URL1,Name2: URL2,..."
    The first colon in each entry separates name from URL.
    URLs contain colons (https://) but we only split on the first one.

    Commas inside URLs can cause false splits, so we reassemble
    fragments that don't contain a colon back onto the previous entry.
    """
    if not descriptions_str:
        return []
    result = []
    parts = descriptions_str.split(",")
    i = 0
    while i < len(parts):
        part = parts[i].strip()
        if not part:
            i += 1
            continue
        if ":" not in part:
            # This fragment is part of the previous URL (comma in URL)
            if result:
                result[-1]["url"] += "," + part
            i += 1
            continue
        idx = part.index(":")
        name = part[:idx].strip()
        link = part[idx + 1:].strip()
        # Check if this looks like a URL continuation (name is a URL scheme)
        if name in ("https", "http") and result:
            result[-1]["url"] += "," + part
        else:
            result.append({"name": name, "url": link})
        i += 1
    return result


if __name__ == "__main__":
    yaml_path = sys.argv[1] if len(sys.argv) > 1 else "../../data.yml"
    json_path = sys.argv[2] if len(sys.argv) > 2 else "../shared/src/commonMain/resources/mountains.json"
    convert(yaml_path, json_path)
