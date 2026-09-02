#!/usr/bin/env python3
"""
Generates Mango TV's launcher icon and Fire TV banner.

The mark is a single amber disc with a thin arc cut near its right edge,
leaving a detached sliver: an aperture read and a fruit read at once. It is
purely geometric so it stays crisp from a 48px launcher tile up to the
320x180 banner, and it carries no gradient, so it compresses to almost
nothing.

Run from the repository root:  python3 tools/generate_brand.py
Requires Pillow. Output is committed, so this only needs re-running when the
mark changes.
"""

import os
from PIL import Image, ImageDraw, ImageFont

SURFACE = (11, 11, 12, 255)      # surface_base  #0B0B0C
ACCENT = (217, 160, 91, 255)     # accent        #D9A05B
TEXT = (242, 239, 233, 255)      # text_primary  #F2EFE9

SS = 4  # supersampling factor; edges are downsampled with LANCZOS

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "app", "src", "main", "res")

FONT_CANDIDATES = [
    "/mnt/skills/examples/canvas-design/canvas-fonts/InstrumentSans-Medium.ttf",
    "/mnt/skills/examples/canvas-design/canvas-fonts/InstrumentSans-Regular.ttf",
    "/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf",
    "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
]


def load_font(size):
    for path in FONT_CANDIDATES:
        if os.path.exists(path):
            return ImageFont.truetype(path, size)
    return ImageFont.load_default()


def draw_mark(size, background=None):
    """
    The mark alone, rendered at `size` px square.

    Composed from three circles on a mask: a disc, a second disc subtracted
    from it to leave a crescent, and a smaller disc set into the void with a
    clear gap. The result reads as an aperture and as a cut fruit at once, and
    stays legible down to a 48px launcher tile because it is only silhouette -
    no gradient, no stroke weight to disappear.

    Geometry is expressed in units of the crescent's radius so the proportions
    hold at every density.
    """
    s = size * SS
    pad = s * 0.09
    r = (s - 2 * pad) / 2.0

    # Normalised layout, in units of r. Bounding box spans x in [-1.00, 0.96].
    crescent_cut_dx = 0.46   # offset of the disc that carves the crescent
    inner_dx = 0.28          # centre of the set-in disc
    inner_r = 0.68

    cx = pad + r - (s - 2 * pad - (1.96 * r)) * 0 + (0.02 * r)
    cy = s / 2.0

    def circle(d, dx, radius, value):
        x, rr = cx + dx * r, radius * r
        d.ellipse([x - rr, cy - rr, x + rr, cy + rr], fill=value)

    mask = Image.new("L", (s, s), 0)
    md = ImageDraw.Draw(mask)
    circle(md, 0.0, 1.0, 255)                 # the disc
    circle(md, crescent_cut_dx, 1.0, 0)       # carve it into a crescent
    circle(md, inner_dx, inner_r, 255)        # set a smaller disc into the void

    img = Image.new("RGBA", (s, s), background or (0, 0, 0, 0))
    amber = Image.new("RGBA", (s, s), ACCENT)
    img.paste(amber, (0, 0), mask)
    return img.resize((size, size), Image.LANCZOS)


def write_icons():
    # Density buckets for a 48dp launcher icon.
    for folder, px in [
        ("mipmap-mdpi", 48),
        ("mipmap-hdpi", 72),
        ("mipmap-xhdpi", 96),
        ("mipmap-xxhdpi", 144),
        ("mipmap-xxxhdpi", 192),
    ]:
        out_dir = os.path.join(RES, folder)
        os.makedirs(out_dir, exist_ok=True)
        tile = Image.new("RGBA", (px, px), SURFACE)
        tile.alpha_composite(draw_mark(px, background=SURFACE))
        tile.convert("RGB").save(
            os.path.join(out_dir, "ic_launcher.png"), "PNG", optimize=True
        )
        print(f"  {folder}/ic_launcher.png  {px}x{px}")


def tracked_text(draw, xy, text, font, fill, tracking):
    """PIL has no letter-spacing, so glyphs are placed individually."""
    x, y = xy
    for ch in text:
        draw.text((x, y), ch, font=font, fill=fill)
        x += draw.textlength(ch, font=font) + tracking
    return x


def text_width(draw, text, font, tracking):
    return sum(draw.textlength(c, font=font) for c in text) + tracking * (len(text) - 1)


def write_banner():
    """Fire TV banner: 320x180dp, supplied at xhdpi."""
    w, h = 320 * SS, 180 * SS
    img = Image.new("RGBA", (w, h), SURFACE)
    d = ImageDraw.Draw(img)

    mark_px = int(h * 0.40)
    mark = draw_mark(mark_px, background=SURFACE)
    mark_x, mark_y = int(w * 0.10), int((h - mark_px) / 2)
    img.alpha_composite(mark, (mark_x, mark_y))

    font = load_font(int(h * 0.135))
    tracking = h * 0.022
    baseline_y = int(h / 2 - font.size * 0.62)
    text_x = mark_x + mark_px + int(w * 0.055)
    end = tracked_text(d, (text_x, baseline_y), "MANGO", font, TEXT, tracking)

    small = load_font(int(h * 0.135))
    tracked_text(d, (end + tracking * 2, baseline_y), "TV", small, ACCENT, tracking)

    out_dir = os.path.join(RES, "drawable-xhdpi")
    os.makedirs(out_dir, exist_ok=True)
    img.resize((320, 180), Image.LANCZOS).convert("RGB").save(
        os.path.join(out_dir, "tv_banner.png"), "PNG", optimize=True
    )
    print("  drawable-xhdpi/tv_banner.png  320x180")


if __name__ == "__main__":
    print("Generating brand assets:")
    write_icons()
    write_banner()
    print("Done.")
