# CamSnap

Client-side camera-position cards and alignment guides for Minecraft 26.3.

## Current status

This port targets Minecraft 26.3, Fabric Loader 0.19.5, Fabric API
0.161.0+26.3, and Java 25. It migrates the project to Minecraft's shipped
Mojang-named classes and the 26.3 extracted-GUI/Fabric HUD APIs.

Minecraft 26.3 replaced the legacy immediate framebuffer/rendering path used
by the original off-screen 3D PiP feed. The camera view area is intentionally
left blank rather than showing a misleading raycast approximation; a full
second Minecraft 3D render is not implemented.

## Features

- Capture and store up to five camera positions with yaw, pitch, and FOV labels.
- Switch slots, save a slot with Alt+1–5, and persist positions per world/server.
- Show a resizable, draggable camera card. Coordinates, direction, FOV, and
  target details appear below the view and can be disabled in the F8 editor or
  Cloth Config. The HUD can show the active camera plus additional saved slots.
- Scale the HUD down to 8% of the screen width (Page Down or the config slider).
- Spawn a client-only, no-gravity mannequin at the active camera position,
  facing the saved direction. The mannequin is removed while its camera's
  chunk is unloaded and returns automatically when that chunk loads again.
  It has no collision and follows the saved player's crouching pose.
- Choose the mannequin skin in Mod Menu > CamSnap configuration. Put 64x64
  player skin PNGs in `.minecraft/camsnap/skinsforcamera`; the selector scans
  that folder when opened and uses the default Steve skin when none is selected.
- Show each camera's loaded/unloaded chunk status in its HUD header; an unloaded
  camera is labeled "NOT LOADED" and its view area reports the same status.
- Detect when a camera mannequin intersects a block; the HUD reports "CAM IN
  BLOCK" and suppresses the camera view status.
- Choose among eight HUD anchors and draw rule-of-thirds or symmetry guides.
- Show the distance to an unloaded target and configure card scale/color.
- Optional Cloth Config screen through ModMenu.

The marker shows the saved position and orientation. The HUD does not currently
render a live 3D camera feed.

## Controls

| Key | Action |
| --- | --- |
| K | Save current position to the active slot / toggle its card |
| J | Cycle card position |
| C | Cycle camera slot |
| 1–5 | Select a slot; Alt+1–5 saves to it |
| [ / ] | Adjust the slot's FOV label |
| Delete | Delete the active camera position |
| G | Cycle alignment guide |
| N / L | Toggle night/day status markers |
| Page Up / Page Down | Adjust card scale (8%–40%) |
| F8 | Open the layout editor; toggle camera slots and details there |

## Build and install

Requirements: JDK 25 and the included Gradle wrapper. In PowerShell, run this
from the project folder:

```powershell
.\gradlew.bat build
```

In Command Prompt, run `gradlew.bat build`.

Install the generated `build\libs\CamSnap-1.0.0.jar` in the Minecraft
`mods` directory alongside Fabric API. Cloth Config and ModMenu are optional
and only needed for the in-game configuration screen.

## Credits

- RedDude
- GitHub Auto Copilot
- Google Gravity
