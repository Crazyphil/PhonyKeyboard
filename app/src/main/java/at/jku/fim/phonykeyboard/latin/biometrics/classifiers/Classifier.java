package at.jku.fim.phonykeyboard.latin.biometrics.classifiers;

import at.jku.fim.phonykeyboard.latin.biometrics.BiometricsEntry;
import at.jku.fim.phonykeyboard.latin.biometrics.BiometricsManager;
import at.jku.fim.phonykeyboard.latin.biometrics.BiometricsManagerImpl;
import at.jku.fim.phonykeyboard.latin.biometrics.data.Contract;

public abstract class Classifier {
    protected BiometricsManagerImpl manager;

    public Classifier(BiometricsManagerImpl manager) {
        this.manager = manager;
    }

    public Contract getDatabaseContract() {
        return null;
    }

    public double getScore() {
        return BiometricsManager.SCORE_NOT_ENOUGH_DATA;
    }

    public boolean clearData() {
        return false;
    }

    public abstract void onCreate();
    public abstract void onStartInput(long context, boolean restarting);
    public abstract void onKeyEvent(BiometricsEntry entry);
    public abstract void onFinishInput(boolean done);
    public abstract void onDestroy();
}
