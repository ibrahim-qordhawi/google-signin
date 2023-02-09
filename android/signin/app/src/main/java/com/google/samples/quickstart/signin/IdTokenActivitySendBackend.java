package com.google.samples.quickstart.signin;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.facebook.CallbackManager;
import com.facebook.CallbackManager.Factory;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Demonstrates retrieving an ID token for the current Google user.
 */
public class IdTokenActivitySendBackend extends AppCompatActivity implements
        View.OnClickListener {

    private static final String TAG = "IdTokenActivityBackend";
    private static final int RC_GET_TOKEN = 9002;

    private GoogleSignInClient mGoogleSignInClient;
    private TextView mIdTokenTextView;
    private Button mRefreshButton;

    private CallbackManager mFacebookCallbackManager;
    private static final String EMAIL = "email";
    private LoginButton mFacebookLoginButton;


        @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mFacebookCallbackManager = CallbackManager.Factory.create();
        LoginManager.getInstance().registerCallback(mFacebookCallbackManager,
            new FacebookCallback<LoginResult>() {
                @Override
                public void onSuccess(LoginResult loginResult) {
                    // App code
                }

                @Override
                public void onCancel() {
                    // App code
                }

                @Override
                public void onError(FacebookException exception) {
                    // App code
                }
        });

        // Views
        mIdTokenTextView = findViewById(R.id.detail);
        mRefreshButton = findViewById(R.id.button_optional_action);
        mRefreshButton.setText(R.string.refresh_token);

        // Button click listeners
        findViewById(R.id.sign_in_button).setOnClickListener(this);
        findViewById(R.id.sign_out_button).setOnClickListener(this);
        findViewById(R.id.disconnect_button).setOnClickListener(this);
        mRefreshButton.setOnClickListener(this);

        // For sample only: make sure there is a valid server client ID.
        validateServerClientID();

        // [START configure_signin]
        // Request only the user's ID token, which can be used to identify the
        // user securely to your backend. This will contain the user's basic
        // profile (name, profile picture URL, etc) so you should not need to
        // make an additional call to personalize your application.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.server_client_id))
                .requestEmail()
                .build();
        // [END configure_signin]

        // Build GoogleAPIClient with the Google Sign-In API and the above options.
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);



            mFacebookLoginButton = (LoginButton) findViewById(R.id.login_button);
            mFacebookLoginButton.setReadPermissions(Arrays.asList(EMAIL));
            // If you are using in a fragment, call loginButton.setFragment(this);

            // Callback registration
            mFacebookLoginButton.registerCallback(mFacebookCallbackManager, new FacebookCallback<LoginResult>() {
                @Override
                public void onSuccess(LoginResult loginResult) {
                    // App code
                }

                @Override
                public void onCancel() {
                    // App code
                }

                @Override
                public void onError(FacebookException exception) {
                    // App code
                }
            });
    }

    private void getIdToken() {
        // Show an account picker to let the user choose a Google account from the device.
        // If the GoogleSignInOptions only asks for IDToken and/or profile and/or email then no
        // consent screen will be shown here.
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_GET_TOKEN);
    }

    private void refreshIdToken() {
        // Attempt to silently refresh the GoogleSignInAccount. If the GoogleSignInAccount
        // already has a valid token this method may complete immediately.
        //
        // If the user has not previously signed in on this device or the sign-in has expired,
        // this asynchronous branch will attempt to sign in the user silently and get a valid
        // ID token. Cross-device single sign on will occur in this branch.
        mGoogleSignInClient.silentSignIn()
                .addOnCompleteListener(this, new OnCompleteListener<GoogleSignInAccount>() {
                    @Override
                    public void onComplete(@NonNull Task<GoogleSignInAccount> task) {
                        handleSignInResult(task);
                    }
                });
    }

    // [START handle_sign_in_result]
    private void handleSignInResult(@NonNull Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            final String idToken = account.getIdToken();

            Log.i(TAG, "token_id=" + idToken);

            // TODO(developer): send ID Token to server and validate
            new Thread(new Runnable() {
                @Override
                public void run() {
                    HttpClient httpClient = new DefaultHttpClient();
//                    HttpPost httpPost = new HttpPost("https://oauth2.googleapis.com/tokeninfo");
                    HttpGet httpGet = new HttpGet("https://oauth2.googleapis.com/tokeninfo?id_token="+idToken);

                    try {
//                        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
//                        nameValuePairs.add(new BasicNameValuePair("id_token", idToken));
//                        httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                        httpClient.getConnectionManager().getSchemeRegistry().register( new Scheme("https", SSLSocketFactory.getSocketFactory(), 443) );
                        HttpResponse response = httpClient.execute(httpGet);
                        int statusCode = response.getStatusLine().getStatusCode();
                        final String responseBody = EntityUtils.toString(response.getEntity());
                        Log.i(TAG, "Signed in as: " + responseBody);
                    } catch (ClientProtocolException e) {
                        Log.e(TAG, "Error sending ID token to backend.", e);
                    } catch (IOException e) {
                        Log.e(TAG, "Error sending ID token to backend.", e);
                    }
                }
            }).start();

            updateUI(account);
        } catch (ApiException e) {
            Log.w(TAG, "handleSignInResult:error", e);
            updateUI(null);
        }
    }
    // [END handle_sign_in_result]

    private void signOut() {
        mGoogleSignInClient.signOut().addOnCompleteListener(this, new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                updateUI(null);
            }
        });
    }

    private void revokeAccess() {
        mGoogleSignInClient.revokeAccess().addOnCompleteListener(this,
                new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        updateUI(null);
                    }
                });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_GET_TOKEN) {
            // [START get_id_token]
            // This task is always completed immediately, there is no need to attach an
            // asynchronous listener.
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
            // [END get_id_token]
        }

        mFacebookCallbackManager.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void updateUI(@Nullable GoogleSignInAccount account) {
        if (account != null) {
            ((TextView) findViewById(R.id.status)).setText(R.string.signed_in);

            String idToken = account.getIdToken();
            mIdTokenTextView.setText(getString(R.string.id_token_fmt, idToken));

            findViewById(R.id.sign_in_button).setVisibility(View.GONE);
            findViewById(R.id.sign_out_and_disconnect).setVisibility(View.VISIBLE);
            mRefreshButton.setVisibility(View.VISIBLE);
        } else {
            ((TextView) findViewById(R.id.status)).setText(R.string.signed_out);
            mIdTokenTextView.setText(getString(R.string.id_token_fmt, "null"));

            findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);
            findViewById(R.id.sign_out_and_disconnect).setVisibility(View.GONE);
            mRefreshButton.setVisibility(View.GONE);
        }
    }

    /**
     * Validates that there is a reasonable server client ID in strings.xml, this is only needed
     * to make sure users of this sample follow the README.
     */
    private void validateServerClientID() {
        String serverClientId = getString(R.string.server_client_id);
        String suffix = ".apps.googleusercontent.com";
        if (!serverClientId.trim().endsWith(suffix)) {
            String message = "Invalid server client ID in strings.xml, must end with " + suffix;

            Log.w(TAG, message);
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sign_in_button:
                getIdToken();
                break;
            case R.id.sign_out_button:
                signOut();
                break;
            case R.id.disconnect_button:
                revokeAccess();
                break;
            case R.id.button_optional_action:
                refreshIdToken();
                break;
        }
    }
}
