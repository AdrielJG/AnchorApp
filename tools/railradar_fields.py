"""
Prints the exact key names RailRadar returns, at every level of the response.

The Android parser reads each field from a list of candidate spellings, so it
survives most naming differences. Run this if something on the live screen shows
"—" and the key it actually uses will be in this output.

    pip install requests
    python railradar_fields.py 95316
"""
import json
import os
import sys

import requests

API_KEY = os.environ.get("RAILRADAR_API_KEY", "rg_557ceeec8874410391e0af871b3c1a02")
TRAIN_NO = sys.argv[1] if len(sys.argv) > 1 else "95316"

resp = requests.get(
    f"https://api.railradar.in/v1/trains/{TRAIN_NO}/live",
    headers={"Authorization": f"Bearer {API_KEY}"},
    timeout=15,
)
payload = resp.json()

if not payload.get("success", True):
    print("Request failed:", payload.get("error", payload))
    sys.exit(1)

data = payload.get("data", payload)

print(f"HTTP {resp.status_code}")
print("top-level keys :", sorted(payload.keys()))
print("data keys      :", sorted(data.keys()))
print()

for section in ("currentLocation", "previousHalt", "nextHalt", "source", "destination"):
    value = data.get(section)
    if isinstance(value, dict):
        print(f"{section:16} keys: {sorted(value.keys())}")
    else:
        print(f"{section:16} {value!r}")

route = data.get("route") or data.get("stations") or data.get("stops") or []
print(f"\nroute: {len(route)} stops")
if route:
    print("stop keys      :", sorted(route[0].keys()))
    print("\nfirst three stops verbatim:")
    print(json.dumps(route[:3], indent=2)[:2000])

# The question the whole screen hinges on: observed position, or replayed schedule?
print("\nisLive          :", data.get("isLive"))
print("trackingMode    :", data.get("trackingMode"))
print("isActualPosition:", (data.get("currentLocation") or {}).get("isActualPosition"))
