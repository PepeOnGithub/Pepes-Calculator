"""Generates the launcher icon from the Font Awesome Free 6.5.2 "calculator" glyph (CC BY 4.0):
a green-gradient calculator mark on the same black rim-lit badge as Pepe's Notes / Pepe's Clock /
Pepe's Weather, for a consistent family look.

Badge recipe (checked against a reference squircle-badge treatment):
  - one light source, from directly above, applied consistently to every surface and shadow;
  - the badge is a top-to-bottom linear gradient (a lit dome), not a gradient from a corner;
  - the bevel is a thin rim light traced along the actual visible edge of the badge shape
    (rect or circle), bright at the top and fading to nothing by mid-height;
  - the glyph gets a soft drop shadow cast straight down (matching the overhead light).

Writes: adaptive background/foreground/monochrome vectors, legacy WebP mipmaps (API 24-25) and
the Play Store PNG. Run from anywhere: python tools/make_icons.py
"""
import io
import os
import re

import cairosvg
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(HERE, "..", "app", "src", "main", "res")


def fa_path(name):
    with open(os.path.join(HERE, f"fa-{name}.svg"), encoding="utf-8") as f:
        return re.search(r'<path d="([^"]+)"', f.read()).group(1)


PATH = fa_path("calculator")  # viewBox 384 x 512
GS = 45.0 / 512.0  # smaller, more margin — full-size content was cropping the rim on some launchers
GX = 54.0 - 384.0 * GS / 2.0
GY = 54.0 - 512.0 * GS / 2.0

SHADOW = [(0.6, 1.6, 0.30), (1.2, 3.0, 0.20), (2.0, 4.6, 0.12)]  # straight down: light is overhead

BG_HI, BG_MID, BG_LO = "FF2A2A2A", "FF141414", "FF000000"
BEVEL_DARK = "26003300"
GLYPH_HI, GLYPH_LO = "FF7ACB7B", "FF2E7D32"  # green gradient (brand accent)


def write(rel, text):
    p = os.path.join(RES, rel)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, "w", encoding="utf-8") as f:
        f.write(text)


HEADER = ('<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
          '    xmlns:aapt="http://schemas.android.com/aapt"\n'
          '    android:width="108dp" android:height="108dp"\n'
          '    android:viewportWidth="108" android:viewportHeight="108">\n')

# ---- background: lit-dome vertical gradient + bottom bevel rim -----------------------------
bg = HEADER
bg += (
    '    <path android:pathData="M0,0h108v108h-108z">\n'
    '        <aapt:attr name="android:fillColor">\n'
    '            <gradient android:startX="54" android:startY="0" android:endX="54" android:endY="108" android:type="linear">\n'
    f'                <item android:color="#{BG_HI}" android:offset="0.0" />\n'
    f'                <item android:color="#{BG_MID}" android:offset="0.5" />\n'
    f'                <item android:color="#{BG_LO}" android:offset="1.0" />\n'
    '            </gradient>\n'
    '        </aapt:attr>\n'
    '    </path>\n'
    f'    <path android:fillColor="#{BEVEL_DARK}" android:pathData="M0,104h108v4h-108z" />\n'
)
write("drawable/ic_launcher_background.xml", bg + "</vector>\n")

# ---- foreground: drop shadow + gradient calculator glyph -------------------------------------
fg = ('<!-- Calculator glyph: Font Awesome Free 6.5.2 (CC BY 4.0), https://fontawesome.com -->\n' + HEADER)
for dx, dy, a in SHADOW:
    alpha_hex = f"{int(round(a * 255)):02X}"
    fg += (
        f'    <group android:scaleX="{GS}" android:scaleY="{GS}" android:translateX="{GX + dx:.2f}" android:translateY="{GY + dy:.2f}">\n'
        f'        <path android:fillColor="#{alpha_hex}000000" android:pathData="{PATH}" />\n'
        '    </group>\n'
    )
fg += (
    f'    <group android:scaleX="{GS}" android:scaleY="{GS}" android:translateX="{GX}" android:translateY="{GY}">\n'
    f'        <path android:pathData="{PATH}">\n'
    '            <aapt:attr name="android:fillColor">\n'
    '                <gradient android:startX="0" android:startY="0" android:endX="384" android:endY="512" android:type="linear">\n'
    f'                    <item android:color="#{GLYPH_HI}" android:offset="0.0" />\n'
    f'                    <item android:color="#{GLYPH_LO}" android:offset="1.0" />\n'
    '                </gradient>\n'
    '            </aapt:attr>\n'
    '        </path>\n'
    '    </group>\n'
)
write("drawable/ic_launcher_foreground.xml", fg + "</vector>\n")

# ---- monochrome (themed icons): one tint colour ----------------------------------------------
mono = HEADER
mono += (
    f'    <group android:scaleX="{GS}" android:scaleY="{GS}" android:translateX="{GX}" android:translateY="{GY}">\n'
    f'        <path android:fillColor="#FFFFFFFF" android:pathData="{PATH}" />\n'
    '    </group>\n'
)
write("drawable/ic_launcher_monochrome.xml", mono + "</vector>\n")

write("mipmap-anydpi-v26/ic_launcher.xml", (
    '<?xml version="1.0" encoding="utf-8"?>\n'
    '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
    '    <background android:drawable="@drawable/ic_launcher_background" />\n'
    '    <foreground android:drawable="@drawable/ic_launcher_foreground" />\n'
    '    <monochrome android:drawable="@drawable/ic_launcher_monochrome" />\n'
    '</adaptive-icon>\n'
))
write("mipmap-anydpi-v26/ic_launcher_round.xml", (
    '<?xml version="1.0" encoding="utf-8"?>\n'
    '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
    '    <background android:drawable="@drawable/ic_launcher_background" />\n'
    '    <foreground android:drawable="@drawable/ic_launcher_foreground" />\n'
    '    <monochrome android:drawable="@drawable/ic_launcher_monochrome" />\n'
    '</adaptive-icon>\n'
))


def svg(size, shape):
    if shape == "round":
        clip, rim = '<circle cx="54" cy="54" r="54"/>', '<circle cx="54" cy="54" r="52.5"/>'
    else:
        clip, rim = '<rect width="108" height="108" rx="24" ry="24"/>', '<rect x="1.5" y="1.5" width="105" height="105" rx="22.5" ry="22.5"/>'
    shadows = "".join(
        f'<g transform="translate({GX + dx} {GY + dy}) scale({GS})"><path d="{PATH}" fill="#000" fill-opacity="{a}"/></g>'
        for dx, dy, a in SHADOW
    )
    return f"""<svg xmlns="http://www.w3.org/2000/svg" width="{size}" height="{size}" viewBox="0 0 108 108">
<defs>
<linearGradient id="bg" x1="54" y1="0" x2="54" y2="108" gradientUnits="userSpaceOnUse">
<stop offset="0" stop-color="#{BG_HI[2:]}"/><stop offset="0.5" stop-color="#{BG_MID[2:]}"/><stop offset="1" stop-color="#{BG_LO[2:]}"/>
</linearGradient>
<linearGradient id="rim" x1="54" y1="0" x2="54" y2="100" gradientUnits="userSpaceOnUse">
<stop offset="0" stop-color="#fff" stop-opacity="0.9"/><stop offset="0.45" stop-color="#fff" stop-opacity="0.4"/><stop offset="1" stop-color="#fff" stop-opacity="0"/>
</linearGradient>
<linearGradient id="glyph" x1="0" y1="0" x2="384" y2="512" gradientUnits="userSpaceOnUse">
<stop offset="0" stop-color="#{GLYPH_HI[2:]}"/><stop offset="1" stop-color="#{GLYPH_LO[2:]}"/>
</linearGradient>
<clipPath id="c">{clip}</clipPath></defs>
<g clip-path="url(#c)">
<rect width="108" height="108" fill="url(#bg)"/>
{shadows}
<g transform="translate({GX} {GY}) scale({GS})"><path d="{PATH}" fill="url(#glyph)"/></g>
{rim.replace("/>", ' fill="none" stroke="url(#rim)" stroke-width="6"/>')}
</g></svg>"""


def render(size, shape="square"):
    png = cairosvg.svg2png(bytestring=svg(size, shape).encode())
    return Image.open(io.BytesIO(png)).convert("RGBA")


for name, px in [("mdpi", 48), ("hdpi", 72), ("xhdpi", 96), ("xxhdpi", 144), ("xxxhdpi", 192)]:
    d = os.path.join(RES, f"mipmap-{name}")
    os.makedirs(d, exist_ok=True)
    render(px, "square").save(os.path.join(d, "ic_launcher.webp"), "WEBP", quality=95)
    render(px, "round").save(os.path.join(d, "ic_launcher_round.webp"), "WEBP", quality=95)

play = os.path.join(HERE, "..", "app", "src", "main", "ic_launcher-playstore.png")
render(512, "square").convert("RGB").save(play)
print("icons written")
