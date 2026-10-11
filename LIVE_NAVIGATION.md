# Live Navigation

Guides the rider through the selected route, leg by leg. Separate from Live Sharing; both can run at once.

## Flow
RouteCard -> JourneyBuilder (LiveJourney legs) -> NavPlanBuilder (NavPlan with stop coords) -> NavigationEngine (state machine) -> NavigationSession (StateFlow) -> NavigationActivity.

## Entry
Route Details card shows "Next Step" with a CTA named after the first leg: "Start Walking" (walk first) or "Start Journey" (transit first). "Resume Navigation" if already navigating this route.

## States
Built from the route's actual legs: WALKING_TO_STOP / WALKING_TO_TRANSFER / WALKING_TO_DESTINATION, WAITING_FOR_TRAIN / METRO / BUS, ON_TRAIN / METRO / BUS, REACHED_STOP (transfer screen), ARRIVED, STOPPED.

## Data rules
- Walk: user GPS; walking steps from Google Directions (walking) when MAPS_API_KEY / Directions API is available.
- Train: user GPS + locked service timetable + RailRadar live status ("View Live Train Details", "Change train").
- Metro: GPS + timetable; no live feed.
- Bus: user GPS against the known stop sequence only. No live bus status, ETA or vehicle position is ever shown. Bus never advances on the clock.

## GPS
One fused subscription in LiveJourneyService serves both sharing and navigation (navigation uses the faster rate). GPS stops on arrival or Stop Navigation. If sharing is active on the same journey, it ends at arrival (existing behaviour).

## Off route
Walking drift, off-corridor, wrong direction, missed stop -> "You appear to be off route" + "Recalculate route" (re-runs search from current location). Wrong train -> "Change train".

## Tests
JVM harness (navigation engine + real bus CSVs): Walk-Train-Walk, Walk-Bus-Walk, Walk-Train-Walk-Bus-Walk, Walk-Bus-Walk-Train-Walk, Train-Bus, Bus-Train all reach ARRIVED through every leg.
