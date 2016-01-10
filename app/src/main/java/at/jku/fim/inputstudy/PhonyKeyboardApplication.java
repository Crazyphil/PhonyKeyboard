package at.jku.fim.inputstudy;

import android.app.Application;

import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;

import at.jku.fim.SensitiveConstants;

@ReportsCrashes(
        formUri = "https://kapferit.cloudant.com/acra-phonykeyboard/_design/acra-storage/_update/report",
        formUriBasicAuthLogin = "iseareptineateleathellye", // optional
        formUriBasicAuthPassword = SensitiveConstants.ACRA_REPORT_PASSWORD, // optional
        reportType = org.acra.sender.HttpSender.Type.JSON,
        httpMethod = org.acra.sender.HttpSender.Method.PUT
)
public class PhonyKeyboardApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ACRA.init(this);

        BackgroundManager.init(this);
    }
}
