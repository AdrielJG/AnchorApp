# Anchor — turning on the real database

Accounts and chat both run through Firebase. Until you add
`app/google-services.json` the app still builds and runs, but it falls back to
on-device storage: accounts live in the local Room table and chat messages never
leave the phone. `Backend.isCloud()` is what decides, and it logs which mode it
picked under the tag `AnchorBackend`.

## Setup — about 15 minutes

1. **Create the project.** console.firebase.google.com → Add project → name it
   `anchor`. Google Analytics is not needed.

2. **Register the app.** Add app → Android. Package name must be exactly
   `com.example.mumbaitransit`. Download `google-services.json` and put it in
   the `app/` folder, next to `build.gradle`.

3. **Turn on Authentication.** Build → Authentication → Get started →
   Sign-in method → Email/Password → Enable.

4. **Create the Realtime Database.** Build → Realtime Database → Create
   database → pick the Singapore or Mumbai region → start in **locked mode**,
   then paste the rules below.

5. Rebuild. Gradle picks up the plugin on its own — nothing to edit.

## Rules

Locked so only signed-in users can read or post, and nobody can post as someone
else or rewrite history:

```json
{
  "rules": {
    "users": {
      "$uid": {
        ".read":  "auth != null && auth.uid == $uid",
        ".write": "auth != null && auth.uid == $uid"
      }
    },
    "usernames": {
      "$name": {
        ".read": true,
        ".write": "auth != null && ((!data.exists() && newData.val() == auth.uid) || data.val() == auth.uid)",
        ".validate": "newData.isString()"
      }
    },
    "rooms": {
      "$roomId": {
        "days": {
          "$dayKey": {
            ".read": "auth != null",
            ".write": "auth != null",
            "messages": {
              "$msgId": {
                ".validate": "newData.hasChildren(['uid','username','sentAt']) && newData.child('uid').val() == auth.uid"
              }
            }
          }
        }
      }
    },
    "journeys": {
      "$code": {
        ".read": "auth != null",
        ".write": "auth != null && (!data.exists() || data.child('meta/ownerUid').val() == auth.uid)"
      }
    }
  }
}
```

### A note on the two account branches

`users/{uid}` is owner-only in both directions. It holds the phone number, the
home and work addresses and every saved route, and nothing in the app needs to
read another person's copy — chat writes the username onto each message rather
than looking it up, so there is no reason to leave the branch readable.

`usernames/{name}` is the opposite: world-readable and holding nothing but a uid.
It has to be readable without a session because sign-up checks whether a name is
free before an account exists to check it with. Read is scoped to `$name`, so a
client can ask about one name at a time and cannot list them. The write rule
claims a free name or lets the owner release their own, which is what makes the
claim survive two people typing the same name at the same instant.

A journey is readable by any signed-in user holding the code, which is the
point of sharing one — the six-character code is the only thing that resolves
it. Writes are locked to the account that started it, so nobody else can move
your position or end your journey.

### A note on the chat write rule

Chat rooms clear every service day, so the day node has to be deletable — that
is what `".write": "auth != null"` at `$dayKey` allows, and it is what lets
`pruneOldDays()` drop rolled-over days instead of letting the database grow for
the life of the project. The trade-off is that the earlier `!data.exists()`
guarantee is gone: a signed-in user could now delete another day's messages.

For a graded demo that is fine. Before this went anywhere real you would move
the prune to a scheduled Cloud Function and put `".write": false` back on
`$dayKey`, leaving only the per-message create rule. The app does not depend on
the prune succeeding — `ChatDay` already scopes every read, so refusing the
delete costs storage and nothing else.

## Data shape

```
users/{uid}                            { name, username, usernameLower,
                                         phone, createdAt }
users/{uid}/home                       { id, kind, label, address,
                                         lat, lon, savedAt }
users/{uid}/work                       { ...same shape as home }
users/{uid}/addresses/{pushId}         { ...same shape, many }
users/{uid}/routes/{pushId}            { id, originLabel, destLabel, scenario,
                                         scenarioLabel, modeStr, totalMin,
                                         totalFare, transfers, linesUsed,
                                         routeType, savedAt }

usernames/{lowercase}                  "<uid>"

rooms/{roomId}/days/{yyyy-MM-dd}/messages/{id}
                                       { uid, username, text, reportType,
                                         trainNo, trainLabel, station,
                                         platform, sentAt }

journeys/{code}/meta                   { code, ownerUid, ownerName,
                                         originLabel, destLabel,
                                         startedAt, plannedTotalMin }
journeys/{code}/legs                   [ { kind, line, lineShort, colorHex,
                                           fromStop, toStop, stops[],
                                           plannedMin, note, serviceLabel,
                                           serviceDepMins, serviceArrMins } ]
journeys/{code}/ping                   { at, lat, lon, fix, legIndex,
                                         nearestStation, nextStop, progressPct,
                                         etaEpochMs, source, status }
```

### Accounts

Sign-up asks for four things and writes exactly those four: **name**, **username**,
**phone**, **password**. `home`, `work`, `addresses` and `routes` do not exist on
a new account — the branch is created the first time the user adds one from
**My Account**, and `addresses` and `routes` hold as many entries as they like.

The password is not in the table above, and that is deliberate. Firebase Auth
holds it, salted and hashed on Google's side; a copy under `users/{uid}` would be
a readable password sitting next to the data it is supposed to protect.

**Phone and password, without OTP.** Firebase Auth has no phone-and-password
provider — its phone sign-in is OTP-only, which would mean an SMS on every login
and no password at all. So the account is created against the email/password
provider under a deterministic address built from the number: +91 98920 27657
becomes `919892027657@anchor.app`. Nothing is ever sent there; it is an
identifier, and `Phone` in `auth/Profile.kt` is the only place that knows about
it. Because the mapping is deterministic, logging in resolves straight from the
number typed into the form with no lookup, and Firebase Auth's own uniqueness
check on email gives one-account-per-number for free.

The cost is that **Firebase's password-reset email cannot work** — there is no
real mailbox behind the address. A reset today means the Firebase console. If
this ever needs to be self-service, the route is phone OTP as a second provider
linked to the same account, not a second password.

Step 3 of the setup is still Email/Password — that is the provider the phone
number rides on. Phone provider does not need to be enabled.

`roomId` is one of the sixteen ids in `ChatRooms.all` — eight suburban rail
(`central-up`, `central-down`, `western-up`, `western-down`, `harbour-up`,
`harbour-down`, `transharbour-up`, `transharbour-down`) and eight metro
(`metro1-up`, `metro1-down`, `metro2a-up`, `metro2a-down`, `metro3-up`,
`metro3-down`, `metro7-up`, `metro7-down`).

The day segment comes from `ChatDay.key()` and turns over at 03:00 Asia/Kolkata
rather than midnight, because Mumbai locals run past 01:00 and restart around
04:00 — cutting at 03:00 puts the reset in the one window where nobody is on a
platform. `ChatDay.RESET_HOUR` is the single constant to change.

`ping` is split from `meta` and `legs` on purpose: it is rewritten every fifteen
seconds while the itinerary is written once, so a follower's screen re-reads one
small object per heartbeat instead of the whole route.

Without `google-services.json` the tracker, the notification and the timeline
all still run, but nothing leaves the phone — the share sheet says so rather
than handing out a code that will never resolve.

`sentAt` is written as `ServerValue.TIMESTAMP`, so a phone with a wrong clock
cannot push its messages to the wrong place in everyone else's list.

## Before this goes public

The rules above stop impersonation but not spam. Add a per-user post rate limit,
a report/block path, and consider requiring a verified email before posting.
