package at.jku.fim.phonykeyboard.latin.biometrics;

import at.usmile.cormorant.api.AbstractConfidenceService;
import at.usmile.cormorant.api.model.StatusDataConfidence;

public class CormorantAuthenticationService extends AbstractConfidenceService {
    @Override
    protected void onDataUpdateRequest() {
        BiometricsManager manager;
        try {
            manager = BiometricsManager.getInstance();
        } catch (IllegalStateException e) {
            manager = null;
        }
        double score = manager != null ? manager.getLastScore() : BiometricsManager.SCORE_CAPTURING_DISABLED;
        publishConfidenceUpdate(score);
    }

    protected void publishConfidenceUpdate(double score) {
        StatusDataConfidence confidence = new StatusDataConfidence();
        if (score == BiometricsManager.SCORE_NOT_ENOUGH_DATA) {
            confidence.status(StatusDataConfidence.Status.TRAINING);
            confidence.confidence(0d);
        } else {
            confidence.status(StatusDataConfidence.Status.OPERATIONAL);
            if (score < BiometricsManager.SCORE_NOT_ENOUGH_DATA) {
                confidence.confidence(0d);
            } else {
                confidence.confidence(score > 0 ? score : -1);
            }
        }
        publishConfidenceUpdate(confidence);
    }
}