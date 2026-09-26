package net.camsnap.client;


/**
 * Stores optional visual-status markers displayed on CamSnap's camera card.
 * Target: Minecraft 26.3 / Fabric Client / Java 25.
 */
public final class CamSnapEnhancer {

    private static final CamSnapEnhancer INSTANCE = new CamSnapEnhancer();

    private boolean fullbrightEnabled = false;
    private boolean lockDaylight = false;
    private float gammaBoost = 1.0f;

    private CamSnapEnhancer() {
    }

    public static CamSnapEnhancer getInstance() {
        return INSTANCE;
    }

    public boolean isFullbrightEnabled() {
        return fullbrightEnabled;
    }

    public void setFullbrightEnabled(boolean fullbrightEnabled) {
        this.fullbrightEnabled = fullbrightEnabled;
    }

    public boolean toggleFullbright() {
        this.fullbrightEnabled = !this.fullbrightEnabled;
        return this.fullbrightEnabled;
    }

    public boolean isLockDaylight() {
        return lockDaylight;
    }

    public void setLockDaylight(boolean lockDaylight) {
        this.lockDaylight = lockDaylight;
    }

    public boolean toggleLockDaylight() {
        this.lockDaylight = !this.lockDaylight;
        return this.lockDaylight;
    }

    public float getGammaBoost() {
        return gammaBoost;
    }

    public void setGammaBoost(float gammaBoost) {
        this.gammaBoost = Math.clamp(gammaBoost, 0.5f, 3.0f);
    }
}
