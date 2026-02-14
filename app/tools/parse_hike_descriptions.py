#!/usr/bin/env python3
"""Parse LaTeX hike descriptions into JSON for the mobile app."""

import json
import math
import os
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path


def strip_latex(text, keep_wpt=False):
    """Strip LaTeX commands from text.

    If keep_wpt is True, convert \\wpt{N} to {{N}} placeholder tokens.
    If keep_wpt is False, remove \\wpt{N} entirely.
    """
    # \numprint{N} -> N
    text = re.sub(r"\\numprint\{(\d+)\}", r"\1", text)
    # \wpt{N} handling
    if keep_wpt:
        text = re.sub(r"\\wpt\{(\d+)\}", r"{{\1}}", text)
    else:
        text = re.sub(r"\\wpt\{(\d+)\}\s*", "", text)
    # LaTeX backtick quote -> apostrophe
    text = text.replace("`", "'")
    # LaTeX em dash
    text = text.replace("---", "\u2014")
    text = text.replace("--", "\u2013")
    return text.strip()


def parse_waypoints(filepath):
    """Parse a _waypoints.tex file into start, end, waypoints, and peaks."""
    if not filepath.exists():
        return None, None, [], []

    content = filepath.read_text(encoding="utf-8")
    start = None
    end = None
    waypoints = []
    peaks = []

    for match in re.finditer(
        r"\\caption\*\{\\centering\s+\\textbf\{(.+?)\}\s*\\\\\s*([\d.]+),\s*([\d.]+)\}",
        content,
    ):
        label = match.group(1)
        lat = float(match.group(2))
        lng = float(match.group(3))

        if label == "Start":
            start = {"name": "Start", "lat": lat, "lng": lng}
        elif label == "End":
            end = {"name": "End", "lat": lat, "lng": lng}
        elif label.startswith("\\wpt{"):
            # \wpt{N}: Name
            wpt_match = re.match(r"\\wpt\{(\d+)\}:\s*(.+)", label)
            if wpt_match:
                waypoints.append(
                    {
                        "number": int(wpt_match.group(1)),
                        "name": wpt_match.group(2).strip(),
                        "lat": lat,
                        "lng": lng,
                    }
                )
        elif label.startswith("\\texttwemoji{mountain}"):
            name = label.replace("\\texttwemoji{mountain}", "").strip()
            peaks.append({"name": name, "lat": lat, "lng": lng})

    return start, end, waypoints, peaks


def extract_ids(filename):
    """Extract xiaobaiyue IDs from filename.

    Examples:
        001_002_Datunshan_Qixingshan_intro.tex -> ["1", "2"]
        003_Dawulunshan_intro.tex -> ["3"]
        006a_Danfengshan_intro.tex -> ["6a"]
        086b_Zulunshan_intro.tex -> ["86b"]
    """
    # Match leading ID segments (digits optionally followed by a letter)
    ids = []
    parts = filename.split("_")
    for part in parts:
        if re.match(r"^\d{3}[a-z]?$", part):
            # Strip leading zeros: "001" -> "1", "006a" -> "6a"
            stripped = part.lstrip("0") or "0"
            ids.append(stripped)
        else:
            break
    return ids


def find_hike_groups(text_dir):
    """Find all unique hike groups based on _intro.tex files."""
    groups = {}
    for intro_file in sorted(text_dir.glob("*_intro.tex")):
        filename = intro_file.name
        # Get the base prefix (everything before _intro.tex)
        base = filename.replace("_intro.tex", "")
        ids = extract_ids(filename)
        if ids:
            groups[base] = ids
    return groups


def parse_gpx_track(gpx_path, target_points=200):
    """Parse a GPX file and return decimated track points as [[lat, lng, ele], ...]."""
    try:
        tree = ET.parse(gpx_path)
    except ET.ParseError:
        return []

    root = tree.getroot()
    ns = {"gpx": "http://www.topografix.com/GPX/1/1"}

    points = []
    for trkpt in root.findall(".//gpx:trkpt", ns):
        lat = trkpt.get("lat")
        lon = trkpt.get("lon")
        if lat and lon:
            ele_elem = trkpt.find("gpx:ele", ns)
            ele = round(float(ele_elem.text), 1) if ele_elem is not None and ele_elem.text else 0.0
            points.append([round(float(lat), 6), round(float(lon), 6), ele])

    if len(points) <= target_points:
        return points

    # Decimate: keep every Nth point, always include first and last
    step = max(1, math.ceil(len(points) / target_points))
    decimated = points[::step]
    if decimated[-1] != points[-1]:
        decimated.append(points[-1])
    return decimated


def parse_stats(stats_path):
    """Parse a _stats.tex file and return the stats string."""
    if not stats_path.exists():
        return None
    return stats_path.read_text(encoding="utf-8").strip()


def parse_hike_descriptions(text_dir, output_path, gpx_dir=None):
    """Parse all hike descriptions and write JSON."""
    text_dir = Path(text_dir)
    if gpx_dir:
        gpx_dir = Path(gpx_dir)
    groups = find_hike_groups(text_dir)

    results = []
    for base, ids in sorted(groups.items()):
        intro_file = text_dir / f"{base}_intro.tex"
        route_file = text_dir / f"{base}_route.tex"
        transport_file = text_dir / f"{base}_transport.tex"
        waypoints_file = text_dir / f"{base}_waypoints.tex"

        intro = ""
        if intro_file.exists():
            intro = strip_latex(intro_file.read_text(encoding="utf-8"), keep_wpt=False)

        route = ""
        if route_file.exists():
            route = strip_latex(route_file.read_text(encoding="utf-8"), keep_wpt=True)

        transport = ""
        if transport_file.exists():
            transport = strip_latex(
                transport_file.read_text(encoding="utf-8"), keep_wpt=False
            )

        start, end, waypoints, peaks = parse_waypoints(waypoints_file)

        # GPX track and stats
        stats = None
        track = []
        gpx_filename = None

        stats_file = text_dir / f"{base}_stats.tex"
        stats = parse_stats(stats_file)

        if gpx_dir:
            gpx_file = gpx_dir / f"{base}.gpx"
            if gpx_file.exists():
                track = parse_gpx_track(gpx_file)
                gpx_filename = gpx_file.name

        entry = {
            "xiaobaiyueIds": ids,
            "intro": intro,
            "route": route,
            "transport": transport,
        }
        if start:
            entry["start"] = start
        if end:
            entry["end"] = end
        if waypoints:
            entry["waypoints"] = waypoints
        if peaks:
            entry["peaks"] = peaks
        if stats:
            entry["stats"] = stats
        if track:
            entry["track"] = track
        if gpx_filename:
            entry["gpxFilename"] = gpx_filename

        results.append(entry)

    output_path = Path(output_path)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    with open(output_path, "w", encoding="utf-8") as f:
        json.dump(results, f, ensure_ascii=False, indent=2)

    tracks_count = sum(1 for r in results if r.get("track"))
    print(f"Parsed {len(results)} hike descriptions to {output_path}")
    print(f"  Total xiaobaiyue IDs covered: {sum(len(r['xiaobaiyueIds']) for r in results)}")
    print(f"  Entries with GPX tracks: {tracks_count}")


if __name__ == "__main__":
    script_dir = Path(__file__).resolve().parent
    text_dir = sys.argv[1] if len(sys.argv) > 1 else str(script_dir / "../../../xiaobaiyuebook/text")
    gpx_dir = sys.argv[3] if len(sys.argv) > 3 else str(script_dir / "../../../xiaobaiyuebook/gpx")
    output_path = (
        sys.argv[2]
        if len(sys.argv) > 2
        else "../shared/src/commonMain/resources/hike_descriptions.json"
    )
    parse_hike_descriptions(text_dir, output_path, gpx_dir)
