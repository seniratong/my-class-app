# Mid-Autumn Greeting Card Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Replace the calculator screen with a student-friendly Mid-Autumn Festival greeting card using a local cartoon illustration and ConstraintLayout.

**Architecture:** Keep the current XML Activity architecture. `activity_main.xml` owns the visual hierarchy, resource XML files own copy/colors/styles, and `MainActivity.kt` only inflates the layout because the greeting card is static.

**Tech Stack:** Android XML, ConstraintLayout 2.1.4, Material 3 theme, Kotlin, generated PNG asset.

## Global Constraints

- Keep the existing package and Activity entry point.
- Do not add external dependencies.
- Use `ConstraintLayout`, `ImageView`, and `TextView` as the core teaching elements.
- Keep visible text in `strings.xml` and colors in `colors.xml`.
- The image must be copied into the workspace as a project asset.

### Task 1: Create the illustration asset

**Files:**
- Create: `app/src/main/res/drawable/mid_autumn_rabbit.png`

- [ ] Generate one original square cartoon illustration with a moon, rabbit, osmanthus and clouds; avoid embedded text so Android resources remain the source of truth for copy.
- [ ] Inspect the generated image for composition, legibility and suitability as a centered phone-card image.
- [ ] Copy the selected asset into the drawable directory.

### Task 2: Replace the calculator layout

**Files:**
- Modify: `app/src/main/res/layout/activity_main.xml`

- [ ] Replace the calculator controls with a title TextView, an ImageView, a greeting TextView, and a sender TextView.
- [ ] Constrain every view on both axes; use `centerCrop` for the illustration and margins that work on a phone screen.
- [ ] Use resource references for all visible text and colors.

### Task 3: Update resources and Activity

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values/colors.xml`
- Modify: `app/src/main/res/values/styles.xml`
- Modify: `app/src/main/java/com/example/myapplication1/MainActivity.kt`

- [ ] Add greeting-card copy and semantic color tokens.
- [ ] Add reusable card/title/text styles without introducing another UI framework.
- [ ] Remove calculator-only imports, state and click listeners; leave a minimal `onCreate` that calls `setContentView`.

### Task 4: Verify the deliverable

**Files:**
- Test: `app/build/outputs/apk/debug/app-debug.apk`

- [ ] Run `:app:assembleDebug` and confirm exit code 0.
- [ ] Run `:app:testDebugUnitTest` and confirm exit code 0.
- [ ] Run `:app:lintDebug` and confirm no hardcoded visible copy is reported.
