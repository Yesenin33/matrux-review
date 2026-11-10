# Final QA — round 2

**Date:** 2026-11-10T20:01:09Z
**Artifacts:** `/root/palma/design/login/v1.png` `v2.png` `v3.png` `v4.png` `all.png`; source `/root/palma/design/login/src/render.py` (161 lines, mtime 20:00:25Z) and the generated `src/v2.html`
**DESIGN.md read at:** 2026-11-10T20:01:09Z (`/root/palma/design/DESIGN.md`, 124 lines, mtime 19:51:41Z, unchanged since round 1; I read it again in full this round and did not reuse the round-1 copy)
**Brief:** `/root/palma/docs/TASK-LOGIN-DESIGN.md`
**Prior reviews:** `.reviews/round-1.md`, `.reviews/round-2.md` (designer-review), `.reviews/final-qa.md` (final-qa round 1: REVISION, item 8)
**Rubric:** the same closed 8-item mockup checklist as round 1. No items were added or removed.
**Scope:** only v2 was rebuilt. v1, v3 and v4 have the same pixel counts as in round 1 (muted pixels: v1 1532, v3 7794, v4 0; ink pixels: v1 12562), so their round-1 evidence still holds. I re-measured them anyway.

## Rubric

| # | Item | Verdict | Evidence |
|---|---|---|---|
| 1 | Sizes, 4 PNG + all.png without captions | PASS | v1–v4 are each 1170 x 2532 RGB (PIL), and render.py:141 asserts this. all.png is 1800 x 940. Its top band y0–47 has 1 colour (255,255,255). Its bottom band y892–939 has 2 colours: white and the frame edge (210,210,215). There is no caption text. v2.png is 47,879 B. |
| 2 | VK ID button is the primary action; string matches strings.xml | PASS | v2 `Продолжить с VK ID` (render.py:72) = strings.xml:160 `sign_in_vk`, and it is the only `<button>`. The white pill bbox is x60–1108, y2220–2386: about 1050 x 168 px, i.e. 350 x 56 CSS, the same as v1/v3/v4. |
| 3 | Name Matrux; no Palma/Палма/палма visible | PASS | The visible brand text is `Matrux` (render.py:67) = strings.xml:3 `app_name`. Grepping v2.html for `palma`/`палма` only hits the `file:///root/palma/...` font URLs, which are not rendered. |
| 4 | No hints, captions, arrows or explanatory lines | PASS | v2 has exactly four text nodes: `Matrux`, `Alpine + XFCE`, `Запущена` and the button label (render.py:67-72). None of them is an instruction. There is no icon and no arrow. |
| 5 | No animals, faces, masks, foreign logos, emoji, gradients or blobs | PASS | Max per-pixel chroma in v2 is 6 (v4: 3). The frame is fully neutral. v2.html contains no `gradient`, no `<img>` and no `penguin_*`. The only soft ramp is the `--shadow` token under the pill. |
| 6 | No invented data | PASS | `Alpine + XFCE` = brief:17. `Запущена` = strings.xml:161 `sign_in_running`, which is the string SignInScreen.kt:141 shows for `GateSystem.Running`. No numbers, dates or other values appear. |
| 7 | Tokens only; ≤1 accent, ≤1 shadow, ≤4 sizes, no eyebrow/caps; margins 20, button 56, radii from scale | PASS | See the v2 token breakdown below this table. |
| 8 | Four genuinely different techniques, not a redraw of SignInScreen.kt | PASS | See the item 8 analysis below. |

### Item 7: v2 token evidence

- **Colours:** v2.html uses only the hex values `#f5f5f7`, `#ffffff`, `#1d1d1f` and `#6e6e73`. These are DESIGN.md:15, :16, :18 and :20.
  - Measured pixels: ink (29,29,31) 12,562; muted (110,110,115) 16,455; white 163,674. The other 585 colours are greyscale antialiasing.
- **Accent:** `#0071e3` has 0 pixels.
- **Shadow:** there is one `box-shadow`, `.cta` at render.py:32, and it equals DESIGN.md:52.
- **Font sizes:** v2 uses two sizes, 32 (Display, DESIGN.md:40) and 15 (Body and the pill label, DESIGN.md:42). The limit is 4.
- **Spacing:**
  - Side margin is 20 (render.py:25). The text bbox left edge is x60–65 px, which is 20 CSS plus side bearing.
  - The head inset is 48 (render.py:34) and the foot inset is 48 (render.py:35). Pill bottom 2386 means 146 px to the frame edge.
  - The gap between the two Display lines is 0, by line boxes: `Matrux` band y163–233 sits in the 144–252 line box, and `Alpine + XFCE` band y269–360 sits in the 252–360 line box.
  - The gap above `Запущена` is 8 (render.py:62); its band is y400–442.
  - All of these values are on the 8 grid.
- **Radius:** only 999 (DESIGN.md:51).
- **Caps:** `XFCE` and `VK ID` are proper names, not labels. There is no eyebrow and no `text-transform`.
- **Contrast** (WCAG relative luminance, computed from token RGB):
  - muted on canvas = 4.66:1. This passes AA for normal text (4.5) as well as large text (3.0). The muted Display line is 32px/600, so it is large text and has 1.66 to spare.
  - `Запущена` (Body 15) is also muted and passes at 4.66 ≥ 4.5.
  - ink on canvas = 15.46:1. Ink on white (pill label) = 16.83:1.

### Item 8: analysis

**v2 against SignInScreen.kt:92-97**

The current screen is: `Spacer(weight 1f)` / `Avatar` / `Spacer(block)` / `SystemState` / `Spacer(weight 1f)` / `VkButton`. Its focal group is centred vertically between two flexible spaces, and `SystemLines` (:154-161) is a centred Title name, then 8, then a Body muted state.

New v2 (render.py:65-73) is: `.head.pair` (pinned at the 48 top inset) / `.push` / `.foot` pill. The changes are:

- **No leading flexible space.** The focal block is anchored to the top edge, so the screen is a top/bottom split, not a centred focal group. This removes exactly the push / group / push order that round 1 quoted.
- **Brand and system share one headline.** `Matrux` (ink) and `Alpine + XFCE` (muted) are two Display lines with zero gap (render.py:67-68). This is a two-tone typographic headline. The current screen has no brand text and sets the system name in Title. Here the system name is a headline weight, separated only by tone.
- **No avatar, no status dot, start alignment.** The state is a trailing Body line under the headline, not a separate centred row.

Changing the order, the anchoring and the type role together is a new composition, not a restyle. The content overlaps with the current screen (system name, `Запущена`, pill), but the brief requires that overlap: brief:17/22 allow only real names and states, and DESIGN.md:118 requires the pill. Overlapping content therefore does not make v2 a redraw.

**v2 against the other three variants**

| Variant | Skeleton (render.py) | Dominant element and its position | Technique |
|---|---|---|---|
| v1 | push / centred group (Display + Code prompt + pill) / push (:47-55) | Display name, vertical centre, with the pill inside the group | getty login prompt |
| v2 | top group (Display ink + Display muted + Body) / push / pill alone (:65-73) | two-line Display headline, top inset | two-tone headline: brand plus live system |
| v3 | top Code transcript / push / foot (Display + 24 + pill) (:84-95) | Display name, bottom, above the pill; the top is small mono | verbatim session launch lines |
| v4 | top Title / push / foot (four Display names + 24 + pill) (:104-116) | 144 px Display stack, bottom | catalogue of real system names |

- v2 is the only variant whose large type sits in the upper third: Display bands y163–360 of 2532.
- v3 and v4 put their Display at the bottom: the all.png sheet shows bands at about y746 and y638–760.
- v1 centres its Display at about y416 of 940 in the sheet.
- v2 is also the only variant whose pill stands alone in the lower region with no type above it.

Round 1's fix direction (final-qa.md, «The system name pinned to the top inset as the dominant element, with the CTA as the only lower element and no centred group») is what render.py:65-73 implements. No two of the four share both the skeleton and the dominant type position.

**Residual (not a FAIL)**

v2, v3 and v4 all end in a bottom-anchored pill at the same band (sheet y788–843). The VK ID pill is the one fixed element every variant must have (brief:2), so placing it at the thumb zone is a shared constraint, not a shared technique.

## Failed items detail

None. All 8 items PASS.

## Carried known issues (owner decisions, not rubric FAILs; carried from final-qa.md)

- **K1:** v1 `matrux login:` and v3's launch lines use Code outside the DESIGN.md:44 scope («Только строки запуска гостя, код в уроках и командах шпаргалок»). If v1 or v3 is chosen, extend DESIGN.md:44 first. v2 does not use Code, so it is not affected.
- **K2:** `.code` (render.py:29) has no `font-feature-settings: "liga" 0, "calt" 0`. The PNGs are unaffected. A Compose implementation must disable the features. This does not apply to v2.
- **K3:** DESIGN.md:118 still prescribes «аватар с замком или галочкой». All four variants drop the avatar, as brief:4 asks. The owner must edit DESIGN.md:118 before SignInScreen.kt is changed. This applies to v2 too.
- **K4 (v2-specific, implementation risk):**
  - v2 sets the system name in Display. A long name, or a large system font scale, wraps the muted line onto 2+ lines at 350 CSS width. The current names fit on one line: `Alpine + XFCE` reaches x661 of 1110.
  - With `GateSystem.Stopped`, the state reads `Не запущена` (strings.xml:162).
  - With `GateSystem.Absent`, there is no name line (SignInScreen.kt:146), so the headline collapses to `Matrux` alone.
  - The owner should see these states before choosing v2.

## Verdict

**PASS.** Round 2 of 2: 0 FAIL out of 8. Item 8, which failed in round 1, is resolved. v2 is now a top-anchored two-tone headline with no centred group between flexible spaces, so it is neither SignInScreen.kt:92-97 nor a copy of v1, v3 or v4. Items 1–7 hold for the new v2 by measured pixels and source line refs. Ready for owner handoff, with K1–K4 recorded as known issues.
