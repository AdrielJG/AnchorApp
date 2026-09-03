"""
Rebuilds the two bundled timetable resources from a fresh pipeline export.

Run this whenever phase2 is regenerated. It exists because the rail export is
rail-only: dropping it straight into res/raw would delete every metro and bus
service from the app.

    python tools/merge_rail_data.py rail_export.csv stop_times_export.csv

Writes app/src/main/res/raw/phase2_unified_enriched.csv (rail from the export,
metro and bus carried over from the current file) and phase2_stop_times.csv.
"""
import sys
from pathlib import Path

import pandas as pd

RAW = Path(__file__).resolve().parents[1] / "app/src/main/res/raw"
ENRICHED = RAW / "phase2_unified_enriched.csv"

rail_path, stops_path = sys.argv[1], sys.argv[2]

current = pd.read_csv(ENRICHED, dtype=str, keep_default_na=False)
rail = pd.read_csv(rail_path, dtype=str, keep_default_na=False)

for column in ("train_no", "train_code"):
    if column not in rail.columns:
        raise SystemExit(f"rail export is missing the {column} column")

# Keep every non-rail service; the export only covers CR and WR.
carried = current[~current["mode"].isin(["CR_Train", "WR_Train"])].copy()
for column in ("train_no", "train_code"):
    carried[column] = ""

merged = pd.concat([rail, carried[rail.columns]], ignore_index=True)
merged.to_csv(ENRICHED, index=False)
pd.read_csv(stops_path, dtype=str, keep_default_na=False).to_csv(
    RAW / "phase2_stop_times.csv", index=False
)

print(f"wrote {len(merged)} services")
print(merged.groupby("mode").size().to_string())

# The engine drops 00:00 as a null sentinel; report how many the export contains.
stops = pd.read_csv(stops_path, dtype=str).set_index("service_id")
sentinels = (stops == "00:00").sum().sum()
if sentinels:
    print(f"\nnote: {sentinels} cells are 00:00, treated as 'does not stop here'")
