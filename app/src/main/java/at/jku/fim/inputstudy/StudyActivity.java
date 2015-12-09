package at.jku.fim.inputstudy;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.databinding.DataBindingUtil;
import android.databinding.ObservableField;
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

import java.text.SimpleDateFormat;
import java.util.Date;

import at.jku.fim.phonykeyboard.latin.R;
import at.jku.fim.phonykeyboard.latin.biometrics.BiometricsManager;
import at.jku.fim.phonykeyboard.latin.databinding.StudyActivityBinding;

public class StudyActivity extends AppCompatActivity {
    private static final int PASSWORD_WORD_LENGTH = 4;

    private SharedPreferences preferences;
    private ObservableField<String> password, lastLogin;
    private PasswordGenerator passwordGenerator;

    private ProgressDialog progressDialog;
    private StudyActivityBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        password = new ObservableField<>();
        lastLogin = new ObservableField<>();
        if (savedInstanceState != null) {
            password.set(savedInstanceState.getString("password"));
            lastLogin.set(savedInstanceState.getString("lastLogin"));
        }
        preferences = getSharedPreferences("StudyActivity", 0);
        passwordGenerator = new PasswordGenerator(this);
        binding = DataBindingUtil.setContentView(this, R.layout.study_activity);
        binding.setPassword(password);
        binding.setLastLogin(lastLogin);

        binding.passwordInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_NULL || actionId == EditorInfo.IME_ACTION_DONE) {
                    processPasswordInput();
                    return true;
                }
                return false;
            }
        });
        binding.loginAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                processPasswordInput();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.study_menu, menu);
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

        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    protected void onStop() {
        preferences.edit().putString("lastLogin", lastLogin.get()).apply();
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

        //noinspection SimplifiableIfStatement
        if (id == R.id.study_action_settings) {
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

        if (binding.passwordInput.getText().toString().equals(password.get())) {
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

        binding.passwordInput.setText("");
        binding.passwordInput.clearFocus();
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(binding.passwordInput.getWindowToken(), 0);
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
