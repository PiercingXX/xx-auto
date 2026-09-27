# xx-auto-verify2

Operator mill contract materialized from dispatch. Do not invent a different exam.

## Goal

Verify one open box: "`playback/AudiobookPlaybackService.kt : MediaSessionService`, `foregroundServiceType="mediaPlayback"`, exported, intent-filter `androidx.media3.session.MediaSessionService`. Owns the ExoPlayer that today lives with `MainActivity` / `PhoneListen` — move it, don't duplicate it." The previous mill (xx-auto-todo4) delivered nothing for it — every file it named was already on main, so building it again lands nothing. The list states no evidence line for it, so state in the change exactly what you ran and what it printed. If it fails, implement exactly what fails and land that. If it passes, check the box off in the todo list and record the evidence in the same change. Do not re-mill files that are already present, and do not widen this past the one box. Locked STACK=android.

### T1 — Implement the scoped goal

- files: app/src/main/java/com/piercingxx/xxauto/MainActivity.kt
- verify: ./gradlew :app:testDebugUnitTest --offline

## Final gate

- verify: ./gradlew testDebugUnitTest --offline
