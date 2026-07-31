# OnTheCourt

OnTheCourt is a native Android app that helps people find sports courts near them and organize pickup games with friends. Pick a sport, find a court on the map, create a game, and invite people to join.

## What it does

- **Pick a sport** - soccer, basketball, or tennis.
- **Find courts** - search a city and see nearby courts on a live Google Map, with a matching list view.
- **Create a game** - set date, time, player limit, number of teams, and duration for a court.
- **Join open games** - see games other users created at the same court and join them.
- **Friends** - search for other users, send and accept friend requests, invite friends straight into a game.
- **Chat** - real-time 1:1 chat with friends.
- **Hebrew UI** - the whole app is in Hebrew, with full right-to-left (RTL) layout support.

## Tech stack

- **Language:** Kotlin
- **UI:** Android Views with ViewBinding and Material 3 components (no Jetpack Compose)
- **Navigation:** Jetpack Navigation Component with Safe Args
- **Backend:** Firebase Authentication and Cloud Firestore (real-time data for games, friends, and chat)
- **Maps:** Google Maps SDK for Android
- **Court search:** SerpApi (Google Maps engine) over OkHttp, used to find real courts near a city
- **Async:** Kotlin Coroutines
- **Build system:** Gradle with Kotlin DSL (`build.gradle.kts`)

## Project structure

```
app/src/main/java/com/onthecourt/app/
  auth/          Login and registration screens
  home/          Sport selection screen (the app's home screen)
  map/           Court search map and court detail screens
  game/          Create game and game detail screens
  mygames/       The user's own active and past games
  friends/       Friend search, requests, and friend list
  chat/          1:1 chat between friends
  model/         Data classes shared across the app (Game, User, ChatMessage)
  util/          Small shared helpers (avatar colors, Firestore actions, sport display names)
```

Navigation runs through a single graph (`nav_graph.xml`) with three top-level screens reachable from the bottom navigation bar: Home, My Games, and Friends. The Home screen also has a shortcut button that always resets navigation back to it, no matter how deep the user is in the court/game flow.

## Setup

1. Clone the repo and open it in Android Studio.
2. Create a Firebase project and download your own `google-services.json` file. Replace the placeholder file at `app/google-services.json` with it.
3. Copy `app/secrets.xml.example` to `app/src/main/res/values/secrets.xml` and fill in your own keys:
   - `google_maps_key` - a Google Maps SDK for Android API key.
   - `serpapi_key` - a SerpApi key (used for live court search).
   `secrets.xml` is gitignored, so your keys stay out of the repo.
4. Enable Firebase Authentication (Email/Password) and Cloud Firestore in your Firebase project.
5. Build and run on an emulator or device running Android 14 (API 34) or newer.

## Notes

- There is no separate backend server. Firebase (Auth and Firestore) and SerpApi are called directly from the app.
- The API keys in this repo are placeholders. The app will build, but map and court search features need real keys to work.
