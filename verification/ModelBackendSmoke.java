import com.elfmcys.yesstevemodel.NativeLibLoader;

public class ModelBackendSmoke {
    public static void main(String[] args) throws Exception {
        if (System.getProperty("OYSM_DISABLE_SMID") != null)
            throw new AssertionError("This check must run without the old launcher workaround");
        NativeLibLoader.init();
        if (!NativeLibLoader.isAvailable() || NativeLibLoader.isLoaded()
                || NativeLibLoader.getErrorComponent() != null)
            throw new AssertionError("Java backend must be available without a native library");
        System.out.println("PASS ModelBackendSmoke: Java backend available without special VM flags");
    }
}
