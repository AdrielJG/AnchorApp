# Live journey sharing

Share a journey in progress with one person. They see where you are, which
train or metro you are on, how far along you are and when you will arrive.
Sharing ends on its own when you arrive.

## Sending

`Share live journey` sits under `Share this route` on every route card. It
opens `LiveJourneyActivity`, which lists the journey leg by leg. Every rail and
metro leg is pre-filled with the next service and can be tapped to lock in the
one you actually boarded — before starting, and at any point after.

`Start live sharing` asks for location, then notifications, then fires the
share sheet with a link and a six-character code.

## Following

The link opens `JourneyViewerActivity`. Anyone without the app can be given the
code instead — `Follow a journey` in the main menu.

The follower sees a status badge, the current position, the current leg, the
locked-in service, next stop or transfer, a progress bar and an arrival time.

## How position is worked out

`JourneyTracker` folds GPS fixes into the leg list `JourneyBuilder` produced
from the route. Two rules matter:

**Progress never moves backwards.** A fix that bounces off a building would
otherwise walk the follower's screen back two stations, which reads as a fault
in the app rather than in the fix.

**A projection is never called a sighting.** Underground on Metro 3 there is no
fix to be had, so the leg advances on the timings of the service the rider
locked in — and the badge says `ON SCHEDULE`, not `LIVE GPS`. With no fix and
no locked service it says `NO SIGNAL` and stops claiming to know anything.

| Badge | Means |
|---|---|
| `LIVE GPS` | Real fix, under 90 seconds old |
| `ON SCHEDULE` | No fix; position interpolated from the locked service |
| `NO SIGNAL` | No fix and nothing to interpolate from |
| `ARRIVED` / `SHARING ENDED` | Journey finished, or stopped by the rider |

## Why a foreground service

`LiveJourneyService` has to be one. The promise of the feature is that someone
can watch your progress while your screen is off, and a backgrounded activity
gets its location updates throttled to nothing within minutes — stranding the
follower on a stale position without either side being told.

The ongoing notification is not a formality either. It is the honest disclosure
that location is leaving the device, and the one-tap way to stop it.

## Ending

Sharing stops when the last leg completes, when `Stop sharing` is tapped, or
after six hours (`JourneyTracker.MAX_DURATION_MS`). All three write a final
ping so the follower's screen closes the loop rather than freezing.

## Files

| | |
|---|---|
| `journey/JourneyModels.kt` | Legs, journey, ping, share codes |
| `journey/JourneyBuilder.kt` | Route card + timetable → legs |
| `journey/JourneyTracker.kt` | Position, progress, ETA. No Android types |
| `journey/JourneyRepository.kt` | Firebase, plus an on-device fallback |
| `journey/LiveJourneyService.kt` | Foreground tracking |
| `journey/JourneySession.kt` | The journey being shared right now |
| `ui/LiveJourneyActivity.kt` | Setup, then live control |
| `ui/JourneyViewerActivity.kt` | The follower's screen |
| `ui/JourneyLegView.kt` | Shared leg renderer, so both sides agree |

## Known limits

Without `google-services.json` everything runs locally — tracker, notification,
timeline — but nothing leaves the phone, so no code resolves. The share sheet
says so rather than handing out a dead code.

The follower gets a station readout and an `Open location in Maps` button, not
an embedded map. An embedded map renders as a grey rectangle on any build
missing `MAPS_API_KEY` in `local.properties`, and a grey rectangle is a worse
answer than a station name.

`https://anchor.app/j/{code}` is a placeholder host. It is registered with
`autoVerify="false"`, so Android offers Anchor in the app chooser rather than
opening it silently. Point it at a real domain with an assetlinks file to make
the link open straight through.

## Shared service
LiveJourneyService also hosts Live Navigation (see LIVE_NAVIGATION.md) on the same location subscription. Sharing behaviour is unchanged.
