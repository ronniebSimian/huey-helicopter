#!/usr/bin/env python3
"""
Generates the Huey's 3D model and texture.

Outputs:
  src/client/java/io/github/ronniebsimian/huey/client/HueyModelGeometry.java
  src/main/resources/assets/huey/textures/entity/huey.png

Run from the project root:  python3 tools/gen_model.py

Model coordinates are Minecraft entity-model pixels (16 px = 1 block):
  +x = helicopter's LEFT, -x = RIGHT
  +y = DOWN (y = 0 is the ground, so "up" is negative)
  +z = BACK (the nose points toward -z)
Seat positions in HueyEntity.java use the same numbers divided by 16.
"""
import os
import random
from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
JAVA_OUT = os.path.join(ROOT, "src/client/java/io/github/ronniebsimian/huey/client/HueyModelGeometry.java")
PNG_OUT = os.path.join(ROOT, "src/main/resources/assets/huey/textures/entity/huey.png")

# ---------------------------------------------------------------- materials
MATERIALS = {
    "olive":    (84, 86, 52),    # FS 34087 olive drab, the Vietnam-era Army finish
    "olive_dk": (64, 66, 40),
    "glass":    (96, 128, 118),  # green-tinted Huey greenhouse glass
    "metal":    (112, 112, 104),
    "dark":     (44, 44, 42),
    "blade":    (38, 38, 36),
    "yellow":   (214, 176, 40),  # rotor tip warning paint
    "interior": (58, 60, 54),
    "canvas":   (104, 88, 58),   # troop seat canvas
    "soot":     (40, 36, 32),
    "gun":      (28, 28, 28),
    "red":      (150, 36, 30),
    "white":    (210, 210, 200),
}

# ---------------------------------------------------------------- geometry
# Each part: name -> (pivot (x,y,z), rotation (rx,ry,rz) radians, [boxes])
# Each box: (x0, y0, z0, w, h, d, material, decal) in coordinates RELATIVE to the part pivot
# (the "body" part pivots at 0,0,0, so its boxes are effectively absolute).
PARTS = {}


def part(name, pivot=(0, 0, 0), rot=(0, 0, 0)):
    PARTS[name] = (pivot, rot, [])
    return PARTS[name][2]


def box(lst, x0, y0, z0, x1, y1, z1, mat, decal=None):
    """Add a box given two corners, relative to the part's pivot."""
    lst.append((min(x0, x1), min(y0, y1), min(z0, z1), abs(x1 - x0), abs(y1 - y0), abs(z1 - z0), mat, decal))


body = part("body")

# --- landing skids
for s in (1, -1):
    box(body, 13 * s, -2, -34, 16 * s, 0, 24, "metal")          # skid tube
    box(body, 13 * s, -4, -38, 16 * s, -2, -34, "metal")        # upturned toe
    box(body, 12 * s, -10, -26, 14 * s, -2, -24, "metal")       # front strut
    box(body, 12 * s, -10, 12, 14 * s, -2, 14, "metal")         # rear strut
    box(body, 16 * s, -3, -8, 17 * s, -1, -4, "dark")           # step

# --- fuselage floor / belly
box(body, -16, -12, -40, 16, -9, 28, "olive")
# --- roof
box(body, -16, -46, -30, 16, -43, 28, "olive")
# green tinted overhead windows above the cockpit
box(body, -15, -46, -42, 15, -44, -30, "glass")
# --- rear cabin wall
box(body, -16, -43, 26, 16, -12, 28, "interior")
# --- cabin side posts behind the open doors
for s in (1, -1):
    box(body, 15 * s, -43, 24, 16 * s, -12, 26, "olive")       # rear door post
    box(body, 15 * s, -43, -16, 16 * s, -12, -14, "olive")     # front door post
    # cockpit door: lower panel + big window
    box(body, 15 * s, -26, -40, 16 * s, -12, -16, "olive")
    box(body, 15 * s, -43, -38, 16 * s, -26, -16, "glass")
    box(body, 15 * s, -43, -40, 16 * s, -26, -38, "olive")     # windshield pillar

# --- nose
box(body, -15, -26, -46, 15, -12, -40, "olive")                # lower nose
box(body, -12, -24, -49, 12, -14, -46, "olive")                # nose cap
box(body, -10, -20, -50, 10, -15, -49, "dark")                 # nose vent
box(body, -13, -43, -42, 13, -26, -40, "glass")                # windshield
box(body, -1, -43, -43, 1, -26, -40, "olive")                  # windshield center post
box(body, -12, -30, -40, 12, -26, -37, "dark")                 # instrument panel glare shield
for s in (1, -1):                                              # chin windows
    box(body, 4 * s, -14, -47, 12 * s, -12, -41, "glass")

# --- pilot & copilot armored seats
for s in (1, -1):
    box(body, 4 * s, -17, -30, 12 * s, -12, -22, "interior")   # cushion
    box(body, 4 * s, -33, -22, 12 * s, -17, -20, "olive_dk")   # armored back

# --- troop bench (faces forward) and gunner seats
box(body, -9, -17, 12, 9, -12, 18, "canvas")
box(body, -9, -30, 24, 9, -17, 26, "canvas")                   # bench back rest
for s in (1, -1):
    box(body, 9 * s, -16, 17, 14 * s, -12, 23, "interior")     # gunner seat
    box(body, 16.5 * s, -26, 19.5, 17.5 * s, -12, 20.5, "metal")  # M60 pintle post

# --- engine cowling on the roof
box(body, -11, -56, -14, 11, -46, 34, "olive")
box(body, -9, -54, -16, 9, -48, -14, "dark")                    # intake screen
box(body, -4, -54, 34, 4, -46, 40, "soot")                      # exhaust
box(body, -1.5, -62, 0, 1.5, -56, 3, "metal")                   # rotor mast

# --- tail boom (tapers toward the back)
box(body, -7, -44, 28, 7, -28, 52, "olive", decal="star")
box(body, -5, -43, 52, 5, -31, 94, "olive", decal="army")
box(body, -3.5, -42, 94, 3.5, -33, 112, "olive")
box(body, -14, -38, 78, 14, -36, 86, "olive")                   # horizontal stabilizers
box(body, -0.5, -33, 110, 0.5, -27, 113, "metal")               # tail skid
box(body, -1, -54, 104, 1, -42, 116, "olive")                   # vertical fin
box(body, -1, -62, 108, 1, -54, 119, "olive")                   # fin top
box(body, 1, -50, 111, 2.5, -46, 115, "dark")                   # tail rotor gearbox

# --- main rotor (spins around its pivot on top of the mast)
main_rotor = part("main_rotor", pivot=(0, -62, 1.5))
box(main_rotor, -3, -3, -3, 3, 0, 3, "dark")                    # hub
for s in (1, -1):
    box(main_rotor, 3 * s, -2, -2.5, 76 * s, -1, 2.5, "blade")
    box(main_rotor, 76 * s, -2, -2.5, 82 * s, -1, 2.5, "yellow")
box(main_rotor, -0.5, -4, -18, 0.5, -3, 18, "metal")           # Bell stabilizer bar
for s in (1, -1):
    box(main_rotor, -1, -4.5, 18 * s, 1, -2.5, 21 * s, "dark")  # bar weights

# --- tail rotor (spins around the x axis, mounted on the LEFT of the fin)
tail_rotor = part("tail_rotor", pivot=(3, -48, 113))
box(tail_rotor, -0.5, -1.5, -1.5, 1.5, 1.5, 1.5, "dark")
for s in (1, -1):
    box(tail_rotor, 0, 1.5 * s, -1.5, 0.8, 13 * s, 1.5, "blade")
    box(tail_rotor, 0, 13 * s, -1.5, 0.8, 15 * s, 1.5, "red")

# --- M60 door guns on their pintle posts. Each gun is modelled pointing forward (-z) and the
# part is turned 90 degrees to face out of its door; HueyModel then swings it to follow the gunner.
import math as _m
for name, s_ in (("gun_left", 1), ("gun_right", -1)):
    g = part(name, pivot=(17 * s_, -27, 20), rot=(0, -s_ * _m.pi / 2, 0))
    box(g, -1, -1, 0, 1, 1, 4, "gun")                  # stock
    box(g, -1, -1.5, -10, 1, 1, 0, "gun")              # receiver
    box(g, -0.5, -0.5, -19, 0.5, 0.5, -10, "gun")      # barrel
    box(g, -1.5, 1, -6, 1.5, 4, -2, "olive_dk")        # ammo can
    box(g, -0.25, -1.5, -18, 0.25, -0.5, -17, "gun")   # front sight

# ---------------------------------------------------------------- UV packing
def uv_size(w, h, d):
    import math
    w, h, d = math.ceil(w), math.ceil(h), math.ceil(d)
    return 2 * (d + w), d + h


boxes = []
for pname, (pivot, rot, lst) in PARTS.items():
    for b in lst:
        boxes.append((pname, b))

# Shelf packer, tallest first
order = sorted(range(len(boxes)), key=lambda i: -uv_size(*boxes[i][1][3:6])[1])
TEX_W = 512
x = y = shelf_h = 0
uv = {}
for i in order:
    bw, bh = uv_size(*boxes[i][1][3:6])
    if x + bw > TEX_W:
        x, y, shelf_h = 0, y + shelf_h, 0
    uv[i] = (x, y)
    x += bw
    shelf_h = max(shelf_h, bh)
TEX_H = 1
while TEX_H < y + shelf_h:
    TEX_H *= 2

# ---------------------------------------------------------------- texture
random.seed(1968)
img = Image.new("RGBA", (TEX_W, TEX_H), (0, 0, 0, 0))
px = img.load()

TINY_FONT = {  # 3x5 pixel font, just the letters we need
    "U": ["101", "101", "101", "101", "111"], "S": ["111", "100", "111", "001", "111"],
    "A": ["010", "101", "111", "101", "101"], "R": ["110", "101", "110", "101", "101"],
    "M": ["10001", "11011", "10101", "10001", "10001"], "Y": ["101", "101", "010", "010", "010"],
    ".": ["0", "0", "0", "0", "1"], " ": ["00", "00", "00", "00", "00"],
}


def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c)


def fill_face(x0, y0, w, h, color, factor, border=True):
    for yy in range(y0, y0 + h):
        for xx in range(x0, x0 + w):
            n = random.uniform(0.94, 1.05)
            f = factor * n
            if border and w >= 6 and h >= 6 and (xx in (x0, x0 + w - 1) or yy in (y0, y0 + h - 1)):
                f *= 0.82  # panel line
            px[xx, yy] = shade(color, f) + (255,)


def draw_text(x0, y0, text, color):
    cx = x0
    for ch in text:
        glyph = TINY_FONT[ch]
        for gy, row in enumerate(glyph):
            for gx, bit in enumerate(row):
                if bit == "1":
                    px[cx + gx, y0 + gy] = color + (255,)
        cx += len(glyph[0]) + 1
    return cx - x0


def text_width(text):
    return sum(len(TINY_FONT[c][0]) + 1 for c in text) - 1


def draw_star(cx, cy, r, color):
    import math
    pts = []
    for k in range(10):
        ang = -math.pi / 2 + k * math.pi / 5
        rr = r if k % 2 == 0 else r * 0.42
        pts.append((cx + rr * math.cos(ang), cy + rr * math.sin(ang)))
    d = ImageDraw.Draw(img)
    d.ellipse([cx - r - 1, cy - r - 1, cx + r + 1, cy + r + 1], fill=color + (255,))
    d.polygon(pts, fill=shade(MATERIALS["olive"], 1.0) + (255,))


import math
for i, (pname, b) in enumerate(boxes):
    _, _, _, w, h, d, mat, decal = b
    W, H, D = math.ceil(w), math.ceil(h), math.ceil(d)
    u, v = uv[i]
    c = MATERIALS[mat]
    glassy = mat == "glass"
    fill_face(u + D, v, W, D, c, 1.12, not glassy)               # top (down face)
    fill_face(u + D + W, v, W, D, c, 0.70, not glassy)           # bottom
    fill_face(u, v + D, D, H, c, 0.95, not glassy)               # right side (-x)
    fill_face(u + D, v + D, W, H, c, 1.00, not glassy)           # front (-z)
    fill_face(u + D + W, v + D, D, H, c, 0.95, not glassy)       # left side (+x)
    fill_face(u + D + W + D, v + D, W, H, c, 0.90, not glassy)   # back (+z)
    if glassy:  # a little glint so glass reads as glass
        for fx, fw in ((u, D), (u + D + W, D), (u + D, W)):
            for k in range(min(fw, H) // 2):
                xx, yy = fx + 1 + k, v + D + 1 + k
                if xx < fx + fw and yy < v + D + H:
                    px[xx, yy] = shade(c, 1.35) + (255,)
    marking = (24, 24, 20)  # subdued black Vietnam-era markings
    if decal == "army":
        tw = text_width("U.S. ARMY")
        for fx in (u, u + D + W):  # both side faces
            draw_text(fx + (D - tw) // 2, v + D + (H - 5) // 2, "U.S. ARMY", marking)
    if decal == "star":
        for fx in (u, u + D + W):
            draw_star(fx + D // 2, v + D + H // 2, 5, marking)

os.makedirs(os.path.dirname(PNG_OUT), exist_ok=True)
img.save(PNG_OUT)

# ---------------------------------------------------------------- java
def f(v):
    s = ("%.3f" % v).rstrip("0").rstrip(".")
    if s in ("-0", ""):
        s = "0"
    return s + "F"


lines = []
lines.append("package io.github.ronniebsimian.huey.client;")
lines.append("")
lines.append("import net.minecraft.client.model.geom.PartPose;")
lines.append("import net.minecraft.client.model.geom.builders.CubeListBuilder;")
lines.append("import net.minecraft.client.model.geom.builders.LayerDefinition;")
lines.append("import net.minecraft.client.model.geom.builders.MeshDefinition;")
lines.append("import net.minecraft.client.model.geom.builders.PartDefinition;")
lines.append("")
lines.append("/**")
lines.append(" * GENERATED by tools/gen_model.py - do not edit by hand.")
lines.append(" * Edit the Python script and re-run it instead.")
lines.append(" */")
lines.append("public final class HueyModelGeometry {")
lines.append("\tprivate HueyModelGeometry() {")
lines.append("\t}")
lines.append("")
lines.append("\tpublic static LayerDefinition create() {")
lines.append("\t\tMeshDefinition mesh = new MeshDefinition();")
lines.append("\t\tPartDefinition root = mesh.getRoot();")
idx = 0
for pname, (pivot, rot, lst) in PARTS.items():
    lines.append(f"\t\troot.addOrReplaceChild(\"{pname}\", CubeListBuilder.create()")
    for b in lst:
        x0, y0, z0, w, h, d, mat, decal = b
        u, v = uv[idx]
        idx += 1
        lines.append(f"\t\t\t.texOffs({u}, {v}).addBox({f(x0)}, {f(y0)}, {f(z0)}, {f(w)}, {f(h)}, {f(d)})")
    if any(rot):
        pose = f"PartPose.offsetAndRotation({f(pivot[0])}, {f(pivot[1])}, {f(pivot[2])}, {f(rot[0])}, {f(rot[1])}, {f(rot[2])})"
    else:
        pose = f"PartPose.offset({f(pivot[0])}, {f(pivot[1])}, {f(pivot[2])})"
    lines.append(f"\t\t\t, {pose});")
lines.append(f"\t\treturn LayerDefinition.create(mesh, {TEX_W}, {TEX_H});")
lines.append("\t}")
lines.append("}")

os.makedirs(os.path.dirname(JAVA_OUT), exist_ok=True)
with open(JAVA_OUT, "w") as fh:
    fh.write("\n".join(lines) + "\n")

print(f"{len(boxes)} boxes, texture {TEX_W}x{TEX_H}")
