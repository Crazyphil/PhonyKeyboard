package at.jku.fim.inputstudy;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import at.jku.fim.phonykeyboard.latin.R;
import at.jku.fim.phonykeyboard.latin.biometrics.BiometricsManager;

public class StudyActivity extends AppCompatActivity {
    private static final int PASSWORD_WORD_LENGTH = 4;

    private SharedPreferences preferences;
    private String password;
    private PasswordGenerator passwordGenerator;

    private ProgressDialog progressDialog;
    private TextView passwordTextView;
    private EditText passwordEditText;
    private ImageButton loginImageButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            password = savedInstanceState.getString("password");
        }
        preferences = getSharedPreferences("StudyActivity", 0);
        passwordGenerator = new PasswordGenerator(this);

        setContentView(R.layout.study_layout);

        passwordTextView = (TextView)findViewById(R.id.study_layout_password_text);
        passwordEditText = (EditText)findViewById(R.id.study_layout_password_input);
        passwordEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_NULL || actionId == EditorInfo.IME_ACTION_DONE) {
                    processPasswordInput();
                    return true;
                }
                return false;
            }
        });
        loginImageButton = (ImageButton)findViewById(R.id.study_layout_login_action);
        loginImageButton.setOnClickListener(new View.OnClickListener() {
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
            password = preferences.getString("password", null);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (password == null) {
            progressDialog = ProgressDialog.show(this, getResources().getString(R.string.study_yourpassword_progress), null, true);
            new GeneratePasswordTask().execute();
        } else {
            passwordTextView.setText(password);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putString("password", password);

        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    protected void onStop() {
        preferences.edit().putString("password", password).apply();
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
        if (passwordEditText.getText().toString().equals(password)) {
            /*Intent confidenceIntent = new Intent(this, PhonyKeyboard.class);
            confidenceIntent.setAction(BiometricsManager.BROADCAST_ACTION_GET_CONFIDENCE);*/
            Intent confidenceIntent = new Intent(BiometricsManager.BROADCAST_ACTION_GET_CONFIDENCE);
            sendOrderedBroadcast(confidenceIntent, null, new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (!intent.getAction().equals(BiometricsManager.BROADCAST_ACTION_GET_CONFIDENCE)) {
                        return;
                    }

                    StringBuilder message = new StringBuilder(3);
                    message.append(getResources().getString(R.string.study_passwordresult_correct_text));
                    message.append("\n");
                    switch (getResultCode()) {
                        case RESULT_OK:
                            double confidence = getResultExtras(true).getDouble(BiometricsManager.BROADCAST_EXTRA_CONFIDENCE);
                            if (confidence < 1) {
                                message.append(getResources().getString(R.string.study_passwordresult_correct_user, confidence));
                            } else {
                                message.append(getResources().getString(R.string.study_passwordresult_correct_impostor, 1 - confidence));
                            }
                            break;
                        case RESULT_CANCELED:
                            switch ((int)getResultExtras(false).getDouble(BiometricsManager.BROADCAST_EXTRA_CONFIDENCE)) {
                                case (int)BiometricsManager.CONFIDENCE_NOT_ENOUGH_DATA:
                                    message.append(getResources().getString(R.string.study_passwordresult_correct_nodata));
                                    break;
                                case (int)BiometricsManager.CONFIDENCE_CAPTURING_ERROR:
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

            progressDialog.setMessage(getResources().getString(R.string.study_yourpassword_verify));
        } else {
            builder.setTitle("Wrong");
            builder.setMessage("Now you would have to try again.");
            builder.show();
        }
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        passwordEditText.setText("");
    }

    private class GeneratePasswordTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {
            return passwordGenerator.getWordBetweenDigits(PASSWORD_WORD_LENGTH, PASSWORD_WORD_LENGTH);
        }

        @Override
        protected void onPostExecute(String result) {
            password = result;
            if (passwordTextView != null) {
                passwordTextView.setText(password);
            }
            if (progressDialog != null) {
                progressDialog.dismiss();
            }
        }
    }
}
