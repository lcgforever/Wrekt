package com.citrix.wrekt.activity;

import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.citrix.wrekt.R;
import com.citrix.wrekt.WrektApplication;
import com.citrix.wrekt.controller.api.ILoginController;
import com.citrix.wrekt.data.LoginState;
import com.citrix.wrekt.data.pref.IntegerPreference;
import com.citrix.wrekt.data.pref.StringPreference;
import com.citrix.wrekt.di.annotation.LoginStatePref;
import com.citrix.wrekt.di.annotation.UserEmailPref;
import com.citrix.wrekt.di.annotation.UsernamePref;
import com.citrix.wrekt.di.component.ActivityComponent;
import com.citrix.wrekt.di.module.ActivityModule;
import com.citrix.wrekt.event.FirebaseAuthFailedEvent;
import com.citrix.wrekt.event.FirebaseAuthSuccessfulEvent;
import com.citrix.wrekt.firebase.api.IFirebaseFactory;
import com.citrix.wrekt.firebase.api.IFirebaseUrlFormatter;
import com.citrix.wrekt.fragment.dialog.ProgressDialogFragment;
import com.citrix.wrekt.util.KeyboardUtils;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.squareup.otto.Subscribe;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

public class LoginActivity extends BaseActivity implements View.OnClickListener {

    private static final String TAG_LOGIN_PROGRESS_DIALOG = "TAG_LOGIN_PROGRESS_DIALOG";

    @Inject
    @LoginStatePref
    IntegerPreference loginStatePref;

    @Inject
    @UsernamePref
    StringPreference usernamePref;

    @Inject
    @UserEmailPref
    StringPreference userEmailPref;

    @Inject
    ILoginController loginController;

    @Inject
    IFirebaseFactory firebaseFactory;

    @Inject
    IFirebaseUrlFormatter firebaseUrlFormatter;

    private TextInputLayout emailTextInputLayout;
    private TextInputLayout passwordTextInputLayout;
    private TextInputLayout confirmPasswordTextInputLayout;
    private TextInputLayout usernameTextInputLayout;
    private TextInputEditText emailEditText;
    private TextInputEditText passwordEditText;
    private TextInputEditText confirmPasswordEditText;
    private TextInputEditText usernameEditText;
    private Button confirmLoginButton;
    private Button switchOptionButton;
    private TextView signUpOptionTextView;
    private LinearLayout socialLoginContainer;
    private LoginButton facebookLoginButton;

    private FragmentManager fragmentManager;
    private ActivityComponent activityComponent;
    private CallbackManager callbackManager;
    private Pattern emailPattern;
    private Pattern usernamePattern;
    private boolean isCurrentOptionLogin = true;
    private boolean emailFormatValid = false;
    private boolean passwordValid = false;
    private boolean passwordMatches = false;
    private boolean usernameValid = false;

    public static void start(Context context) {
        Intent intent = new Intent(context, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        inject();

        fragmentManager = getFragmentManager();
        callbackManager = CallbackManager.Factory.create();
        emailPattern = Pattern.compile(getString(R.string.email_format_regex));
        usernamePattern = Pattern.compile(getString(R.string.username_format_regex));

        emailTextInputLayout = (TextInputLayout) findViewById(R.id.email_text_input_layout);
        passwordTextInputLayout = (TextInputLayout) findViewById(R.id.password_text_input_layout);
        confirmPasswordTextInputLayout = (TextInputLayout) findViewById(R.id.confirm_password_text_input_layout);
        usernameTextInputLayout = (TextInputLayout) findViewById(R.id.username_text_input_layout);
        emailEditText = (TextInputEditText) findViewById(R.id.email_edit_text);
        passwordEditText = (TextInputEditText) findViewById(R.id.password_edit_text);
        confirmPasswordEditText = (TextInputEditText) findViewById(R.id.confirm_password_edit_text);
        usernameEditText = (TextInputEditText) findViewById(R.id.username_edit_text);
        confirmLoginButton = (Button) findViewById(R.id.confirm_login_button);
        switchOptionButton = (Button) findViewById(R.id.switch_option_button);
        signUpOptionTextView = (TextView) findViewById(R.id.sign_up_option_text_view);
        socialLoginContainer = (LinearLayout) findViewById(R.id.social_login_container);
        facebookLoginButton = (LoginButton) findViewById(R.id.facebook_login_button);

        emailEditText.addTextChangedListener(new EmailTextWatcher());
        passwordEditText.addTextChangedListener(new PasswordTextWatcher());
        confirmPasswordEditText.addTextChangedListener(new ConfirmPasswordTextWatcher());
        usernameEditText.addTextChangedListener(new UsernameTextWatcher());
        confirmLoginButton.setOnClickListener(this);
        switchOptionButton.setOnClickListener(this);
        setupFacebookLogin();
        emailEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_NEXT || actionId == EditorInfo.IME_ACTION_UNSPECIFIED) {
                    passwordEditText.requestFocus();
                    return true;
                }
                return false;
            }
        });
        passwordEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((actionId == EditorInfo.IME_ACTION_NEXT || actionId == EditorInfo.IME_ACTION_UNSPECIFIED)) {
                    if (isCurrentOptionLogin) {
                        loginOrSignUpUser();
                    } else {
                        confirmPasswordEditText.requestFocus();
                    }
                    return true;
                }
                return false;
            }
        });
        confirmPasswordEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((actionId == EditorInfo.IME_ACTION_NEXT || actionId == EditorInfo.IME_ACTION_UNSPECIFIED)) {
                    usernameEditText.requestFocus();
                    return true;
                }
                return false;
            }
        });
        usernameEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_UNSPECIFIED)) {
                    loginOrSignUpUser();
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        hideLoginProgressDialog();
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onStop() {
        super.onStop();
        emailEditText.setText("");
        passwordEditText.setText("");
        confirmPasswordEditText.setText("");
        usernameEditText.setText("");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        activityComponent = null;
    }

    @Override
    protected void inject() {
        WrektApplication application = (WrektApplication) getApplication();
        activityComponent = application.getAppComponent().plus(new ActivityModule(this));
        activityComponent.inject(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.confirm_login_button:
                loginOrSignUpUser();
                break;

            case R.id.switch_option_button:
                emailEditText.setText("");
                passwordEditText.setText("");
                confirmPasswordEditText.setText("");
                usernameEditText.setText("");
                if (isCurrentOptionLogin) {
                    // If current option is login, switch to show sign up option
                    confirmPasswordTextInputLayout.setVisibility(View.VISIBLE);
                    usernameTextInputLayout.setVisibility(View.VISIBLE);
                    socialLoginContainer.setVisibility(View.GONE);
                    signUpOptionTextView.setText(R.string.has_account_text);
                    switchOptionButton.setText(R.string.login_text);
                    confirmLoginButton.setText(R.string.sign_up_text);
                    passwordEditText.setImeActionLabel(getString(R.string.ime_action_next_text),
                            passwordEditText.getImeActionId());
                } else {
                    // If current option is sign up, switch to show login option
                    confirmPasswordTextInputLayout.setVisibility(View.GONE);
                    usernameTextInputLayout.setVisibility(View.GONE);
                    socialLoginContainer.setVisibility(View.VISIBLE);
                    signUpOptionTextView.setText(R.string.no_account_text);
                    switchOptionButton.setText(R.string.sign_up_text);
                    confirmLoginButton.setText(R.string.login_text);
                    passwordEditText.setImeActionLabel(getString(R.string.login_text),
                            passwordEditText.getImeActionId());
                }
                isCurrentOptionLogin = !isCurrentOptionLogin;
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        showLoginProgressDialog();
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    @Subscribe
    public void onFirebaseAuthSuccessfulEventReceived(FirebaseAuthSuccessfulEvent event) {
        setLoginStateAndFinish(event.getLoginState());
    }

    @Subscribe
    public void onFirebaseAuthFailedEventReceived(FirebaseAuthFailedEvent event) {
        hideLoginProgressDialog();
        String loginFailedMessage = String.format(
                getString(R.string.login_firebase_error_message), event.getErrorMessage());
        Toast.makeText(LoginActivity.this, loginFailedMessage, Toast.LENGTH_SHORT).show();
    }

    private void setupFacebookLogin() {
        facebookLoginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(final LoginResult loginResult) {
                GraphRequest request = GraphRequest.newMeRequest(loginResult.getAccessToken(), new GraphRequest.GraphJSONObjectCallback() {
                    @Override
                    public void onCompleted(JSONObject object, GraphResponse response) {
                        // Get facebook data from login
                        String email = "";
                        try {
                            if (object.has("email")) {
                                email = object.getString("email");
                            }
                        } catch (JSONException e) {
                            Log.e("findme: ", "Facebook get email failed: " + e.getMessage());
                        }

                        loginController.loginWithOAuth(LoginState.FACEBOOK_LOGGED_IN,
                                getString(R.string.auth_client_facebook), loginResult.getAccessToken().getToken(), email);
                    }
                });
                Bundle parameters = new Bundle();
                // Set parameters need to be extracted
                parameters.putString("fields", "email");
                request.setParameters(parameters);
                request.executeAsync();
            }

            @Override
            public void onCancel() {

            }

            @Override
            public void onError(FacebookException error) {
                Log.e("findme: ", "Facebook login error: " + error.getMessage());
                Toast.makeText(LoginActivity.this, R.string.login_error_message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loginOrSignUpUser() {
        KeyboardUtils.hideKeyboard(this);
        if (!emailFormatValid) {
            emailTextInputLayout.setError(getString(R.string.email_format_invalid_message));
        } else if (!passwordValid) {
            passwordTextInputLayout.setError(getString(R.string.password_invalid_message));
        } else if (!isCurrentOptionLogin && !passwordMatches) {
            confirmPasswordTextInputLayout.setError(getString(R.string.confirm_password_invalid_message));
        } else if (!isCurrentOptionLogin && !usernameValid) {
            usernameTextInputLayout.setError(getString(R.string.username_invalid_message));
        } else {
            showLoginProgressDialog();
            String email = emailEditText.getText().toString();
            String password = passwordEditText.getText().toString();
            if (isCurrentOptionLogin) {
                loginUserWithFirebase(email, password);
            } else {
                String username = usernameEditText.getText().toString();
                signUpUserWithFirebaseThenLogin(email, password, username);
            }
        }
    }

    private void showLoginProgressDialog() {
        ProgressDialogFragment fragment =
                (ProgressDialogFragment) fragmentManager.findFragmentByTag(TAG_LOGIN_PROGRESS_DIALOG);
        if (fragment == null) {
            fragment = ProgressDialogFragment.newInstance(R.string.login_progress_dialog_message);
        }
        fragment.show(fragmentManager, TAG_LOGIN_PROGRESS_DIALOG);
    }

    private void hideLoginProgressDialog() {
        ProgressDialogFragment fragment =
                (ProgressDialogFragment) fragmentManager.findFragmentByTag(TAG_LOGIN_PROGRESS_DIALOG);
        if (fragment != null) {
            fragment.dismiss();
        }
    }

    private void signUpUserWithFirebaseThenLogin(String email, String password, String username) {
        loginController.signUpThenLogin(email, password, username);
    }

    private void loginUserWithFirebase(String email, String password) {
        loginController.login(LoginState.WREKT_LOGGED_IN, email, password, usernamePref.get());
    }

    private void setLoginStateAndFinish(LoginState loginState) {
        hideLoginProgressDialog();
        loginStatePref.set(loginState.getValue());
        MainActivity.start(LoginActivity.this);
        finish();
    }


    private class EmailTextWatcher implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            emailTextInputLayout.setError(null);
            Matcher emailMatcher = emailPattern.matcher(s);
            emailFormatValid = !TextUtils.isEmpty(s) && emailMatcher.matches();
        }
    }

    private class PasswordTextWatcher implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            passwordTextInputLayout.setError(null);
            passwordValid = !TextUtils.isEmpty(s) && s.length() >= 6;
        }
    }

    private class ConfirmPasswordTextWatcher implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            confirmPasswordTextInputLayout.setError(null);
            passwordMatches = !TextUtils.isEmpty(s) && s.toString().equals(passwordEditText.getText().toString());
        }
    }

    private class UsernameTextWatcher implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            usernameTextInputLayout.setError(null);
            Matcher usernameMatcher = usernamePattern.matcher(s);
            usernameValid = !TextUtils.isEmpty(s) && usernameMatcher.matches();
        }
    }
}
