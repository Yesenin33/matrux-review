# Designer review — round 1

**Date:** 2026-11-10T19:46:19Z
**DESIGN.md re-read at:** 2026-11-10T19:46:19Z (`/root/palma/design/DESIGN.md`, 124 lines)
**Artifact:** `/root/palma/design/login/v1.png` `v2.png` `v3.png` `v4.png` `all.png`; source `/root/palma/design/login/src/render.py` (183 lines)
**Also checked:** `/root/palma/CLAUDE.md`, `/root/palma/docs/TASK-LOGIN-DESIGN.md`, `app/src/main/java/ru/palma/app/shell/Proot.kt`, `image-build/build.sh`, `app/src/main/java/ru/palma/app/ui/screens/SignInScreen.kt`, `app/src/main/res/values/strings.xml`
**Viewport:** mobile (390x844 @3x)

## Summary

- BLOCK: 3
- WARN: 8
- FYI: 6

## Checks that pass (with evidence)

- Colours: every hex in render.py:21-29 is in DESIGN.md:15-27. The one literal, `color: #fff` (render.py:49), is `--surface`. No gradients, blobs, or images.
- Radii: 999 (render.py:49) and 12 (render.py:122). Both are tokens (DESIGN.md:51).
- Font sizes per screen: v1 = 32/13/15, v2 = 20/32/15, v3 = 13/32/15, v4 = 32/13/15. Each screen uses 3 sizes or fewer, so the 4-size limit holds (DESIGN.md:46).
- Shadows: v4 has one (render.py:123). v1–v3 have none (DESIGN.md:107).
- Icons: lucide only (render.py:7). Stroke is 1.75 in the output (`stroke-width="1.75"` in src/v2.html and src/v4.html). Sizes are 24 (render.py:85) and 20 (render.py:131), as DESIGN.md:90 requires.
- Name: "Matrux" matches strings.xml:3. No visible Palma/Палма in render.py. `/root/palma` appears only inside `file://` URLs (render.py:33-34).
- Strings: "Продолжить с VK ID" and "Запущена" match strings.xml:160-161. "Debian 12" is a real name (ManifestTest.kt:18), and an Arch image exists (`image-build/arch-rootfs/`).
- No eyebrow text, no caps labels. "XFCE" and "VK ID" are proper names. No penguins, animals, emoji, or VK logo.
- Hit areas: the CTA is 56 (render.py:49) and v4 rows are 48 (render.py:122).
- §1.8 Overlay occlusion: no variant has an overlay at first paint at 390 wide.

## Issues

### [BLOCK] B1. CTA is an accent fill, but DESIGN.md specifies a white pill with a shadow (all 4 variants)
- **Location:** render.py:49-50; DESIGN.md:118
- **Rule:** DESIGN.md:118: "одна кнопка «Продолжить с VK ID» (пилюля, поверхность `--surface`, одна тень)". The brief says "Токены и запреты брать из DESIGN.md … макет ему не противоречит". CLAUDE.md says "ТОЛЬКО из значений design/DESIGN.md".
- **Evidence:** `.cta { … background: var(--accent); color: #fff; …}`. The PNGs show a #0071e3 pill at y≈1763–1895 px (@3x display coordinates) on every frame. The live screen also uses `.background(colors.surface)` with `cardShadow` (SignInScreen.kt:187-188).
- **Fix suggestion:** Choose one of two:
  - (a) Make `.cta` use `background: var(--surface); color: var(--ink); box-shadow: var(--shadow)`. In v4 this means the selected row has to lose its shadow (one shadow per screen).
  - (b) The orchestrator rewrites DESIGN.md:118 first ("Вход: акцентная пилюля") and the variants are re-rendered afterwards.
  
  Without one of these, the mockups contradict the source of truth.

### [BLOCK] B2. v2 status text "Запущена" fails contrast (4.02:1)
- **Location:** render.py:87 (`<p class="body ok">Запущена</p>`), render.py:46 (`.ok { color: var(--success); }`)
- **Rule:** §1.7: body text < 4.5:1 is BLOCK. This is 15px / 400, so it is not large text.
- **Evidence:** I computed the WCAG ratio for #1d8a4e on #f5f5f7: **4.02:1**. Both fills are flat token colours with no image or gradient. The check tool was not available (see W1).
- **Fix suggestion:** Use `--muted` (4.66:1), as the live screen does (SignInScreen.kt:141 `color = colors.muted`), or `--ink-soft` (7.26:1). If green is the point, keep `--success` only on a 20px lucide `badge-check` icon (3:1 is enough for icons) and set the word in muted.

### [BLOCK] B3. v4 puts the system catalog and a second action on the sign-in screen
- **Location:** render.py:121-134
- **Rule:** The brief says "Вход единственный — кнопка VK ID". DESIGN.md:112 says "Каталог — единственное место про системы". DESIGN.md:118 allows "живое состояние системы", meaning one system, not a list.
- **Evidence:** `<div class="row on code"><span>Alpine + XFCE</span><span class="ic">{icon("chevron-right", 20)}</span></div>` plus 3 more 48px rows. The chevron and the white raised row read as a tappable menu item (v4.png, y≈758–872), which makes it a second action. On a real install, Debian, Ubuntu and Arch need Pro (gen-mockup.py:309 `"pro"`), so before sign-in the list offers choices the user cannot make.
- **Fix suggestion:** Remove `chevron-right` and the raised `.on` state. Show the four names as one passive mono line, e.g. `Alpine + XFCE · Debian 12 · Ubuntu + XFCE · Arch + XFCE`, with no row affordance. Or replace the technique entirely.

### [WARN] W1. Contrast tool not run, so these numbers are formula-computed, not a pass
- **Location:** `test-v2/tools/text-contrast.mjs` (not in the repo); `omd` is not on PATH
- **Rule:** §1.7: an unrun check must be reported, not counted as a pass.
- **Evidence:** I applied the WCAG 2.x formula to the token hex values. This is valid only because every surface is a flat colour.

  | Pair | Ratio |
  |---|---|
  | white on accent | 4.70 |
  | muted on canvas | 4.66 |
  | success on canvas | 4.02 |
  | ink-soft on canvas | 7.26 |
  | ink on canvas | 15.46 |
  | ink on surface | 16.83 |

  muted on canvas is the 13px Code text in v1/v3/v4 (render.py:65, 104-106, 132-134). It passes with 0.16 to spare.
- **Fix suggestion:** Re-run with the real tool once it is available. Do not lighten `--muted` any further for mono text.

### [WARN] W2. v3 launch lines are reassembled from real fragments, not quoted
- **Location:** render.py:104-107; Proot.kt:45-56; build.sh:31,37
- **Rule:** Brief: "Никаких выдуманных данных", and the launch lines must come from the real sources.
- **Evidence:**
  - `$ proot --link2symlink --sysvipc` cuts an argv that Kotlin builds; nobody types it at a shell. The real argv is `--kill-on-exit --link2symlink --sysvipc -0 -r <rootfs> … /bin/sh <SESSION_SCRIPT> vnc` (Proot.kt:46-56).
  - `$ Xvfb :0 -screen 0 1280x800x24` drops `-nolisten tcp … &` (build.sh:31).
  - `exec dbus-run-session startxfce4` (build.sh:37) replaces the shell, so the next line, `$ ▌` (render.py:107), cannot happen.
  - The `$` prompt also suggests an interactive root shell, which the session does not have.
- **Fix suggestion:** Drop the `$ ` prefixes and the trailing prompt line. Quote the lines exactly as in build.sh:31 and build.sh:37 (redirect and `&` may be trimmed only at the end). Or show only `exec dbus-run-session startxfce4` as the last line, with no caret.

### [WARN] W3. Code (mono) font used outside its allowed scope
- **Location:** render.py:65 (v1 `matrux login:`), render.py:131-134 (v4 system names)
- **Rule:** DESIGN.md:44: "Только строки запуска гостя, код в уроках и командах шпаргалок". The brief asks for "моноширинный набор, приглашение входа", which conflicts with this.
- **Evidence:** `<p class="code"><span class="muted">matrux login:</span>…` and `<div class="row on code"><span>Alpine + XFCE</span>`.
- **Fix suggestion:** If the owner keeps mono for a login prompt or system names, add that scope to DESIGN.md:44 first, per the CLAUDE.md rule "сначала добавь его в DESIGN.md". Otherwise set v4 names in Body.

### [WARN] W4. Off-token spacing
- **Location:** render.py:51, 58, 75, 77, 97, 120, 121, 122
- **Rule:** DESIGN.md:50: unit 8, page margins 20, block gap 24, card padding 20.
- **Evidence:**

  | Value | Location | Problem |
  |---|---|---|
  | `padding-bottom: 44px` | :51 | not a multiple of 8 |
  | `gap: 12px` | :58 | not a spacing token |
  | `padding-top: 64px` | :75, :97, :120 | not a token |
  | `margin-bottom: 16px` | :77 | not a token |
  | `gap: 4px` | :121 | not a multiple of 8 |
  | `padding: 0 16px` | :122 | card padding is 20 |

  The live screen uses `navigationBarsPadding()` + `padding(bottom = space.block)` (SignInScreen.kt:87-89).
- **Fix suggestion:** Use 24 for the bottom (on top of the nav-bar inset), 24 between blocks, 8 for tight pairs, and 20 for row padding. Treat the top as status-bar inset + 24 and stop hardcoding 64.

### [WARN] W5. CTA states are not specified
- **Location:** render.py:49-50
- **Rule:** §1.4 says missing focus/active/disabled is BLOCK. I downgraded it to WARN because these are static PNG mockups and the HTML never ships. The live button already has press scale and disabled alpha (SignInScreen.kt:170-185).
- **Evidence:** No `:focus-visible`, `:active` or `[disabled]` rules exist for `.cta`.
- **Fix suggestion:** When implementing, keep SignInScreen.kt:170-185 (press `PRESS_SCALE`, `DISABLED_ALPHA`) and add a focus ring of at least 3:1 against canvas. Accent on canvas is 4.31, which is enough.

### [WARN] W6. v1 and v3 use the same technique
- **Location:** render.py:47 (`.caret`), used at render.py:65 and render.py:107; render.py:64 and render.py:111 (Display "Matrux")
- **Rule:** Brief: the variants must be genuinely different techniques.
- **Evidence:** Both are a mono prompt with a block caret, Display "Matrux" and a bottom pill. v3 is v1 with three extra lines, and the name moved to the bottom (all.png, frames 1 and 3).
- **Fix suggestion:** Give the caret to one variant only. For example, v1 keeps the prompt, and v3 becomes a passive session transcript with no caret and no `$` (see W2).

### [WARN] W7. v2 uses an abstract monitor glyph for the system and stays close to the current screen
- **Location:** render.py:85 (`icon("monitor", 24)`); DESIGN.md:91; SignInScreen.kt:141,156
- **Rule:** DESIGN.md:91: "Иконка дистрибутива — реальный логотип дистрибутива, не абстрактная «шестерёнка»". The brief bans foreign logos, so a real distro logo is not an option either. Brief item 4: "не перерисовывать текущий экран входа".
- **Evidence:** v2.png has a 24px monitor at x≈50–100, y≈853–900. The composition is system name + state + VK pill, which is SignInScreen's `SystemState` (Title name + Body state) without the avatar, left-aligned and enlarged.
- **Fix suggestion:** Remove the monitor icon; this also cuts one element. To move v2 further from the current screen, make the system name the only large element, drop "Matrux" from the Title slot, or change what the state shows.

### [WARN] W8. v1 `matrux login:` with a caret looks like a text field
- **Location:** render.py:65
- **Rule:** Brief: "только то, что реально будет на экране"; one action only.
- **Evidence:** A getty-style prompt plus a block caret (v1.png, y≈920–955) tells the user to type. Tapping it will do nothing, because the only input is the VK button.
- **Fix suggestion:** Drop the caret, or make the prompt read as already completed. Do not imply a field that does not exist.

### [FYI] F1. DESIGN.md:118 still prescribes an avatar with a lock or checkmark
- **Location:** DESIGN.md:118
- **Evidence:** None of the four variants has an avatar, which is correct under brief item 4. DESIGN.md:118 still prescribes one.
- **Fix suggestion:** Whichever variant is chosen, update DESIGN.md:118 (and §1.1/B1) before editing SignInScreen.kt.

### [FYI] F2. The source of truth still names the product Palma
- **Location:** DESIGN.md:1, DESIGN.md:8 ("Palmа", with a Cyrillic "а")
- **Evidence:** This is not visible in the mockups, but it is the source of truth.
- **Fix suggestion:** Rename in a separate task.

### [FYI] F3. All four variants share one skeleton
- **Location:** render.py:38, 48, 51
- **Evidence:** Same 20 margin, same `.push` spacers, same full-width pill at an identical position (all.png, every frame). Only the middle block differs.

### [FYI] F4. A blinking caret would break motion rules
- **Location:** render.py:47
- **Evidence:** The caret is static. If it blinks on device, that is an infinite animation (DESIGN.md:98) and it needs a reduced-motion variant (DESIGN.md:97). Keep it static.

### [FYI] F5. v2 shows only the best-case state
- **Location:** render.py:86-87
- **Evidence:** v2 shows "Запущена". On a fresh install the real sign-in state is "Система не установлена" (strings.xml:9) or the installing percentage (SignInScreen.kt:147). The layout must hold for all of them.

### [FYI] F6. Overflow is hidden from the PNGs
- **Location:** render.py:36 (`overflow-x: clip`)
- **Evidence:** Horizontal overflow is clipped, so it cannot be detected from the PNGs. This matters for W2/v3 under large fonts.

## Real-device risk per variant

- **v1:** Dark theme. `--muted` mono text and an `--ink` block caret on canvas need a separate dark build (DESIGN.md:10), and the caret must not invert to a bright block. The large empty space above and below also turns into an off-centre void in landscape (844 wide, 390 tall), where the `.push` spacers collapse.
- **v2:** Long system name. "Ubuntu + XFCE" at Display 32 already fills about 60% of the width. With a longer future name or 1.3x system font it wraps to two Display lines. Allow `maxLines = 2` and check that the state line still fits above the pill.
- **v3:** Large system font. `white-space: pre` (render.py:98) on `exec dbus-run-session startxfce4` (34 chars ≈ 265px at 13px) overflows 350px at about 1.3x font scale. It gets clipped instead of wrapping. Use soft wrap or ellipsis.
- **v4:** Rotation. Four 48px rows + Display + 56 pill + insets need about 420dp of height. Landscape (about 360dp usable) forces scrolling, which violates "без прокрутки". At large font sizes the 48px rows also clip their text.

## Verdict

**BLOCK** (BLOCK = 3).

1. B1: make the CTA a white pill with one shadow, as in DESIGN.md:118, or amend DESIGN.md:118 first.
2. B2: v2 state text at 4.02:1 is below 4.5:1.
3. B3: v4 shows a system list with a second action.

Round 2 should also clear W2 (fabricated `$` transcript, impossible prompt after `exec`) and W6 (v1/v3 share a technique).
