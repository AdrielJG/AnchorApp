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
        ".read": "auth != null",
        ".write": "auth != null && auth.uid == $uid"
      }
    },
    "rooms": {
      "$roomId": {
        "messages": {
          ".read": "auth != null",
          "$msgId": {
            ".write": "auth != null && !data.exists()",
            ".validate": "newData.hasChildren(['uid','username','sentAt']) && newData.child('uid').val() == auth.uid"
          }
        }
      }
    }
  }
}
```

`!data.exists()` on the write rule means a message can be created but never
edited or deleted from the client.

## Data shape

```
users/{uid}                  { username, email }
rooms/{roomId}/messages/{id} { uid, username, text, reportType,
                               trainNo, trainLabel, station, platform, sentAt }
```

`roomId` is one of the eight ids in `ChatRooms.all` — `central-up`,
`central-down`, `western-up`, `western-down`, `harbour-up`, `harbour-down`,
`transharbour-up`, `transharbour-down`.

`sentAt` is written as `ServerValue.TIMESTAMP`, so a phone with a wrong clock
cannot push its messages to the wrong place in everyone else's list.

## Before this goes public

The rules above stop impersonation but not spam. Add a per-user post rate limit,
a report/block path, and consider requiring a verified email before posting.
