# Multimodal routing

## Model
Every train, bus and mixed route is a `RouteCard` with `legs: List<RouteLeg>`
(`model/Models.kt`). A leg has a `LegMode`: WALK, TRAIN, METRO or BUS. One ride = one
leg; a change of line or mode ends a leg, and the walk between rides is its own leg.
Auto and cab cards have no legs.

## Generation
`engine/JourneyPlanner.kt` runs one generic search over `RoutingConfig.ROUTE_PATTERNS`
(`engine/RoutingConfig.kt`). A pattern is a list of ride modes (RAIL, BUS). The search
has no per-pattern code: to add a combination, add one line to `ROUTE_PATTERNS`.

- RAIL steps: the existing rail-graph Dijkstra (`TransitEngine`), four weightings,
  distinct paths only. Train-only keeps the original pairwise search.
- BUS steps: `BusEngine`. Bus-only keeps the original `findOptions` search.
  Station-anchored rides use `ridesToTargets`.
- Walks are added where rides meet.

## Validity rules (all in RoutingConfig)
| Constant | Value | Applies to |
|---|---|---|
| MAX_WALK_TO_BUS_STOP | 0.9 km | origin/destination ↔ bus stop |
| MAX_WALK_TO_STATION | 0.5 km | bus stop ↔ rail/metro station when changing mode |
| MAX_TRANSIT_LEGS | 3 | pattern steps (a train step with an in-network line change counts once) |
| MIN_PROGRESS_KM | 0.5 km | each ride must end this much nearer the destination (mixed routes) |
| MAX_OVERSHOOT_KM | 0.5 km | a ride may not pass nearer the destination than where it ends (mixed routes) |

Walking from the origin to the first station, and from the last station to the destination,
keeps the existing rule: the Board at / Get off at station, or the three nearest stations.
The planner drops only routes that revisit a station and exact duplicates; choosing what
is shown is the selector's job.

## Classification
One rule, `JourneyKind.of(legs)` in `model/Models.kt`, from the journey's rides only.
Walks never count; metro counts as train. The pattern that generated a journey is ignored.

| Legs | Kind |
|---|---|
| WALK, TRAIN, WALK | TRAIN |
| WALK, BUS, WALK | BUS |
| WALK, TRAIN, BUS, WALK | MIXED |
| WALK, BUS, TRAIN, BUS, WALK | MIXED |

## Selection
`engine/RouteSelector.kt`, run once per search, builds a separate short list for each filter.

1. Validate: drop a ride that goes nowhere, or a bus hop quicker to walk than to wait for and ride.
2. Deduplicate: one journey per set of rides.
3. Classify: as above.
4. Collapse near-identical: same kind, same rides in order (any bus counts as the same ride,
   trains must be the same line), changes within `SAME_PLACE_KM` → keep the best one.
5. Drop dominated: beaten on time, fare, walking and transfers at once.
6. Rank by convenience score: time as shown, walking ×2, expected train wait, per-transfer
   cost, bus↔train change cost, fare (₹4 = 1 min), lowest MRI on the journey, bus stop-order confidence.
7. Pick up to `MAX_ROUTES_SHOWN` (8): the best; the fastest, cheapest, least-walking and
   fewest-transfer routes when they beat it by a margin that matters; under All, the best
   train-only, bus-only and mixed route if none is listed and it is useful and not beaten
   outright; then the next best that ride different lines. Nothing is added to fill a quota.

Useful = score within 1.15 × best + 10 and time within 1.5 × fastest + 10, per filter.
All weights and limits are constants in `engine/RoutingConfig.kt`.

Labels: ⚡ Fastest, 💰 Cheapest, 🚶 Least walking, 🔄 Fewest transfers go to the one listed
route that is strictly best on that count (a tie earns none). A kind alternative with no
badge reads 🚆 Train only / 🚇 Metro only / 🚌 Bus only; the top card with no badge reads
⭐ Recommended; the rest keep their own title.

The old distance-only bus estimate card is removed: Bus shows only real bus journeys.

## Screen
`ui/RouteListActivity.kt`: All | Train | Bus | Mixed, each showing its own list from the
selector. All ends with auto and cab (outside the cap). No paging.

The map (`map/RouteShape.kt`), share image (`share/RouteShareCard.kt`), Live Journey
(`journey/JourneyBuilder.kt`) and train timings (`TransitEngine.timetableFor`) all read the legs.

## Test run (real data, desktop JVM)
Transit cards shown under each filter; All also ends with auto and cab.

| Trip | Generated | All | Train | Bus | Mixed | Time |
|---|---|---|---|---|---|---|
| Jui Nagar → Mankhurd | 119 | 1 | 1 | 1 | 2 | 0.50 s |
| Thane → Vashi | 454 | 3 | 2 | 1 | 3 | 0.25 s |
| Ghatkopar → Kurla | 289 | 1 | 1 | 1 | 1 | 0.09 s |
| Andheri W → Powai | 2,667 | 6 | 3 | 1 | 5 | 0.49 s |
| Borivali → Goregaon E | 3,363 | 2 | 3 | 1 | 1 | 0.24 s |
| Dadar → BKC | 4,130 | 1 | 1 | 1 | 1 | 0.30 s |
| Chembur → Worli | 5,387 | 5 | 1 | 1 | 4 | 0.49 s |
| Colaba → Andheri | 32,584 | 2 | 2 | 2 | 1 | 2.18 s |
| Mulund → Nerul | 462 | 3 | 2 | 2 | 1 | 0.13 s |

58 random trips: 569,380 generated → 215 transit cards under All (3.7 a trip). On every trip,
no Train card has a bus leg, no Bus card has a train or metro leg, every Mixed card has
both, and no listed card is beaten outright by another in the same list.

Almost all the time is generation; selection adds about 0.1 s. On a phone, expect roughly
3–5× these times.
