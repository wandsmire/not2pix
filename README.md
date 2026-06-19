<p align="center">
  <img src="test_1024x1024.png" width="128" alt="Not2Pix icon"/>
</p>

<h1 align="center">Not2Pix</h1>

<p align="center">
  <strong>A fast, cross-platform pixel art editor built with libGDX</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/version-0.2.0-blue" alt="Version"/>
  <img src="https://img.shields.io/badge/platform-Android%20%7C%20Desktop-green" alt="Platforms"/>
  <img src="https://img.shields.io/badge/license-GPL--3.0-orange" alt="License"/>
  <img src="https://img.shields.io/badge/libGDX-1.13.0-red" alt="libGDX"/>
</p>

---

## ✨ Features

- 🎨 **Multi-tool drawing** — Pencil, Eraser, Fill, Lasso Fill, Line, Rectangle, Diamond, Ellipse, and Shape tools
- ✏️ **Pixel perfect mode** — Toggle pixel-perfect stroke processing for the pencil tool
- 🪣 **Fill & Clear** — Flood fill with color or clear to transparency (longpress fill tool to toggle)
- 📐 **Selection tool** — Select, move, copy, rotate (90° and free 360°), mirror H/V, outline, and color adjust
- 🎨 **Color adjustment** — Real-time HSV sliders (hue shift, saturation, brightness) on selections
- 🖼️ **Layer support** — Multiple layers with visibility eyes, solo mode, drag-to-reorder, opacity controls
- 🎞️ **Frame animation** — Create sprite animations with frame-by-frame editing and onion skin
- 🎁 **GIF export** — Export animations as animated GIFs
- 📂 **Aseprite format** — Read and write `.ase` files for interoperability
- 🗺️ **Minimap & zoom** — Navigate large canvases with up to 128x zoom, transparent minimap background
- 🎛️ **HSV color picker** — Vertical hue bar, SV gradient, old/new color preview with checkerboard, alpha slider
- 🎨 **Palette bar** — Collapsible (6 colors default, expandable), double-tap to edit, longpress for eyedropper
- ⚙️ **Preferences** — Configurable grid color, tile grid color, background color (persistent)
- 🔗 **NotTiled integration** — Edit tilesets directly from [NotTiled](https://github.com/wandsmire/NotTiled) via intents
- ↩️ **Undo/Redo** — Full undo history (disabled during active selection for safety)
- 📱 **Touch-optimized** — Bottom strip for curved screens, gesture support for pan, zoom, and drawing
- 📁 **Multi-document** — Tab strip with close buttons, + button for new documents
- 📂 **File manager chooser** — Use any installed file manager on Android, not just the default SAF picker

## 🏗️ Project Structure

```
Not2Pix/
├── core/          # Shared editor logic (tools, UI, document model)
├── android/       # Android launcher & platform integration
├── desktop/       # Desktop (LWJGL3) launcher
├── gradle/        # Gradle wrapper
├── build_debug.sh # Build debug APK
└── deploy_to_phone.sh  # Build + install + launch on device
```

## 🚀 Getting Started

### Prerequisites

- **JDK 17** (OpenJDK recommended)
- **Android SDK** with build-tools 34.0.0 and API 34
- **ADB** (for device deployment)

### Build Debug APK

```bash
./build_debug.sh
```

The APK will be at `out/Not2Pix_debug.apk`.

### Build & Deploy to Device

```bash
./deploy_to_phone.sh
```

This builds, installs, and launches Not2Pix on all connected devices.

### Run on Desktop

```bash
./gradlew :desktop:run
```

## 🛠️ Tools

| Tool | Description |
|------|-------------|
| Pencil | Draw individual pixels with configurable brush size and pixel-perfect mode |
| Eraser | Erase to transparency with configurable brush size |
| Fill | Flood-fill bounded areas; longpress to toggle clear-to-transparency mode |
| Line | Draw pixel-perfect lines (via Shape tool) |
| Rectangle | Draw rectangles outline (via Shape tool) |
| Diamond | Draw diamond shapes (via Shape tool) |
| Ellipse | Draw ellipses (via Shape tool) |
| Lasso Fill | Draw freehand outline, auto-fills enclosed area on release (via Shape tool) |
| Selection | Select, move, copy, rotate, mirror, outline, and color adjust regions |

## 🎯 Selection Tool Actions

| Action | Description |
|--------|-------------|
| Confirm | Commit selection to layer |
| Cancel | Restore original pixels |
| Copy | Stamp selection at current position |
| Rotate 90° | Clockwise rotation |
| Free Rotate | Drag horizontally to rotate any angle |
| Mirror H | Flip horizontally |
| Mirror V | Flip vertically |
| Outline | Add 1px outline around non-transparent pixels using selected color |
| Color Adjust | Real-time HSV sliders for hue shift, saturation, and brightness |

**Tip:** Double-tap the Selection tool button to select the entire layer.

## 📋 Supported Formats

| Format | Read | Write |
|--------|------|-------|
| PNG | ✅ | ✅ |
| BMP | ✅ | — |
| Aseprite (.ase) | ✅ | ✅ |
| GIF (animated) | — | ✅ |

## 🖥️ UI Layout

```
┌─────────────────────────────────────────┐
│ StatusBar: Not2Pix          32x32       │
├─────────────────────────────────────────┤
│ DocStrip: [Doc1 x] [Doc2 x] [+]        │
├─────────────────────────────────────────┤
│ FrameStrip                              │
├────┬────────────────────────────┬───────┤
│    │                            │ Palette│
│Tool│        Canvas              │  Bar   │
│ bar│                            │       │
│    │                            │[Layer]│
├────┴────────────────────────────┴───────┤
│ [Fit] [Move][Zoom-][Zoom+]  [Undo][Redo]│
│              Bottom Strip               │
└─────────────────────────────────────────┘
```

## 🤝 Integration with NotTiled

Not2Pix can be launched directly from NotTiled to edit tilesets. It accepts `EDIT` and `VIEW` intents for PNG/BMP images, and exposes a custom `com.mirwanda.not2pix.EDIT_TILESET` action for seamless round-trip editing.

## 📄 License

This project is licensed under the **GNU General Public License v3.0** — see the [LICENSE](LICENSE) file for details.

---

<p align="center">
  Made with ☕ and pixels
</p>
