from PIL import Image, ImageDraw
import math

SIZE = 512
img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
draw = ImageDraw.Draw(img)

# --- rounded rect background ---
RADIUS = 96
BG = (28, 28, 36, 255)

def rounded_rect(d, x0, y0, x1, y1, r, fill):
    d.rectangle([x0 + r, y0, x1 - r, y1], fill=fill)
    d.rectangle([x0, y0 + r, x1, y1 - r], fill=fill)
    d.ellipse([x0, y0, x0 + 2*r, y0 + 2*r], fill=fill)
    d.ellipse([x1 - 2*r, y0, x1, y0 + 2*r], fill=fill)
    d.ellipse([x0, y1 - 2*r, x0 + 2*r, y1], fill=fill)
    d.ellipse([x1 - 2*r, y1 - 2*r, x1, y1], fill=fill)

rounded_rect(draw, 0, 0, SIZE, SIZE, RADIUS, BG)

# --- canvas area (light, centered) ---
CX, CY = SIZE // 2, SIZE // 2
CANVAS_W, CANVAS_H = 310, 310
cx0 = CX - CANVAS_W // 2
cy0 = CY - CANVAS_H // 2
cx1 = cx0 + CANVAS_W
cy1 = cy0 + CANVAS_H

# canvas bg with slight border
rounded_rect(draw, cx0 - 4, cy0 - 4, cx1 + 4, cy1 + 4, 10, (60, 62, 80, 255))
draw.rectangle([cx0, cy0, cx1, cy1], fill=(240, 240, 248, 255))

# --- pixel grid (16x16 grid on the canvas) ---
GRID = 16
CELL = CANVAS_W // GRID  # ~19px per cell

# checkerboard subtle background
for row in range(GRID):
    for col in range(GRID):
        if (row + col) % 2 == 0:
            px = cx0 + col * CELL
            py = cy0 + row * CELL
            draw.rectangle([px, py, px + CELL - 1, py + CELL - 1], fill=(230, 230, 240, 255))

# --- pixel art drawing on the canvas: a small cute character / smiley ---
# palette
COLORS = {
    'skin':  (255, 213, 170, 255),
    'eye':   (50,  50,  60,  255),
    'mouth': (200, 80,  80,  255),
    'hair':  (80,  55,  30,  255),
    'body':  (80,  120, 200, 255),
    'bg':    None,
    'shine': (255, 255, 255, 255),
}

def px(col, row, color_key):
    if COLORS[color_key] is None:
        return
    x = cx0 + col * CELL
    y = cy0 + row * CELL
    draw.rectangle([x + 1, y + 1, x + CELL - 2, y + CELL - 2], fill=COLORS[color_key])

# 16x16 pixel character centered in grid (columns 4-11, rows 2-13)
# head
for c in range(5, 11):
    px(c, 2, 'hair')
for c in range(4, 12):
    px(c, 3, 'hair')
for c in range(4, 12):
    px(c, 4, 'skin')
for c in range(4, 12):
    px(c, 5, 'skin')
for c in range(4, 12):
    px(c, 6, 'skin')
for c in range(4, 12):
    px(c, 7, 'skin')

# eyes
px(6, 5, 'eye')
px(9, 5, 'eye')
px(6, 5, 'shine')   # overwrite for shine pixel
draw.rectangle([cx0 + 6*CELL + 1, cy0 + 5*CELL + 1, cx0 + 6*CELL + 4, cy0 + 5*CELL + 4], fill=(255,255,255,255))
draw.rectangle([cx0 + 9*CELL + 1, cy0 + 5*CELL + 1, cx0 + 9*CELL + 4, cy0 + 5*CELL + 4], fill=(255,255,255,255))
px(6, 5, 'eye')
px(9, 5, 'eye')

# mouth / smile
px(6,  7, 'mouth')
px(7,  7, 'mouth')
px(8,  7, 'mouth')
px(9,  7, 'mouth')
px(5,  6, 'mouth')
px(10, 6, 'mouth')

# body
for c in range(5, 11):
    px(c, 8, 'body')
for c in range(4, 12):
    px(c, 9, 'body')
for c in range(4, 12):
    px(c, 10, 'body')
for c in range(4, 12):
    px(c, 11, 'body')

# arms
px(3, 9, 'body')
px(3, 10, 'body')
px(12, 9, 'body')
px(12, 10, 'body')

# legs
for c in [5, 6, 9, 10]:
    px(c, 12, 'body')
for c in [5, 6, 9, 10]:
    px(c, 13, 'skin')

# --- pencil tool icon in bottom-right corner ---
# Draw a stylized pencil outside the canvas
PX, PY = cx1 + 18, cy1 - 80  # top-left of pencil bounding box

def rr(d, x0, y0, x1, y1, fill):
    d.rectangle([x0, y0, x1, y1], fill=fill)

# pencil body (rotated 45°): draw as a diagonal strip
pencil_color = (255, 210, 60, 255)
tip_color    = (240, 200, 160, 255)
dark         = (60, 60, 70, 255)

for i in range(22):
    w = max(2, 14 - i // 2)
    x = PX + i * 2
    y = PY + i * 2
    c = tip_color if i > 16 else pencil_color
    draw.rectangle([x, y, x + w, y + w], fill=c)
draw.polygon([(PX + 44, PY + 42), (PX + 50, PY + 56), (PX + 56, PY + 50)], fill=(30, 30, 30, 220))

# --- grid lines on canvas (thin, subtle) ---
LINE_COLOR = (200, 200, 210, 180)
for i in range(GRID + 1):
    x = cx0 + i * CELL
    draw.line([(x, cy0), (x, cy1)], fill=LINE_COLOR, width=1)
    y = cy0 + i * CELL
    draw.line([(cx0, y), (cx1, y)], fill=LINE_COLOR, width=1)

img.save("icon_512.png")
print("Saved icon_512.png")
