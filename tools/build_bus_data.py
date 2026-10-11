#!/usr/bin/env python3
"""
Turns the three uploaded bus CSVs into three compact files the Anchor app can
read from res/raw.

The uploaded route_stops file has no sequence column, so stop order along each
route has to be reconstructed. Three things do that work, in order of how much
they matter:

  1. Pinning the terminals. Each route declares `from` and `to`. Matching those
     to actual stops on the route fixes both ends of the chain, which is what
     stops a greedy walk from running past its own terminus and collecting
     stragglers on the way out.
  2. Nearest-neighbour construction, seeded at the pinned start.
  3. 2-opt and Or-opt refinement, which shortens the chain by undoing crossings
     and relocating stops the greedy pass left in the wrong place. Pinned
     terminals are held still throughout.

Each route carries a `seq_conf` of high, medium or low so the app can treat an
unverified ordering differently from a pinned one.

Outputs
  bus_stops.csv       stop_id,name,lat,lon
  bus_routes.csv      route_id,ref,name,operator,from,to,n_stops,km,seq_conf
  bus_route_stops.csv route_id,seq,stop_id,cum_km
"""

import math
import os
import re
import unicodedata

import pandas as pd

UP = "./source_csv"   # stops.csv, routes.csv, route_stops.csv as downloaded
OUT = "../app/src/main/res/raw"

R_EARTH = 6371.0
MERGE_KM = 0.25         # platform/stop pairs closer than this are one stop
TERMINAL_WEAK = 0.60    # minimum score to pin a terminal to a stop at all
TERMINAL_MARGIN = 0.20  # how far the best match must lead the runner-up
MIRROR_TOLERANCE = 1.15 # a borrowed order may be this much longer, no more
MAX_ROUTE_KM = 60.0     # above this it is an intercity coach, not a city route


# ── Geometry and text helpers ─────────────────────────────────────────────────

def hav(a, b):
    lat1, lon1 = a
    lat2, lon2 = b
    p1, p2 = math.radians(lat1), math.radians(lat2)
    x = (math.sin(math.radians(lat2 - lat1) / 2) ** 2
         + math.cos(p1) * math.cos(p2) * math.sin(math.radians(lon2 - lon1) / 2) ** 2)
    return R_EARTH * 2 * math.atan2(math.sqrt(x), math.sqrt(1 - x))


# Words that say nothing about which stop is meant, so they are ignored when
# matching a declared terminal to a stop name.
STOPWORDS = {"bus", "station", "stn", "stand", "depot", "terminus",
             "the", "of", "and"}


def norm(s):
    if not isinstance(s, str):
        return ""
    s = unicodedata.normalize("NFKD", s).encode("ascii", "ignore").decode()
    s = re.sub(r"[^a-z0-9]+", " ", s.lower().strip())
    return re.sub(r"\s+", " ", s).strip()


def tokens(s):
    t = set(norm(s).split())
    core = t - STOPWORDS
    return core if core else t


def name_sim(a, b):
    """
    Similarity of a stop name to a declared terminal, 0.0 to 1.0.

    Terminals are written loosely against the stop list — "Airoli Bus Station"
    for a stop called "Airoli" — so exact equality misses most of them. Core
    tokens carry most of the weight, but full-token overlap is folded in so that
    a terminal like "Kalwa Depot" scores strictly higher on the stop of that
    name than on its neighbour "Kalwa Naka", which shares only the core word.
    """
    na, nb = norm(a), norm(b)
    if not na or not nb:
        return 0.0
    if na == nb:
        return 1.0
    ta, tb = set(na.split()), set(nb.split())
    ca, cb = (ta - STOPWORDS) or ta, (tb - STOPWORDS) or tb
    core = len(ca & cb) / min(len(ca), len(cb))
    full = len(ta & tb) / len(ta | tb)
    return 0.75 * core + 0.25 * full


def op_family(op):
    """
    Collapse the operator spellings in the source to one label per undertaking.

    The file carries "Thane Muncipal Transport", "Thane Municipal Transport" and
    "TMT" for the same body, and route numbers repeat across undertakings — TMT
    56 runs in Thane while BEST 56 runs Vesava to Worli — so the operator has to
    be part of any route-number comparison.
    """
    o = norm(op)
    if "brihanmumbai" in o or o.startswith("best") or "b e s t" in o:
        return "BEST"
    if "thane" in o or o == "tmt":
        return "TMT"
    if "nmmt" in o or "navi mumbai" in o:
        return "NMMT"
    if "mira" in o or "bhayander" in o:
        return "MBMT"
    return o.upper()[:8] or "OTHER"


def clean(s):
    if not isinstance(s, str):
        return ""
    return s.replace(",", " ").replace('"', "").replace("\n", " ").strip()


# ── Identifiers ───────────────────────────────────────────────────────────────
# The source ids are strings scoped by undertaking, and not in one shape: most
# are PREFIX_number (BEST_11008), but Thane's carry the route number and the
# direction as well (TMT_1AC_1). The app reads both stop_id and route_id as Long.
#
# So each undertaking gets its own millions block and its ids are numbered inside
# it, in sorted order. Numbering the whole set in one pass is what keeps the
# result stable: it cannot drift with the order the rows happen to be visited.
# bus_id_map.csv carries the source id for every number issued.

OP_BLOCK = {"BEST": 1, "TMT": 2, "NMMT": 3, "KDMT": 4, "VVMT": 5, "MBMT": 6}
BLOCK = 1_000_000


class IdSpace:
    """Source id strings -> Longs, unique across undertakings."""

    def __init__(self):
        self._map = {}

    def assign(self, src_ids):
        ids = sorted({str(s).strip() for s in src_ids})
        prefixes = sorted({i.split("_")[0].upper() for i in ids})
        blocks = dict(OP_BLOCK)
        for p in prefixes:
            if p not in blocks:
                blocks[p] = 10 + len(blocks)
        grouped = {}
        for sid in ids:
            grouped.setdefault(blocks[sid.split("_")[0].upper()], []).append(sid)
        for block, members in grouped.items():
            if len(members) >= BLOCK:
                raise ValueError(f"block {block} overflows: {len(members)} ids")
            for i, sid in enumerate(members, start=1):
                self._map[sid] = block * BLOCK + i
        return self

    def __call__(self, src_id):
        return self._map[str(src_id).strip()]


STOP_IDS, ROUTE_IDS = IdSpace(), IdSpace()


# ── Chain construction ────────────────────────────────────────────────────────

def chain_len(order, pts):
    return sum(hav(pts[order[i - 1]], pts[order[i]]) for i in range(1, len(order)))


def nn_chain(ids, pts, start, end=None):
    """Greedy nearest-neighbour walk from `start`, holding `end` back until last."""
    order = [start]
    remaining = set(ids) - {start}
    if end is not None:
        remaining.discard(end)
    while remaining:
        cur = pts[order[-1]]
        nxt = min(remaining, key=lambda s: hav(cur, pts[s]))
        order.append(nxt)
        remaining.discard(nxt)
    if end is not None:
        order.append(end)
    return order


def two_opt(order, pts, fix_end):
    """
    Undo crossings by reversing internal segments.

    A greedy walk regularly doubles back on itself; reversing the offending
    segment is the cheapest way to straighten that out. The first stop, and the
    last when pinned, stay where they are so the route keeps its direction.
    """
    n = len(order)
    improved = True
    while improved:
        improved = False
        hi = n - 2 if fix_end else n - 1
        for i in range(1, hi + 1):
            for j in range(i, hi + 1):
                a, b, c = order[i - 1], order[i], order[j]
                d = order[j + 1] if j + 1 < n else None
                before = hav(pts[a], pts[b]) + (hav(pts[c], pts[d]) if d else 0.0)
                after = hav(pts[a], pts[c]) + (hav(pts[b], pts[d]) if d else 0.0)
                if after < before - 1e-9:
                    order[i:j + 1] = list(reversed(order[i:j + 1]))
                    improved = True
    return order


def or_opt(order, pts, fix_end, max_seg=3):
    """
    Relocate short runs of stops the greedy pass dropped in the wrong place.

    2-opt cannot move a stop past its neighbours without reversing everything in
    between, so a single stop stranded mid-chain needs this instead.

    Cost is scored as a delta — the three edges the move breaks against the three
    it makes — rather than by re-measuring the whole chain for every candidate
    position. On a 147-stop route the re-measuring version is hours of work; this
    one is a second. The block is tried both ways round, because a run of stops
    lifted out of one corridor often belongs in the other facing backwards.
    """
    def d(a, b):
        return hav(pts[a], pts[b]) if a is not None and b is not None else 0.0

    improved = True
    while improved:
        improved = False
        n = len(order)
        last = n - 1 if fix_end else n
        for seg in range(1, max_seg + 1):
            i = 1
            while i + seg <= last:
                block = order[i:i + seg]
                head, tail = block[0], block[-1]
                p = order[i - 1]
                nxt = order[i + seg] if i + seg < n else None
                # What taking the block out gives back.
                gain = d(p, head) + d(tail, nxt) - d(p, nxt)
                if gain <= 1e-9:
                    i += 1
                    continue

                rest = order[:i] + order[i + seg:]
                hi_k = len(rest) - (1 if fix_end else 0)
                best_cost, best_k, best_rev = None, None, False
                for k in range(1, hi_k + 1):
                    if k == i:
                        continue
                    u = rest[k - 1]
                    v = rest[k] if k < len(rest) else None
                    base = d(u, v)
                    fwd = d(u, head) + d(tail, v) - base
                    rev = d(u, tail) + d(head, v) - base
                    cost, is_rev = (rev, True) if rev < fwd else (fwd, False)
                    if best_cost is None or cost < best_cost:
                        best_cost, best_k, best_rev = cost, k, is_rev

                if best_cost is not None and best_cost < gain - 1e-9:
                    b = list(reversed(block)) if best_rev else block
                    order = rest[:best_k] + b + rest[best_k:]
                    improved = True
                    n = len(order)
                    last = n - 1 if fix_end else n
                else:
                    i += 1
    return order


def extreme_seeds(ids, pts, k=4):
    """Endpoints of the longest span, plus the stops furthest from the centroid."""
    if len(ids) <= 2:
        return list(ids)
    lat = sum(pts[s][0] for s in ids) / len(ids)
    lon = sum(pts[s][1] for s in ids) / len(ids)
    far = sorted(ids, key=lambda s: -hav((lat, lon), pts[s]))[:k]
    best, pair = -1.0, (ids[0], ids[-1])
    for i in range(len(ids)):
        for j in range(i + 1, len(ids)):
            d = hav(pts[ids[i]], pts[ids[j]])
            if d > best:
                best, pair = d, (ids[i], ids[j])
    return list(dict.fromkeys([pair[0], pair[1], *far]))


def refine(ids, pts, start, end):
    order = nn_chain(ids, pts, start, end)
    order = two_opt(order, pts, fix_end=end is not None)
    order = or_opt(order, pts, fix_end=end is not None)
    return order


def build_order(ids, pts, names, term_from, term_to):
    """
    Best stop order for one route, with a confidence label.

    A terminal that matches a stop name outright pins that end and is trusted.
    A terminal that only matches loosely still pins the end — it is the best
    evidence available — but the route is not allowed to claim high confidence
    on it, because a loose match is exactly how "Kalwa Depot" ends up on the
    stop called "Kalwa Naka".
    """

    def match(term):
        """
        Resolve a declared terminal to a stop on this route.

        A match is only trusted when it clearly leads the runner-up. That is what
        separates "Siddhachal" landing on "Siddhachal Society", where nothing
        else comes close, from "Kalwa Depot" landing on "Kalwa Naka" while the
        actual Kalwa Depot stop sits right beside it scoring just as well.
        """
        if not term:
            return None, False
        scored = sorted(((name_sim(names[s], term), s) for s in ids), reverse=True)
        best, sid = scored[0]
        if best < TERMINAL_WEAK:
            return None, False
        runner = scored[1][0] if len(scored) > 1 else 0.0
        return sid, (best >= 1.0 or best - runner >= TERMINAL_MARGIN)

    start, s_sure = match(term_from)
    end, e_sure = match(term_to)
    if start is not None and start == end:
        end, e_sure = None, False

    if start is not None and end is not None:
        return refine(ids, pts, start, end), ("high" if s_sure and e_sure else "medium")

    if start is not None or end is not None:
        flip = start is None
        anchor = start if start is not None else end
        order = refine(ids, pts, anchor, None)
        if flip:
            order.reverse()
        return order, "medium"

    # Neither terminal resolves to a stop on this route. Try several seeds, keep
    # the shortest chain, then orient it on whatever name evidence exists.
    best_order, best_len = None, None
    for seed in extreme_seeds(ids, pts):
        order = refine(ids, pts, seed, None)
        L = chain_len(order, pts)
        if best_len is None or L < best_len:
            best_order, best_len = order, L
    order = best_order
    if term_from or term_to:
        fwd = name_sim(names[order[0]], term_from) + name_sim(names[order[-1]], term_to)
        rev = name_sim(names[order[-1]], term_from) + name_sim(names[order[0]], term_to)
        if rev > fwd:
            order.reverse()
    return order, "low"


def mirror_order(weak_ids, strong_order, pts, max_km=0.3, min_cover=0.6):
    """
    Order one direction of a route from the opposite direction's known order.

    The single biggest gap in the source is that a route's return leg often
    omits the terminal stop the outbound leg has, which leaves nothing to pin.
    But the two directions run the same corridor, so a confident outbound chain
    reversed is a far better answer than a greedy walk. Each return-leg stop
    takes the position of its nearest outbound counterpart; anything with no
    counterpart is slotted in wherever it shortens the chain least.

    Returns None when the two directions do not overlap enough to trust.
    """
    pos = {sid: i for i, sid in enumerate(strong_order)}
    placed, orphans = [], []
    for s in weak_ids:
        near = min(strong_order, key=lambda t: hav(pts[s], pts[t]))
        if hav(pts[s], pts[near]) <= max_km:
            placed.append((pos[near], s))
        else:
            orphans.append(s)

    if not placed or len(placed) / len(weak_ids) < min_cover:
        return None

    placed.sort(key=lambda x: -x[0])          # opposite direction, so reversed
    order = [s for _, s in placed]

    for s in orphans:
        best_i, best_cost = 0, None
        for i in range(len(order) + 1):
            cost = chain_len(order[:i] + [s] + order[i:], pts)
            if best_cost is None or cost < best_cost:
                best_cost, best_i = cost, i
        order = order[:best_i] + [s] + order[best_i:]

    return order


# ── Load ──────────────────────────────────────────────────────────────────────

stops = pd.read_csv(f"{UP}/stops.csv")
routes = pd.read_csv(f"{UP}/routes.csv")
rstops = pd.read_csv(f"{UP}/route_stops.csv")

# A handful of source ids carry trailing spaces, which would otherwise make one
# stop look like two and break the join between the three files.
stops["stop_id"] = stops["stop_id"].astype(str).str.strip()
routes["route_id"] = routes["route_id"].astype(str).str.strip()
rstops["stop_id"] = rstops["stop_id"].astype(str).str.strip()
rstops["route_id"] = rstops["route_id"].astype(str).str.strip()

stops = stops[stops["name"].notna()].copy()
stops["key"] = stops["name"].map(norm)

# ── Collapse platform/stop twins ──────────────────────────────────────────────
# The source lists each physical stop twice, once as role=platform (roadside)
# and once as role=stop (a point on the way). Same stop, two ids.

reps, rep_rows = {}, {}
for key, grp in stops.groupby("key"):
    clusters = []
    for _, row in grp.iterrows():
        pt = (row.lat, row.lon)
        for c in clusters:
            if hav(pt, (c[1], c[2])) <= MERGE_KM:
                c[3].append(row.stop_id)
                break
        else:
            clusters.append([row.stop_id, row.lat, row.lon, [row.stop_id]])
    for rep_id, lat, lon, members in clusters:
        rep_rows[rep_id] = {"stop_id": rep_id, "name": grp.iloc[0]["name"],
                            "lat": lat, "lon": lon}
        for m in members:
            reps[m] = rep_id

print(f"stops: {len(stops)} raw -> {len(rep_rows)} after merging platform/stop twins")

# ── Pass 1: reconstruct every route on its own evidence ──────────────────────

PTS = {i: (r["lat"], r["lon"]) for i, r in rep_rows.items()}
NAMES = {i: r["name"] for i, r in rep_rows.items()}

route_meta = routes.set_index("route_id").to_dict("index")
built, skipped = {}, []

for route_id, grp in rstops.groupby("route_id"):
    meta = route_meta.get(route_id)
    if meta is None:
        skipped.append((route_id, "route_id absent from routes.csv"))
        continue

    ids, seen = [], set()
    for sid in grp.stop_id:
        rep = reps.get(sid)
        if rep is not None and rep not in seen:
            seen.add(rep)
            ids.append(rep)
    if len(ids) < 2:
        skipped.append((route_id, f"only {len(ids)} usable stop(s)"))
        continue

    term_from = meta.get("from") if isinstance(meta.get("from"), str) else ""
    term_to = meta.get("to") if isinstance(meta.get("to"), str) else ""

    # Greedy-only baseline, kept solely to report what the refinement bought.
    base_len = chain_len(nn_chain(ids, PTS, extreme_seeds(ids, PTS)[0]), PTS)
    order, conf = build_order(ids, PTS, NAMES, term_from, term_to)

    built[route_id] = {"ids": ids, "order": order, "conf": conf, "meta": meta,
                       "from": term_from, "to": term_to, "base_len": base_len,
                       "via": "own"}

# ── Pass 2: borrow order from the opposite direction ─────────────────────────
# Many return legs omit the terminal their outbound leg carries, so they cannot
# pin and fall back to a greedy walk. Where the outbound direction is confident,
# its reversed order is better evidence than anything the return leg has alone.
#
# Route numbers are not unique across undertakings, so the operator is part of
# the match. mirror_order's own coverage test is the second guard: two routes
# that do not share a corridor cannot reach the 60% overlap it demands.

by_key = {}
for rid, b in built.items():
    key = (norm(b["meta"].get("ref")), op_family(b["meta"].get("operator")))
    by_key.setdefault(key, []).append(rid)

def borrow(source_conf, award):
    """One sweep: order weak routes from a partner at `source_conf`."""
    n = 0
    for rid, b in built.items():
        if b["conf"] == "high" or b["via"] != "own":
            continue
        # A chain that already begins and ends on its own declared terminals has
        # better evidence than a borrowed one. Only routes missing a terminal —
        # the case the mirror exists for — are eligible.
        own = b["order"]
        if (name_sim(NAMES[own[0]], b["from"]) >= TERMINAL_WEAK
                and name_sim(NAMES[own[-1]], b["to"]) >= TERMINAL_WEAK):
            continue
        key = (norm(b["meta"].get("ref")), op_family(b["meta"].get("operator")))
        partners = [
            q for q in by_key.get(key, [])
            if q != rid and built[q]["conf"] == source_conf and built[q]["via"] != f"mirrored from {rid}"
            and name_sim(built[q]["from"], b["to"]) >= TERMINAL_WEAK
            and name_sim(built[q]["to"], b["from"]) >= TERMINAL_WEAK
        ]
        if not partners:
            continue
        best = max(partners, key=lambda q: len(built[q]["order"]))
        order = mirror_order(b["ids"], built[best]["order"], PTS)
        if order is None:
            continue
        # A borrowed order that wanders is worse than the route's own evidence.
        if chain_len(order, PTS) > chain_len(b["order"], PTS) * MIRROR_TOLERANCE:
            continue
        b["order"], b["conf"], b["via"] = order, award, f"mirrored from {best}"
        n += 1
    return n

# Twice from confident sources, so a route upgraded on the first sweep can seed
# the second; then once from medium sources, which can fix an order but must not
# lend a confidence they do not have themselves.
mirrored = borrow("high", "high")
mirrored += borrow("high", "high")
mirrored += borrow("medium", "medium")

# ── Pass 3: measure and emit ─────────────────────────────────────────────────

ordered_rows, route_rows, report = [], [], []

for route_id, b in built.items():
    order, meta = b["order"], b["meta"]
    cum, prev, hops, rows = 0.0, None, [], []
    for i, sid in enumerate(order):
        if prev is not None:
            h = hav(PTS[prev], PTS[sid])
            hops.append(h)
            cum += h
        rows.append((route_id, i + 1, sid, round(cum, 3)))
        prev = sid

    if cum > MAX_ROUTE_KM:
        skipped.append((route_id, f"{cum:.0f} km — intercity coach, out of scope"))
        continue

    ordered_rows.extend(rows)
    route_rows.append({
        "route_id": route_id, "ref": clean(meta.get("ref")),
        "name": clean(meta.get("name")), "operator": clean(meta.get("operator")),
        "from": clean(b["from"]) or NAMES[order[0]],
        "to": clean(b["to"]) or NAMES[order[-1]],
        "n_stops": len(order), "km": round(cum, 2), "seq_conf": b["conf"],
    })
    report.append({
        "route_id": route_id, "ref": clean(meta.get("ref")), "n": len(order),
        "conf": b["conf"], "via": b["via"],
        "greedy_km": round(b["base_len"], 2), "final_km": round(cum, 2),
        "saved_pct": round(100 * (b["base_len"] - cum) / b["base_len"], 1) if b["base_len"] else 0.0,
        "worst_hop": round(max(hops), 2) if hops else 0.0,
        "head_ok": name_sim(NAMES[order[0]], b["from"]) >= TERMINAL_WEAK,
        "tail_ok": name_sim(NAMES[order[-1]], b["to"]) >= TERMINAL_WEAK,
    })

# ── Write ─────────────────────────────────────────────────────────────────────

os.makedirs(OUT, exist_ok=True)
used = {r[2] for r in ordered_rows}
so = pd.DataFrame([rep_rows[s] for s in sorted(used)])
STOP_IDS.assign(so["stop_id"])
ROUTE_IDS.assign(r["route_id"] for r in route_rows)

so["src_id"] = so["stop_id"]
so["stop_id"] = so["stop_id"].map(STOP_IDS)
so["name"] = so["name"].map(clean)
so["lat"] = so["lat"].round(6)
so["lon"] = so["lon"].round(6)
so = so.sort_values("stop_id")
so[["stop_id", "name", "lat", "lon"]].to_csv(f"{OUT}/bus_stops.csv", index=False)

ro = pd.DataFrame(route_rows)
ro["src_id"] = ro["route_id"]
ro["route_id"] = ro["route_id"].map(ROUTE_IDS)
ro = ro.sort_values("route_id")
ro[["route_id", "ref", "name", "operator", "from", "to",
    "n_stops", "km", "seq_conf"]].to_csv(f"{OUT}/bus_routes.csv", index=False)

# Kept in route-then-sequence order: BusEngine reads the rows in file order to
# tell one direction of a route from the other.
rso = pd.DataFrame(ordered_rows, columns=["route_id", "seq", "stop_id", "cum_km"])
rso["route_id"] = rso["route_id"].map(ROUTE_IDS)
rso["stop_id"] = rso["stop_id"].map(STOP_IDS)
rso = rso.sort_values(["route_id", "seq"])
rso.to_csv(f"{OUT}/bus_route_stops.csv", index=False)

# Traceability back to the source ids. Stays in tools/ — it is not needed on the
# device and would only add weight to the APK.
pd.concat([
    so[["src_id", "stop_id"]].rename(columns={"stop_id": "app_id"}).assign(kind="stop"),
    ro[["src_id", "route_id"]].rename(columns={"route_id": "app_id"}).assign(kind="route"),
]).to_csv("bus_id_map.csv", index=False)

# ── Report ────────────────────────────────────────────────────────────────────

rep = pd.DataFrame(report)
print(f"routes reconstructed: {len(route_rows)}  (skipped {len(skipped)})")
for r, why in skipped:
    print(f"   skipped {r}: {why}")
print()
print(f"bus_stops.csv        {len(so)} stops")
print(f"bus_routes.csv       {len(route_rows)} routes")
print(f"bus_route_stops.csv  {len(ordered_rows)} ordered stop rows")
print()
print("sequence confidence:")
print(rep.conf.value_counts().reindex(["high", "medium", "low"])
      .fillna(0).astype(int).to_string())
print()
print(f"chain ends on both declared terminals: "
      f"{int((rep.head_ok & rep.tail_ok).sum())}/{len(rep)}")
imp = rep[rep.saved_pct > 0.05]
print(f"refinement shortened the chain on {len(imp)} routes, "
      f"median saving {imp.saved_pct.median():.1f}%, best {rep.saved_pct.max():.1f}%")
print(f"routes ordered from their opposite direction: {mirrored}")
print(f"routes with an inter-stop gap over 3 km: {int((rep.worst_hop > 3).sum())}")
print()
print("Lowest-confidence routes (review these):")
print(rep[rep.conf == "low"].sort_values("n", ascending=False)
      .head(10)[["route_id", "ref", "n", "final_km", "worst_hop"]].to_string(index=False))
