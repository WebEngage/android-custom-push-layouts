package com.webengage.android.pushlayouts;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.webengage.sdk.android.Analytics;
import com.webengage.sdk.android.WebEngage;

public class MainActivity extends AppCompatActivity {
    private static final String USER_ID_KEY = "user_id";
    private SharedPreferences mSharedPrefs;

    private EditText mUserIdEditText;
    private Button mLoginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();
    }

    private void init() {
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        mUserIdEditText = findViewById(R.id.user_id_edittext);
        mLoginButton = findViewById(R.id.login_button);

        if (mSharedPrefs.getString(USER_ID_KEY, null) != null) {
            mUserIdEditText.setText(mSharedPrefs.getString(USER_ID_KEY, ""));
            mLoginButton.setText(getString(R.string.logout));
        } else {
            mUserIdEditText.setText("");
            mLoginButton.setText(getString(R.string.login));
        }
    }

    public void login(View view) {
        if (mLoginButton.getText().equals(getString(R.string.login))) {
            String userId = mUserIdEditText.getText().toString();
            if (!userId.isEmpty()) {
                mSharedPrefs.edit().putString(USER_ID_KEY, userId).apply();
                WebEngage.get().user().login(userId);
                mLoginButton.setText(getString(R.string.logout));
            } else {
                Toast.makeText(this, "Invalid User ID!", Toast.LENGTH_LONG).show();
            }
        } else {
            mSharedPrefs.edit().putString(USER_ID_KEY, null).apply();
            WebEngage.get().user().logout();
            mLoginButton.setText(getString(R.string.login));
            mUserIdEditText.setText("");
        }
    }

    public void track(View view) {
        String event = ((EditText) findViewById(R.id.event_edittext)).getText().toString().trim();
        if (!event.isEmpty()) {
            WebEngage.get().analytics().track(event, new Analytics.Options().setHighReportingPriority(true));
        } else {
            Toast.makeText(this, "Invalid User ID!", Toast.LENGTH_LONG).show();
        }
    }
}
