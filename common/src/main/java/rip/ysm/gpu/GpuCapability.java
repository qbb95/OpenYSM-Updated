package rip.ysm.gpu;

/** The 26.2 port intentionally uses the vanilla deferred geometry renderer. */
public final class GpuCapability {
    private static final String REASON = "Minecraft 26.2 uses deferred model geometry; legacy direct-OpenGL acceleration is unavailable";
    public static boolean isAvailable() { return false; }
    public static String getReason() { return REASON; }
    public static void check() { }
}
