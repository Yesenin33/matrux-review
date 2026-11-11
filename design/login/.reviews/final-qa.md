# Final QA — round 1

**Date:** 2026-11-10T19:56:13Z
**Artifacts:** `/root/palma/design/login/v1.png` `v2.png` `v3.png` `v4.png` `all.png`; source `/root/palma/design/login/src/render.py` (161 lines) and the generated `src/v1..v4.html`
**DESIGN.md read at:** 2026-11-10T19:56:13Z (`/root/palma/design/DESIGN.md`, 124 lines, mtime 19:51:41Z; I re-read it this round and did not cache it)
**Brief:** `/root/palma/docs/TASK-LOGIN-DESIGN.md` (50 lines)
**Prior reviews:** `.reviews/round-1.md`, `.reviews/round-2.md` (designer-review, round 2: 0 BLOCK / 6 WARN)
**Rubric:** the closed 8-item acceptance checklist from the orchestrator's brief. This is a mockup lane, so it replaces the document rubric in SKILL.md §1. Each item is binary PASS or FAIL.
**Method:**
- Pixel checks:
  - `magick` histograms.
  - Chroma max(R,G,B)-min(R,G,B) for every pixel.
  - Pixel counts of exact token colours.
  - Threshold bounding boxes.
  - ASCII dumps of the mono glyphs.
- Source checks: line refs into render.py, the brief, strings.xml, palma-session and SignInScreen.kt.

## Rubric

| # | Item | Verdict | Evidence |
|---|---|---|---|
| 1 | Sizes, 4 PNG + all.png without captions | PASS | `file`: v1–v4 are each 1170 x 2532 RGB. render.py:141 asserts this. all.png is 1800 x 940, 4 frames in a row on (255,255,255). The top band y0–47 and bottom band y892–939 have 2 unique colours each (white and the frame edge). No text is outside the frames. |
| 2 | VK ID button is the primary action; string matches strings.xml | PASS | `Продолжить с VK ID` (render.py:52, 72, 93, 114) = strings.xml:160 `sign_in_vk`. It is the only `<button>` in each variant. Pill bbox is 1050 x 168 px at x=60 in all four frames, i.e. 350 x 56 CSS. It sits on `--surface` with `--shadow` (render.py:32), as DESIGN.md:118 requires. |
| 3 | Name Matrux; no Palma/Палма/палма visible | PASS | Visible brand text is only `Matrux` (render.py:50, 65, 92, 105) = strings.xml:3 `app_name`, plus `matrux login:` (render.py:51). `/root/palma` occurs only inside `file://` font URLs (src/v*.html:11-12), which are never rendered. There is no `<title>`. |
| 4 | No hints, captions, arrows or explanatory lines | PASS | Every text node is one of four kinds: brand, real system name, real state (strings.xml:161), or a verbatim session line. None of them is an instruction like «Выбери…» or «После входа…» (brief:38-39). There are no arrows, no icons, and no `.caption` class in BASE (render.py:10-36). |
| 5 | No animals, faces, masks, foreign logos, emoji, gradients or blobs | PASS | Max chroma per pixel is 6 in v1–v3 and 3 in v4. 0 pixels exceed chroma 6, so the frames are fully neutral: no colour artwork, no LCD fringe (`--disable-lcd-text`, render.py:129), no `ic_vk` logo. The only soft ramp is the `--shadow` token under the pill. `картинка-ассет` and `картинка-ассет` (SignInScreen.kt:76, 116) are absent. |
| 6 | No invented data | PASS | v2 `Alpine + XFCE` / `Запущена` (render.py:68-69) = brief:17 and strings.xml:161. v4 shows the four names exactly as brief:17 lists them (render.py:109-112). v3 matches palma-session verbatim; only the 2-space leading indent of the shell block is dropped: `:15 export XDG_RUNTIME_DIR=/tmp/runtime-root`, `:483 export DISPLAY=:0`, `:484 exec dbus-run-session -- xfce4-session`. v1 `matrux login:` is the login prompt the brief allows (brief:16). It is not a system name or state string. |
| 7 | Tokens only; ≤1 accent, ≤1 shadow, ≤4 sizes, no eyebrow/caps; margins 20, button 56, radii from scale | PASS | See the token breakdown below this table. |
| 8 | Four genuinely different techniques, not a redraw of SignInScreen.kt | **FAIL** | v2 maps one-to-one onto SignInScreen.kt:92-97 with the avatar deleted. See the detail below. |

### Item 7: token evidence

- **Colours:** the hex values in render.py:12-15 are 4 DESIGN.md tokens (:15, :16, :18, :20). Measured pixels are only those greys and their antialiasing.
- **Accent:** `#0071e3` is not used. Measured: 0 pixels.
- **Shadow:** one `box-shadow`, `.cta` at render.py:32, which equals DESIGN.md:52.
- **Font sizes per screen (DESIGN.md:40-44):**

  | Screen | Sizes |
  |---|---|
  | v1 | 32 / 13 / 15 |
  | v2 | 20 / 32 / 15 |
  | v3 | 13 / 32 / 15 |
  | v4 | 20 / 32 / 15 |

- **Margins:**
  - Side margin is 20 (render.py:25). The text bbox left edge is 61–63 px, i.e. 20 CSS plus side bearing.
  - Pill bottom is at 2388 px, so 144 px = 48 CSS to the frame edge, i.e. 24 block plus 24 inset.
  - Spacing values used are 8 / 24 / 48 only (render.py:34-35, 43-44, 61, 81, 101).
- **Radius:** only 999 (render.py:32), from DESIGN.md:51.
- **Caps:** `XFCE`, `VK ID` and `XDG_RUNTIME_DIR` are proper names or literal script text, not labels. No eyebrow.

## Failed items detail

### [8] v2 is the current sign-in screen with the avatar removed

- **Location:** render.py:64-73 against SignInScreen.kt:92-97 and :154-161.
- **Evidence (verbatim):**
  - render.py:66-72:
    ```
    <div class="push"></div>
    <div class="sys">
      <h1 class="display">Alpine + XFCE</h1>
      <p class="body muted">Запущена</p>
    </div>
    <div class="push"></div>
    <div class="foot"><button class="cta">Продолжить с VK ID</button></div>
    ```
  - SignInScreen.kt:92-97:
    ```
    Spacer(Modifier.weight(1f))
    Avatar(ui.signedIn)
    Spacer(Modifier.height(space.block))
    SystemState(ui.system)
    Spacer(Modifier.weight(1f))
    VkButton(ui.enabled, onSignIn)
    ```
- **How v2 maps onto SignInScreen.kt:**

  | SignInScreen.kt | v2 (render.py) | Change |
  |---|---|---|
  | `Spacer(weight 1f)` | `.push` :66 | identical |
  | `Avatar` | — | deleted |
  | `SystemLines`: name, then 8, then `sign_in_running` Body muted (:155-161, :141) | `.sys`, `gap: 8px`, name, then `Запущена` Body muted (:61, :67-70) | identical |
  | `Spacer(weight 1f)` | `.push` :71 | identical |
  | `VkButton`: surface pill, shadow | `.cta` :72 | identical |

  The other differences are cosmetic:
  - The name is set in Display instead of Title.
  - The alignment is start instead of centre.
  - The art and avatar are removed.
  - A Title `Matrux` head is added (:65).

  That is a restyle of the current composition, which brief:4 forbids: «Состав окна придумать заново, а не перерисовывать текущий экран входа». Round-1 W7 raised this. Round 2 marked it resolved only because the monitor icon was removed; nobody re-checked the composition afterwards.
- **Second issue in the same item:** the round-2 N2 fix moved v4 off v2's skeleton and onto v3's.
  - v3 (render.py:85-94) is: small head text at the 48 inset, push, foot group of Display + 24 + pill.
  - v4 (render.py:105-115) is the same: small head text at the 48 inset, push, foot group of Display + 24 + pill.
  - In all.png the last Display line of v3 and v4 sits on the same band (about y746 in the sheet), and the pill sits on the same band (y788–843) in v2, v3 and v4.
  - v3 and v4 are still distinguishable by technique: a mono transcript against a 144 px Display stack. So this alone does not fail the item, but it limits the options for the v2 fix.
- **Fix direction:**
  - Rebuild v2 so that its element order is not push / name+state / push / pill, i.e. it no longer reproduces SignInScreen.kt:92-97.
  - Its skeleton must also not match v3/v4 (top head, bottom Display + pill) or v1 (one centred group).
  - Possible directions, keeping the name and state real:
    - The live state as a single full-width line set directly on the pill's top edge.
    - The system name pinned to the top inset as the dominant element, with the CTA as the only lower element and no centred group.
  - Re-render all.png after the change.

## Carried known issues (not rubric FAILs; owner decisions)

- **K1 (accepted, from the orchestrator):** v1 `matrux login:` (render.py:51) uses Code mono outside DESIGN.md:44 «Только строки запуска гостя, код в уроках и командах шпаргалок». DESIGN.md:44 has to be extended before implementation if v1 is chosen. v3 is arguably inside that scope, because the lines are verbatim guest launch lines (palma-session:15/483/484). But they appear on the sign-in screen, not during a live launch, so put both in the same DESIGN.md edit.
- **K2:** `.code` (render.py:29) omits `font-feature-settings: "liga" 0, "calt" 0`, which DESIGN.md:44 requires. mono.ttf is JetBrains Mono, which has `liga`/`calt` tables. In the current pixels v3's ` -- ` renders as two separate dashes (ASCII dump of v3 y268–328: `####....####`), so the PNGs comply. A Compose implementation must still set the features off.
- **K3:** DESIGN.md:118 still prescribes «аватар с замком или галочкой». No variant has an avatar, which follows brief:4/22. Owner must edit DESIGN.md:118 together with K1 before touching SignInScreen.kt.
- **K4 (designer W1):** the repo contrast tool is absent. Measured by pixel instead:
  - muted (110,110,115) on canvas (245,245,247) = 4.66:1. Present in v1 (1532 px), v2 (1627 px) and v3 (7794 px).
  - v4 has 0 muted pixels: all names are ink.
  - The CTA label is ink on white, 16.83:1.

## Verdict

**REVISION.** Round 1 of 2 has 1 FAIL (item 8, v2). In the orchestrator's PASS/BLOCK terms this is **not PASS**: send it back to the design executor to recompose v2 only. Items 1–7 pass with the evidence above. If item 8 still fails in round 2, the verdict is BLOCK.
