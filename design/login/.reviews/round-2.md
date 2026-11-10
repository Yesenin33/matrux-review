# Designer review — round 2 (final for this gate)

**Date:** 2026-11-10T19:50:55Z
**DESIGN.md re-read at:** 2026-11-10T19:50:55Z (`/root/palma/design/DESIGN.md`, 124 lines, re-read this round, not cached)
**Artifact:** `/root/palma/design/login/v1.png` `v2.png` `v3.png` `v4.png` `all.png` (1170x2532 each); source `/root/palma/design/login/src/render.py` (176 lines)
**Prior report:** `/root/palma/design/login/.reviews/round-1.md`
**Also checked:** `docs/TASK-LOGIN-DESIGN.md`, `image-build/build.sh`, `app/src/main/assets/palma-session`, `app/src/main/java/ru/palma/app/shell/Proot.kt`, `AppGraph.kt:82`, `SessionController.kt:348-387`
**Viewport:** mobile 390x844 @3x

## Summary

- BLOCK: 0
- WARN: 6
- FYI: 6

## How contrast was measured

`test-v2/tools/text-contrast.mjs` does not exist and `omd` is not on PATH, so the tool was not run (W1). I measured the PNGs directly instead: `magick` to PPM, then for each text band the darkest pixel against the real background pixel (245,245,247).

| Band (CSS px) | Darkest pixel | Ratio |
|---|---|---|
| v1 `Matrux` y356–380 | (29,29,31) ink | 15.46 |
| v1 `matrux login:` y398–411 | (110,110,115) muted | 4.66 |
| v2 `Запущена` y423–437 | (110,110,115) muted | 4.66 |
| v3 log lines 1–3 y53–105 | (110,110,115) muted | 4.66 |
| v3 line 4 y112–122 | (29,29,31) ink | 15.46 |
| v4 Debian / Ubuntu / Arch y377–473 | (110,110,115) muted | 4.66 (32px, so the bar is 3:1) |
| CTA label on white | ink on (255,255,255) | 16.83 |
| CTA pill against canvas, v1 column x=195 | white (255,255,255) / darkest shadow (232,232,234) | 1.09 / 1.12 |

All text passes. The 4.66 cases for 13px mono have 0.16 to spare.

## Prior items — status

| ID | Round-1 issue | Status | Evidence |
|---|---|---|---|
| B1 | CTA was an accent fill | **RESOLVED** | render.py:46: `background: var(--surface); color: var(--ink); box-shadow: var(--shadow)`, which matches DESIGN.md:118. No `var(--accent)` is used anywhere in a variant body. Measured: 0 accent pixels; the label is (29,29,31) on (255,255,255). |
| B2 | `Запущена` at 4.02:1 | **RESOLVED** | render.py:83 `class="body muted"`. Measured 4.66:1. |
| B3 | v4 menu with a second action | **RESOLVED** (the action is gone; the ambiguity left over is N3) | render.py:116-128: plain `<p class="display">`, no chevron, no `.row`, no raised state, no shadow other than the CTA's. |
| W1 | Contrast tool not run | **UNRESOLVED** (tooling) | See W1 below. |
| W2 | `$` transcript and a prompt after `exec` | **RESOLVED as specified**, but the fix I suggested in round 1 was wrong (see N1) | render.py:100-103 match build.sh:27, :28, :31 (only `>/tmp/xvfb.log 2>&1 &` trimmed from the end) and :37 character for character. No `$`, no caret. |
| W3 | Mono outside DESIGN.md:44 scope | **UNRESOLVED, accepted as a known issue** (WARN) | v4 now uses Inter Display, so only v1 render.py:65 is left. See W3 below. |
| W4 | Off-token spacing | **RESOLVED** | render.py:48-49 `48px` = 24 inset + 24 block gap. render.py:57 `8px`, :58 `24px`, :75 `gap: 8px`, :95 `gap: 24px`, :38 sides `20px`. Every value is 8, 20, 24 or 48, all from DESIGN.md:50/53. |
| W5 | CTA states not specified | **Downgraded to FYI (F1)** | The HTML is a static picture and never ships. The states already exist in SignInScreen.kt:170-185. A PNG cannot fix this. |
| W6 | v1 and v3 used the same technique | **RESOLVED**; the same problem now appears between v2 and v4 (N2) | v1 is one centred group (render.py:62-68). v3 splits into a log at the top and the name plus CTA at the bottom (render.py:99-109). No `.caret` class remains. |
| W7 | Abstract monitor icon in v2 | **RESOLVED** | No `icon(` call appears in any variant body. The `icon()` helper at render.py:11 is now unused. |
| W8 | Prompt plus caret looked like a text field | **RESOLVED** | render.py:65 has no caret. The CTA follows 24px below as the answer to the prompt. |

## Issues

### [WARN] W1. Contrast tool still not run
- **Location:** `test-v2/tools/text-contrast.mjs` (not in the repo); `omd` is not on PATH
- **Rule:** §1.7: an unrun check is reported, not counted as a pass.
- **Evidence:** The numbers above come from PNG pixels against the real background, which is the same method the tool uses (nominal colour against the actual background pixel). Every surface is flat, so I am confident in the numbers. The rule still requires this WARN.
- **Fix suggestion:** Run the tool when it is available. Do not lighten `--muted`: 13px mono is at 4.66.

### [WARN] W3. v1 `matrux login:` uses mono outside DESIGN.md:44
- **Location:** render.py:65; DESIGN.md:44 ("Только строки запуска гостя, код в уроках и командах шпаргалок")
- **Decision: WARN with a known issue, not BLOCK.** Reasons:
  - Mono is an existing token (DESIGN.md:44), not a new value, so CLAUDE.md's "сначала добавь в DESIGN.md" rule about new values does not apply directly.
  - The owner's brief explicitly asks for "моноширинный набор, приглашение входа" (TASK-LOGIN-DESIGN.md, «Тема Linux»).
  - These are exploration PNGs; nothing is shipped from them.
- **Condition:** If v1 is chosen, this becomes BLOCK at implementation unless DESIGN.md:44 gains "приглашение окна входа" before SignInScreen.kt is touched. The same applies to v3: launch lines shown on a sign-in screen are decoration, not a running guest. Strictly that is outside "строки запуска гостя" too, so add both scopes in one edit.
- **Fix suggestion:** Hand the owner one line to add to DESIGN.md:44: "…, приглашение и строки запуска на окне входа."

### [WARN] N1 (NEW). The v3 launch lines come from a script the device no longer runs
- **Location:** render.py:102-103; build.sh:31, :37; app/src/main/assets/palma-session:479-484
- **Rule:** Brief: "Никаких выдуманных данных"; "только то, что реально будет на экране".
- **Evidence:**
  - The app overwrites the rootfs session script with the bundled asset on every launch: `sessionScript = { app.assets.open("palma-session")… }` (AppGraph.kt:82), then `engine.installSessionScript(rootfs)` (SessionController.kt:348, Proot.kt:111-120). So build.sh:24-38 is replaced on a current install.
  - In the shipped script:
    - `exec dbus-run-session startxfce4` does not appear. The real line is `exec dbus-run-session -- xfce4-session` (palma-session:484) or `dbus-run-session -- sh -c "… exec xfce4-session"` (:186).
    - `1280x800x24` does not appear literally. The real line is `Xvfb ":0" -screen 0 "${MAXW}x${MAXH}x24" -nolisten tcp -ac` (:479).
  - So 2 of the 4 lines (render.py:102-103) are real text from the repo, but they never run on a current device.
  - In round 1 I told you to quote build.sh. That was my mistake: I had not checked the asset override.
- **Fix suggestion:** Quote the live X11 path, which runs when `x11.available()` (SessionController.kt:351, :376):
  ```
  export XDG_RUNTIME_DIR=/tmp/runtime-root      palma-session:15
  export DISPLAY=:0                             palma-session:483
  exec dbus-run-session -- xfce4-session        palma-session:484
  ```
  Keep the last line in ink. This also removes the long Xvfb line, which fixes N4.

### [WARN] N2 (NEW). v2 and v4 are the same composition
- **Location:** render.py:78-87 and render.py:120-131
- **Rule:** Brief: "три-четыре разных варианта", genuinely different techniques.
- **Evidence:** The two bodies match element for element:
  - Title `Matrux` in `.head` at y53.7–68.3, the same band in both.
  - A push spacer.
  - A Display `Alpine + XFCE` at x20.0–220.3, the same width in both.
  - A push spacer.
  - The pill in `.foot` at y730–821, the same band in both.

  v4 is v2 with three muted names added under the system name and `Запущена` removed. v3 also shares that foot band (y730–821), so three of the four frames end identically in all.png. Only v1 has a different skeleton.
- **Fix suggestion:** Change the skeleton of one variant, not just its middle block. For example, v4 could drop the `.head` Title and let the name stack start at the top inset with the CTA at the bottom. Or v2 could put `Matrux` in the Display slot and the system name and state in Body underneath.

### [WARN] N3 (NEW). The v4 stack (one name in ink, three muted) still reads as a selector
- **Location:** render.py:116-127 (`.names { color: var(--muted) }`, `.names .on { color: var(--ink) }`)
- **Rule:** Brief: "Вход единственный — кнопка VK ID". DESIGN.md:112: "Каталог — единственное место про системы".
- **Evidence:** Four 32px lines on a 36px pitch (y341, 377, 413, 449), one dark and the rest grey. This is the iOS picker-wheel and selected-row pattern, so users will try to tap or scroll it even without a chevron.
  - The ink highlight claims Alpine is "current". On a fresh install the real state is "Система не установлена" (strings.xml:9), and nothing is current.
  - The list is also hardcoded: on a real install the catalog comes from the server (StandShots.kt:113-115 already shows `Debian 13`, `Ubuntu`, `Arch`).
- **Fix suggestion:** Drop the selection semantics. Either set all four names in one colour (all ink, or all muted), which makes the stack a typographic statement, or keep the highlight only when an installed system exists and state that dependency in the hand-off note.

### [WARN] N4 (NEW). v3 Xvfb line overflows at the "Large" system font size
- **Location:** render.py:94 (`white-space: pre`), render.py:102, render.py:36 (`overflow-x: clip`)
- **Rule:** §1.5: no horizontal overflow. The brief asks about large system fonts.
- **Evidence:** The line spans x20.3–349.0 (328.7px) out of 350 available, so 21px are left. It overflows at about 1.065x font scale. Android "Large" is 1.15x. With `pre` and `clip`, the end of the line (`tcp`) is silently cut off.
- **Fix suggestion:** The N1 fix removes this line. The longest line left would be `export XDG_RUNTIME_DIR=/tmp/runtime-root` (305px at 1x, about 351px at 1.15x). For implementation, allow soft wrap (`softWrap = true`, hanging indent) instead of `pre`.

### [FYI] F1. CTA states (was W5)
- **Location:** render.py:46-47
- **Evidence:** `.cta` has no `:active`, `:focus-visible` or `[disabled]` rules. This does not matter for a PNG. At implementation, keep SignInScreen.kt:170-185 (press scale, disabled alpha) and add a focus ring of at least 3:1 against canvas (`--ink` 15.46; do not reintroduce accent).

### [FYI] F2. The pill's edge contrast is 1.09:1, and DESIGN.md:118 requires it
- **Location:** render.py:46; DESIGN.md:32, :118
- **Evidence:** Measured: white pill (255,255,255) on canvas = 1.09:1; the darkest shadow pixel (232,232,234) = 1.12:1. This is not a WCAG 1.4.11 failure, because the label (16.83:1) identifies the control. On a phone in sunlight, though, the pill reads as text on the background. This is a DESIGN.md decision; changing it is not the writer's job. I am raising it for the owner.

### [FYI] F3. DESIGN.md:118 still requires an avatar with a lock or checkmark
- **Location:** DESIGN.md:118
- **Evidence:** None of the variants has an avatar, which follows brief item 4. Update DESIGN.md:118 together with the W3 change before editing SignInScreen.kt.

### [FYI] F4. The PNGs carry LCD subpixel colour fringes
- **Location:** render.py:141-146 (Chrome flags)
- **Evidence:** v1 pixel (585,1405) is (255,247,220), a yellow fringe inside the CTA label. That is not a token colour; it comes from Chrome's LCD antialiasing, and `-webkit-font-smoothing` (render.py:37) has no effect on Linux.
- **Fix suggestion:** Add `--disable-lcd-text` to the Chrome arguments so the PNGs contain only greyscale antialiasing.

### [FYI] F5. Unused tokens and helper in render.py
- **Location:** render.py:11-16 (`icon()`), :24 `--ink-soft`, :26 `--accent`, :27 `--success`, :28 `--hairline`, :42 `.caption`
- **Evidence:** No variant uses them. They do no harm, but `--accent` sitting in BASE invites it back in.

### [FYI] F6. Budgets verified per screen
- **Font sizes:** v1 32/13/15, v2 20/32/15, v3 13/32/15, v4 20/32/15. Three sizes each, within the four allowed by DESIGN.md:46.
- **Shadows:** one per screen, the CTA's (render.py:46). Accent: none. Radius: only 999.
- **Icons:** none, so the lucide rule is satisfied by default.
- **Content:** no Palma/Палма visible. No eyebrow text, caps labels, emoji, animals or logos. No hints or explanatory lines.
- **Names:** match the brief exactly.
- **Layout:** at most 3 blocks per screen (DESIGN.md:54).
- **Overlays:** none at first paint (§1.8).

## Real-device risk per variant

- **v1:** In landscape (390 tall) the push spacers collapse, and the getty block (about 142px plus the CTA) still fits. The risk is dark theme: muted mono needs its own dark value (DESIGN.md:10).
- **v2:** At 1.3x font size, a longer name wraps "Ubuntu + XFCE" (Display) onto two lines. Allow `maxLines = 2`. On a fresh install the state shown must be the real one (strings.xml:9), not `Запущена`.
- **v3:** See N4 (overflow at 1.07x font scale with the current lines) and N1 (wrong source).
- **v4:** Four Display lines (144px) + Title + CTA + 2x48 insets come to about 380px. Landscape (about 360 usable) scrolls, which breaks "без прокрутки". With more catalog entries from the server the stack grows.

## Verdict

**REVISION** (BLOCK = 0, WARN = 6 > 3).

All three round-1 BLOCKs are RESOLVED, so there is no BLOCK escalation to the orchestrator. The gate can pass to owner selection with these known issues:

1. **N1:** v3 should quote `app/src/main/assets/palma-session:15/483/484`, not build.sh. This one is my round-1 mistake. It also clears N4.
2. **N2 / N3:** v2 and v4 share one skeleton, and the v4 highlight reads as a picker.
3. **W3:** if v1 or v3 is chosen, change DESIGN.md:44 (and :118, see F3) before implementation.
4. **W1:** the contrast tool is missing from the repo. The pixel-measured numbers all pass.
