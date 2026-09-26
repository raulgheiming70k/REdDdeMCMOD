package net.camsnap.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Architectural grid and center-line overlay renderer for CamSnap.
 * Offers 3 distinct modes for builders and architects:
 * 1. Rule of Thirds (Golden composition alignment)
 * 2. Symmetry Center Line (Vertical & horizontal block crosshairs)
 * 3. Clean View (No overlay)
 * Target: Minecraft 26.3 / Fabric Client / Java 25.
 */
public final class CamSnapGridRenderer {

    private static final CamSnapGridRenderer INSTANCE = new CamSnapGridRenderer();

    public enum GridMode {
        CLEAN("Clean View"),
        RULE_OF_THIRDS("Rule of Thirds"),
        SYMMETRY_CENTER("Symmetry Center Line");

        private final String displayName;

        GridMode(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public GridMode next() {
            GridMode[] values = values();
            return values[(this.ordinal() + 1) % values.length];
        }
    }

    private GridMode currentMode = GridMode.CLEAN;

    // Palette
    private static final int COLOR_GRID_LINE = 0x55FFFFFF;       // Translucent white
    private static final int COLOR_INTERSECT_DOT = 0xCCFFD700;   // Gold composition intersection dots
    private static final int COLOR_CENTER_LINE = 0x8800FFCC;     // Cyan symmetry guideline
    private static final int COLOR_CENTER_DOT = 0xFFFF3B30;      // Precision red center dot

    private CamSnapGridRenderer() {
    }

    public static CamSnapGridRenderer getInstance() {
        return INSTANCE;
    }

    public GridMode getCurrentMode() {
        return currentMode;
    }

    public void setCurrentMode(GridMode currentMode) {
        if (currentMode != null) {
            this.currentMode = currentMode;
        }
    }

    public GridMode cycleMode() {
        this.currentMode = this.currentMode.next();
        return this.currentMode;
    }

    /**
     * Renders alignment guides over the camera card.
     */
    public void renderGrid(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        switch (currentMode) {
            case CLEAN -> {
                // No overlay rendered
            }
            case RULE_OF_THIRDS -> renderRuleOfThirds(graphics, x, y, width, height);
            case SYMMETRY_CENTER -> renderSymmetryCenterLine(graphics, x, y, width, height);
        }
    }

    /**
     * Mode 1: Rule of Thirds 3x3 composition grid with intersection points.
     */
    private void renderRuleOfThirds(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        int x1 = x + (width / 3);
        int x2 = x + ((width * 2) / 3);
        int y1 = y + (height / 3);
        int y2 = y + ((height * 2) / 3);

        // Vertical division lines
        graphics.fill(x1, y, x1 + 1, y + height, COLOR_GRID_LINE);
        graphics.fill(x2, y, x2 + 1, y + height, COLOR_GRID_LINE);

        // Horizontal division lines
        graphics.fill(x, y1, x + width, y1 + 1, COLOR_GRID_LINE);
        graphics.fill(x, y2, x + width, y2 + 1, COLOR_GRID_LINE);

        // Subtle 2x2 intersection highlight dots
        drawDot(graphics, x1, y1, COLOR_INTERSECT_DOT);
        drawDot(graphics, x2, y1, COLOR_INTERSECT_DOT);
        drawDot(graphics, x1, y2, COLOR_INTERSECT_DOT);
        drawDot(graphics, x2, y2, COLOR_INTERSECT_DOT);
    }

    /**
     * Mode 2: Symmetry Center Line with continuous vertical/horizontal axes and center red marker.
     */
    private void renderSymmetryCenterLine(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        int cx = x + (width / 2);
        int cy = y + (height / 2);

        // Full vertical symmetry line
        graphics.fill(cx, y, cx + 1, y + height, COLOR_CENTER_LINE);

        // Full horizontal symmetry line
        graphics.fill(x, cy, x + width, cy + 1, COLOR_CENTER_LINE);

        // Precision 3x3 center dot
        graphics.fill(cx - 1, cy - 1, cx + 2, cy + 2, COLOR_CENTER_DOT);
    }

    private void drawDot(GuiGraphicsExtractor graphics, int px, int py, int color) {
        graphics.fill(px - 1, py - 1, px + 2, py + 2, color);
    }
}
