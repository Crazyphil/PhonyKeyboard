package at.jku.fim.inputstudy;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import at.jku.fim.phonykeyboard.latin.R;

public class StudyActivity extends AppCompatActivity {
    private static final int PASSWORD_WORD_LENGTH = 4;

    private String password;

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

        setContentView(R.layout.study_layout);

        passwordTextView = (TextView)findViewById(R.id.study_layout_password_text);
        passwordEditText = (EditText)findViewById(R.id.study_layout_password_input);
        loginImageButton = (ImageButton)findViewById(R.id.study_layout_login_action);
        loginImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(StudyActivity.this);
                if (passwordEditText.getText().toString().equals(password)) {
                    builder.setTitle("Correct");
                    builder.setMessage("Now you would be logged in.");
                    new GeneratePasswordTask().execute();
                } else {
                    builder.setTitle("Wrong");
                    builder.setMessage("Now you would have to try again.");
                }
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                builder.show();

                passwordEditText.setText("");
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

    private class GeneratePasswordTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {
            PasswordGenerator generator = new PasswordGenerator(StudyActivity.this);
            return generator.getWordBetweenDigits(PASSWORD_WORD_LENGTH, PASSWORD_WORD_LENGTH);
        }

        @Override
        protected void onPostExecute(String result) {
            password = result;
            passwordTextView.setText(password);
            progressDialog.dismiss();
        }
    }
}
