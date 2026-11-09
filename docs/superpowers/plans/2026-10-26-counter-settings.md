# Counter Settings Implementation Plan

**Goal:** Add the approved menu → edit two scores → return result flow and persistent scores to the standalone counter.

**Architecture:** Java/XML with AppCompat Toolbar and XML menu. MainActivity registers ActivityResultLauncher before creation, passes two integer extras to ScoreSettingsActivity, accepts only complete nonnegative results, saves both scores together through MODE_PRIVATE SharedPreferences and apply(). Saved-instance state takes priority during recreation. Back/cancel never changes scores.

**Constraints:** Work on master, preserve existing changes, no currency features, no dependency upgrades, no push.

- [x] Test score input bounds and rejection before implementing parsing.
- [x] Add isolated preference tests and reset stored scores before/after every scoring UI test.
- [x] Add score storage, Toolbar/menu resources, editor layout and Activity; preserve unfinished editor input across recreation.
- [x] Implement Bundle/Intent extras, ActivityResultLauncher callback and persistence on every score change and reset.
- [x] Verify edit/save/cancel, invalid inputs, recreation, relaunch persistence and score reset persistence on the emulator.
- [x] Run assembleDebug, testDebugUnitTest, lintDebug and connectedDebugAndroidTest; review the diff and verify no currency code.

## Verification

- assembleDebug, testDebugUnitTest, lintDebug and connectedDebugAndroidTest succeeded.
- 3 unit tests and 10 emulator tests passed; lint reported 0 errors.
- Menu save/cancel, editor recreation, score relaunch/reset persistence and rotation were verified.
