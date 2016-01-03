package at.jku.fim.inputstudy;

import android.app.Application;

public class PhonyKeyboardApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        BackgroundManager.init(this);
    }
}
