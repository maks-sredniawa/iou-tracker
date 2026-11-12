# Identity assets

Source art for the launcher icon and the Play listing. **Everything in `app/src/main/res` that draws
the mark is generated from here** — edit `mark.py` and regenerate, never hand-edit the path data in
the `VectorDrawable`s.

| Asset | Source | State |
| --- | --- | --- |
| `mark.py`'s `PARTS` | Tally, geometric, drawn in code (docs/phase-0.md, *The mark: Tally*) | final |
| `drawable/ic_launcher_{background,foreground,monochrome}.xml`, `drawable/ic_notification.xml`, `mipmap-*`, `play-icon-512.png` | `mark.py` → `make-launcher-icon.py` | generated |
| `play-feature-graphic.png`, `play-feature-graphic-<tag>.png` (1024×500, one per translated listing; `COPY`) | `mark.py` + `theme/Color.kt` (+ `feature-background.png` if present) → `make-feature-graphic.py` | generated |
| `play-screenshots/<n>_<scene>-<tag>.png` | `scripts/screenshots.py --build release --numbered` → `pad-screenshot.py` | — |

## The mark

The mark is **Tally**: four strokes and a strike through them. `mark.py` builds it from its
measurements as nine clockwise cubic subpaths — each bar split in two by the strike's band, and the
strike itself — with no hole and no overlap. Both generators import it, so the launcher icon, the
notification icon and the feature graphic cannot drift into being different marks.

- **Its colours are read out of `theme/Color.kt`**: `GROUND` is the light scheme's `primary`,
  `MARK_INK` its `surface`, and the feature graphic takes its roles from the dark scheme. A
  regenerated palette reaches every asset by re-running the two scripts; pasted values would stay
  the old hue on a ground that moved.
- **The gap between the strike and the bars must stay empty**, never painted in the ground's
  colour. A themed launcher (API 33+) tints the whole monochrome layer one flat colour, and a
  painted gap simply vanishes. It is 8 units, about 1px at the notification icon's 24dp on an mdpi
  screen, so it blurs there and is clean from xhdpi up.
- `trace-mark.py` stays as a tool for a traced mark: paste its curves as `Subpath`s into `PARTS`.

## Where the art has to come from

**Use art you have the right to ship.** This is not a formality — it is the thing most likely to
block a first Play upload, and it is discovered late.

- An icon traced from an **emoji font** carries that font's licence. Noto Emoji is Apache-2.0 or
  OFL depending on the release, and both impose obligations on the *outlines* — which is what
  tracing copies.
- A **stock icon** is somebody's asset with somebody's terms, whatever the site's marketing says.
- **Art you drew, or art generated from your own prompt, is clean.** Keep a note of which, in this
  file, so the provenance survives you forgetting it.

Rendering *text* with a font is a different question from redistributing its *outlines*: the OFL
explicitly permits documents produced with a font. `make-feature-graphic.py` renders the wordmark
with Noto Sans on that basis and ships no glyph outlines.

## Regenerating

```bash
python3 art/make-launcher-icon.py       # icons, mipmaps, the 512² Play asset
python3 art/make-feature-graphic.py     # the 1024×500 listing graphics
```

Run both after any change to `mark.py` or to the palette.

`make-launcher-icon.py` prints a **safe-radius check** and will tell you when the art is clipped by
a circular launcher mask. It is a real constraint rather than a formality: a launcher may mask the
icon to a 66dp circle, so anything outside a 33dp radius from the centre is cut off. Shrink
`ART_SIZE` until it says `inside`.

Two sizes worth knowing, both already handled:

- The **notification icon** is scaled up relative to the launcher icon. At the launcher's own art
  size the mark would be a speck in a 24dp slot.
- The **flat mipmaps** are cropped to the 72dp a launcher actually shows of the 108dp adaptive
  canvas, so they match what the adaptive icon looks like rather than appearing to shrink.

## The feature graphic and Play's crop

Play shows the 1024×500 graphic in a **16:9 box with `object-fit: cover`** on the search-results
card, so 68px of each side never reaches a viewer. It is correct in every local preview and in the
Console's uploader, and wrong only on the surface most people see. `make-feature-graphic.py` lays out
against the cropped frame, prints the spans it measured, and **exits 1** when the copy is clipped or
runs into the mark — shorten the tagline rather than moving the margin.

A photographic ground is `art/feature-background.png` (cover-fitted, never stretched). Record where
it came from in the provenance table below.

## Provenance

| File | Where it came from | Licence / terms |
| --- | --- | --- |
| `mark.py` (Tally) | Designed in this repo and drawn in code from its measurements, chosen 2026-11-11. No traced or third-party art. | The app's own licence |
| `feature-background.png` | *(if used)* | |
