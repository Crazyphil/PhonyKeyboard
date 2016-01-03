package at.jku.fim.inputstudy;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.databinding.DataBindingUtil;
import android.databinding.ObservableBoolean;
import android.databinding.ObservableField;
import android.databinding.ObservableInt;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import at.jku.fim.SensitiveConstants;
import at.jku.fim.phonykeyboard.latin.Constants;
import at.jku.fim.phonykeyboard.latin.R;
import at.jku.fim.phonykeyboard.latin.biometrics.BiometricsManager;
import at.jku.fim.phonykeyboard.latin.biometrics.BiometricsManagerImpl;
import at.jku.fim.phonykeyboard.latin.biometrics.data.BiometricsDbHelper;
import at.jku.fim.phonykeyboard.latin.databinding.StudyActivityBinding;

public class StudyActivity extends AppCompatActivity {
    protected static final String PREFERENCES_NAME = "StudyActivity";

    private static final int PASSWORD_WORD_LENGTH = 4;

    private SharedPreferences preferences;
    private ObservableField<String> password, lastLogin, captureMotivation;
    private ObservableInt captureCount;
    private ObservableBoolean isCaptureMode;
    private PasswordGenerator passwordGenerator;

    private ProgressDialog progressDialog;
    private StudyActivityBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        password = new ObservableField<>();
        lastLogin = new ObservableField<>();
        captureMotivation = new ObservableField<>();
        isCaptureMode = new ObservableBoolean(getIntent().getAction() != null);
        captureCount = new ObservableInt();

        if (savedInstanceState != null) {
            password.set(savedInstanceState.getString("password"));
            lastLogin.set(savedInstanceState.getString("lastLogin"));
            captureCount.set(savedInstanceState.getInt("captureCount"));
        }

        preferences = getSharedPreferences(PREFERENCES_NAME, 0);
        passwordGenerator = new PasswordGenerator(this);
        setCaptureMotivation();

        binding = DataBindingUtil.setContentView(this, R.layout.study_activity);
        binding.setPassword(password);
        binding.setLastLogin(lastLogin);
        binding.setIsCaptureMode(isCaptureMode);
        binding.setCaptureCount(captureCount);
        binding.setCaptureMotivation(captureMotivation);

        binding.editTextPassword.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_NULL || actionId == EditorInfo.IME_ACTION_DONE) {
                    processPasswordInput();
                    return true;
                }
                return false;
            }
        });
        binding.imageButtonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                processPasswordInput();
            }
        });
        if (isCaptureMode.get()) {
            binding.editTextPassword.setPrivateImeOptions(Constants.ImeOption.INTERNAL_BIOMETRICS_CLASSIFIER + "=CaptureClassifier");
        }
    }

    private void setCaptureMotivation() {
        if (isCaptureMode.get()) {
            if (captureCount.get() == 0) {
                captureMotivation.set(getResources().getString(R.string.study_capture_motivation_0));
            } else if (captureCount.get() < 10) {
                captureMotivation.set(getResources().getString(R.string.study_capture_motivation_1));
            } else if (captureCount.get() < 30) {
                captureMotivation.set(getResources().getString(R.string.study_capture_motivation_10));
            } else if (captureCount.get() < 50) {
                captureMotivation.set(getResources().getString(R.string.study_capture_motivation_30));
            } else if (captureCount.get() < 70) {
                captureMotivation.set(getResources().getString(R.string.study_capture_motivation_50));
            } else if (captureCount.get() < 90) {
                captureMotivation.set(getResources().getString(R.string.study_capture_motivation_70));
            } else if (captureCount.get() < 100) {
                captureMotivation.set(getResources().getString(R.string.study_capture_motivation_90));
            } else {
                captureMotivation.set(getResources().getString(R.string.study_capture_motivation_100));
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (isCaptureMode.get()) {
            menu.add(0, 0, 100, R.string.study_action_send_capture);
        }
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (preferences.contains("password")) {
            password.set(preferences.getString("password", null));
        }
        if (preferences.contains("lastLogin")) {
            lastLogin.set(preferences.getString("lastLogin", null));
        }
        if (preferences.contains("captureCount")) {
            captureCount.set(preferences.getInt("captureCount", 0));
            setCaptureMotivation();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (password.get() == null) {
            progressDialog = ProgressDialog.show(this, getResources().getString(R.string.study_yourpassword_progress), null, true);
            new GeneratePasswordTask().execute();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putString("password", password.get());
        savedInstanceState.putString("lastLogin", lastLogin.get());
        savedInstanceState.putInt("captureCount", captureCount.get());

        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    protected void onStop() {
        preferences.edit().putString("lastLogin", lastLogin.get()).apply();
        preferences.edit().putInt("captureCount", captureCount.get()).apply();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        passwordGenerator.destroy();
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == 0) {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:")); // only email apps should handle this
            intent.putExtra(Intent.EXTRA_EMAIL, SensitiveConstants.STUDY_CAPTURE_RECEPIENT_EMAIL);
            intent.putExtra(Intent.EXTRA_SUBJECT, "Collected typing data");
            intent.putExtra(Intent.EXTRA_TEXT, getResources().getString(R.string.study_capture_email_text) + "\n");
            intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(getDatabasePath(BiometricsDbHelper.DATABASE_NAME)));
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                Toast.makeText(this, getResources().getText(R.string.study_action_send_capture_error), Toast.LENGTH_SHORT).show();
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void processPasswordInput() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(StudyActivity.this);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        if (binding.editTextPassword.getText().toString().equals(password.get())) {
            /*Intent confidenceIntent = new Intent(this, PhonyKeyboard.class);
            confidenceIntent.setAction(BiometricsManager.BROADCAST_ACTION_GET_SCORE);*/
            Intent scoreIntent = new Intent(BiometricsManager.BROADCAST_ACTION_GET_SCORE);
            sendOrderedBroadcast(scoreIntent, null, new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (!intent.getAction().equals(BiometricsManager.BROADCAST_ACTION_GET_SCORE)) {
                        return;
                    }

                    StringBuilder message = new StringBuilder(3);
                    message.append(getResources().getString(R.string.study_passwordresult_correct_text));
                    message.append("\n");
                    switch (getResultCode()) {
                        case RESULT_OK:
                            double score = getResultExtras(true).getDouble(BiometricsManager.BROADCAST_EXTRA_SCORE);
                            if (score < 1) {
                                message.append(getResources().getString(R.string.study_passwordresult_correct_user, score));
                            } else {
                                message.append(getResources().getString(R.string.study_passwordresult_correct_impostor, 1 - score));
                            }
                            lastLogin.set(SimpleDateFormat.getDateTimeInstance().format(new Date()));
                            break;
                        case RESULT_CANCELED:
                            switch ((int)getResultExtras(false).getDouble(BiometricsManager.BROADCAST_EXTRA_SCORE)) {
                                case (int)BiometricsManager.SCORE_NOT_ENOUGH_DATA:
                                    message.append(getResources().getString(R.string.study_passwordresult_correct_nodata));
                                    if (isCaptureMode.get()) {
                                        captureCount.set((int)getResultExtras(false).getLong(BiometricsManagerImpl.INTERNAL_BROADCAST_EXTRA_CAPTURE_COUNT, 0));
                                        setCaptureMotivation();
                                        lastLogin.set(SimpleDateFormat.getDateTimeInstance().format(new Date()));

                                        if (captureCount.get() > 0) {
                                            Calendar nextCapture = GregorianCalendar.getInstance();
                                            nextCapture.add(Calendar.MILLISECOND, CaptureReminderService.CAPTURE_REPEAT_MS);
                                            CaptureReminderService.scheduleNotification(StudyActivity.this, nextCapture);
                                        }
                                    }
                                    break;
                                case (int)BiometricsManager.SCORE_CAPTURING_ERROR:
                                    message.append(getResources().getString(R.string.study_passwordresult_correct_failed));
                                    break;
                            }
                            break;
                        default:
                            message.append(getResources().getString(R.string.study_passwordresult_correct_nobiometrics));
                            break;
                    }

                    builder.setTitle(getResources().getString(R.string.study_passwordresult_correct_title));
                    builder.setMessage(message.toString());
                    progressDialog.dismiss();
                    builder.show();
                }
            }, null, RESULT_FIRST_USER, null, null);

            progressDialog = ProgressDialog.show(this, getResources().getString(R.string.study_yourpassword_verify), null, true, false);
        } else {
            builder.setTitle(getResources().getString(R.string.study_passwordresult_wrong_title));
            builder.setMessage(getResources().getString(R.string.study_passwordresult_wrong_text));
            builder.show();
        }

        binding.editTextPassword.setText("");
        binding.editTextPassword.clearFocus();
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(binding.editTextPassword.getWindowToken(), 0);
    }

    private class GeneratePasswordTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {
            return passwordGenerator.getWordBetweenDigits(PASSWORD_WORD_LENGTH, PASSWORD_WORD_LENGTH);
        }

        @Override
        protected void onPostExecute(String result) {
            password.set(result);
            preferences.edit().putString("password", password.get()).apply();
            if (progressDialog != null) {
                progressDialog.dismiss();
            }
        }
    }
}
