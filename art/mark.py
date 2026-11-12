"""Tally, the app's mark: the single definition of its identity.

Four strokes and a strike through them, drawn here in code (docs/phase-0.md, *The mark: Tally*).
Both generators import it (`make-launcher-icon.py` emits it as `VectorDrawable` path data,
`make-feature-graphic.py` rasterises it with PIL), so the icon and the listing cannot drift into
being two different marks.

Coordinates are in arbitrary units around an origin at the mark's centre; callers fit them to their
own canvas with `fit()`. y grows downward, as it does in both SVG and PIL.
"""

import math
import re
from dataclasses import dataclass
from pathlib import Path

ROOT = Path(__file__).parent.parent


def color_kt():
    """The generated `theme/Color.kt`, found rather than spelled out so a namespace rename cannot break it."""
    found = sorted((ROOT / "app/src/main/java").rglob("theme/Color.kt"))
    if not found:
        raise SystemExit("no app/src/main/java/**/theme/Color.kt — run scripts/gen_scheme.py first")
    return found[0]


def scheme(name, *roles):
    """Colour roles read out of `theme/Color.kt`'s `light` or `dark` scheme, as (r, g, b)."""
    path = color_kt()
    body = path.read_text().split(f"{name}ColorScheme(", 1)[-1].split("\n    )", 1)[0]
    out = []
    for role in roles:
        found = re.search(rf"\b{role} = Color\(0x[0-9A-Fa-f]{{2}}([0-9A-Fa-f]{{6}})\)", body)
        if not found:
            raise SystemExit(f"{path}: no `{role}` in the {name} scheme — has it been renamed?")
        h = found.group(1)
        out.append(tuple(int(h[i:i + 2], 16) for i in (0, 2, 4)))
    return out


# The only copy of the icon's two colours: make-launcher-icon.py writes every drawable from them.
GROUND, MARK_INK = scheme("light", "primary", "surface")


@dataclass(frozen=True)
class Subpath:
    """One closed contour, as cubic path data (`M` then `C` segments only).

    `d` is always wound CLOCKWISE on screen (positive shoelace area with y downward). A hole is
    stored the same way and reversed at read time by `nodes()`, so the non-zero fill rule that SVG
    and VectorDrawable use subtracts it. Tally has no hole; the support stays for a traced mark.
    """

    d: str
    hole: bool = False


BAR_X = (-54, -18, 18, 54)
BAR_WIDTH = 18
BAR_REACH = 60
BAND = 34
STRIKE_THICKNESS = 18
STRIKE_LENGTH = 190
ANGLE = math.radians(24)

_COS, _SIN = math.cos(ANGLE), math.sin(ANGLE)
_ALONG = (_COS, -_SIN)
_ACROSS = (_SIN, _COS)


class _Pen:
    """Accumulates a contour as cubic segments: lines as degenerate cubics, arcs as true ones."""

    def __init__(self, start):
        self.pts = [start]

    def line(self, q):
        p = self.pts[-1]
        self.pts += [
            (p[0] + (q[0] - p[0]) / 3, p[1] + (q[1] - p[1]) / 3),
            (p[0] + (q[0] - p[0]) * 2 / 3, p[1] + (q[1] - p[1]) * 2 / 3),
            q,
        ]

    def arc(self, centre, r, a0, a1):
        """A clockwise-on-screen arc from angle a0 to a1 (degrees), split into quarter turns."""
        steps = max(1, round(abs(a1 - a0) / 90))
        for i in range(steps):
            s = math.radians(a0 + (a1 - a0) * i / steps)
            e = math.radians(a0 + (a1 - a0) * (i + 1) / steps)
            k = 4 / 3 * math.tan((e - s) / 4) * r
            p0 = (centre[0] + r * math.cos(s), centre[1] + r * math.sin(s))
            p3 = (centre[0] + r * math.cos(e), centre[1] + r * math.sin(e))
            self.pts += [
                (p0[0] - k * math.sin(s), p0[1] + k * math.cos(s)),
                (p3[0] + k * math.sin(e), p3[1] - k * math.cos(e)),
                p3,
            ]

    def subpath(self, tf=lambda p: p):
        def f(p):
            x, y = tf(p)
            return f"{round(x, 4):g},{round(y, 4):g}"

        n = self.pts
        return Subpath(f"M{f(n[0])}" + "".join(
            "C" + " ".join(f(n[i + j]) for j in range(3)) for i in range(1, len(n), 3)
        ) + "Z")


def _band_edge_y(x, side):
    """y where the band's upper (side=-1) or lower (side=+1) edge crosses the vertical line at x."""
    return (side * BAND / 2 - x * _SIN) / _COS


def _bar_pieces(cx):
    r = BAR_WIDTH / 2
    left, right = cx - r, cx + r
    cap = BAR_REACH - r

    top = _Pen((left, -cap))
    top.arc((cx, -cap), r, 180, 360)
    top.line((right, _band_edge_y(right, -1)))
    top.line((left, _band_edge_y(left, -1)))
    top.line((left, -cap))

    bottom = _Pen((left, _band_edge_y(left, 1)))
    bottom.line((right, _band_edge_y(right, 1)))
    bottom.line((right, cap))
    bottom.arc((cx, cap), r, 0, 180)
    bottom.line((left, _band_edge_y(left, 1)))

    return top.subpath(), bottom.subpath()


def _strike():
    """A stadium drawn level around the origin, then turned onto the band's angle."""
    r = STRIKE_THICKNESS / 2
    half = STRIKE_LENGTH / 2 - r
    pen = _Pen((-half, -r))
    pen.line((half, -r))
    pen.arc((half, 0), r, -90, 90)
    pen.line((-half, r))
    pen.arc((-half, 0), r, 90, 270)
    return pen.subpath(lambda p: (
        p[0] * _ALONG[0] + p[1] * _ACROSS[0],
        p[0] * _ALONG[1] + p[1] * _ACROSS[1],
    ))


PARTS = (*(piece for cx in BAR_X for piece in _bar_pieces(cx)), _strike())

_NUM = re.compile(r"-?\d*\.?\d+(?:e-?\d+)?")


def _points(d):
    """Path data -> the flat control-point list: 1 start point plus 3 per cubic segment."""
    pts = []
    for chunk in re.finditer(r"[MC]([^MCZz]*)", d):
        nums = [float(v) for v in _NUM.findall(chunk.group(1))]
        pts += list(zip(nums[0::2], nums[1::2]))
    return pts


def nodes(e, tf=lambda p: p):
    """Control points, caller-transformed, wound for fill or hole."""
    pts = _points(e.d)
    if e.hole:
        pts.reverse()
    return [tf(p) for p in pts]


def path_data(e, tf=lambda p: p, precision=2):
    """SVG/VectorDrawable path data for one subpath."""
    n = nodes(e, tf)

    def f(p):
        return f"{round(p[0], precision):g},{round(p[1], precision):g}"

    out = [f"M{f(n[0])}"]
    for i in range(1, len(n), 3):
        out.append("C" + " ".join(f(n[i + j]) for j in range(3)))
    return "".join(out) + "Z"


def outline(e, tf=lambda p: p, steps=32):
    """The same subpath flattened to a polygon, for renderers that can't draw curves."""
    n = nodes(e, tf)
    pts = []
    for i in range(1, len(n), 3):
        (x0, y0), (x1, y1), (x2, y2), (x3, y3) = n[i - 1], n[i], n[i + 1], n[i + 2]
        for s in range(steps):
            t = s / steps
            u = 1 - t
            pts.append((
                u * u * u * x0 + 3 * u * u * t * x1 + 3 * u * t * t * x2 + t * t * t * x3,
                u * u * u * y0 + 3 * u * u * t * y1 + 3 * u * t * t * y2 + t * t * t * y3,
            ))
    return pts


def bounds(parts=PARTS):
    """Bounding box of the silhouette. Holes are ignored — a hole cannot extend the mark."""
    xs, ys = [], []
    for e in parts:
        if e.hole:
            continue
        for x, y in outline(e):
            xs.append(x)
            ys.append(y)
    return min(xs), min(ys), max(xs), max(ys)


def fit(canvas, art_size, parts=PARTS):
    """A transform placing the mark centred on a square canvas at a given longest-side size.

    Returns a function mapping mark coordinates to canvas coordinates.
    """
    x0, y0, x1, y1 = bounds(parts)
    scale = art_size / max(x1 - x0, y1 - y0)
    cx, cy = (x0 + x1) / 2, (y0 + y1) / 2
    return lambda p: (canvas / 2 + (p[0] - cx) * scale, canvas / 2 + (p[1] - cy) * scale)
