package at.jku.fim.phonykeyboard.latin.biometrics;

import android.content.Context;
import android.util.Log;
import android.view.inputmethod.EditorInfo;

public class BiometricsPolicy {
    private static final String TAG = "BiometricsPolicy";

    private static BiometricsPolicy instance;

    private boolean biometricsAllowed;

    private BiometricsPolicy() { }

    public static BiometricsPolicy getInstance() {
        if (instance == null) {
            instance = new BiometricsPolicy();
        }
        return instance;
    }

    public void setEditorInfo(Context context, EditorInfo editorInfo) {
        String packageName = BiometricsManager.getInstance().getVerifiedPackageName(editorInfo);
        biometricsAllowed = packageName.equals(context.getPackageName());
        Log.i(TAG, String.format("%s biometrics for package %s", biometricsAllowed ? "Allow" : "Deny", packageName));
    }

    public boolean isBiometricsAllowed() {
        return biometricsAllowed;
    }
}
