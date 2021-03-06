package yoon.develop.luckydiceout;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;

public class SignInActivity extends AppCompatActivity
    implements View.OnClickListener{
    public static final String USER_EMAIL = "user-email";
    public static final String USER_PASSWORD = "user-password";
    public static final String APP_SIGN_IN = "app-sign-in";

    private static final int RC_SIGN_IN = 9000;
    private final String TAG = "FB_SIGNIN";

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private GoogleSignInClient mGoogleSignInClient;
    private GoogleSignInAccount mGoogleAccount;

    private EditText etPass;
    private EditText etEmail;

    /**
     * Standard Activity lifecycle methods
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        // Set up click handlers and view item references
        findViewById(R.id.btnCreate).setOnClickListener(this);
        findViewById(R.id.btnSignIn).setOnClickListener(this);
        findViewById(R.id.btnSignOut).setOnClickListener(this);
        findViewById(R.id.sign_in_button).setOnClickListener(this);

        // Set the dimensions of the sign-in button.
        SignInButton signInButton = findViewById(R.id.sign_in_button);
        signInButton.setSize(SignInButton.SIZE_WIDE);

        etEmail = (EditText)findViewById(R.id.etEmailAddr);
        etPass = (EditText)findViewById(R.id.etPassword);

        // TODO: Get a reference to the Firebase auth object
        mAuth = FirebaseAuth.getInstance();

        // TODO: Attach a new AuthListener to detect sign in and out
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    Log.d(TAG, "Signed in: " + user.getUid());
                    findViewById(R.id.btnCreate).setVisibility(View.INVISIBLE);
                    findViewById(R.id.btnSignIn).setVisibility(View.INVISIBLE);
                    findViewById(R.id.btnSignOut).setVisibility(View.VISIBLE);
                    switchActivity(null);
                } else {
                    // User is signed out
                    Log.d(TAG, "Currently signed out");
                    findViewById(R.id.btnCreate).setVisibility(View.VISIBLE);
                    findViewById(R.id.btnSignIn).setVisibility(View.VISIBLE);
                    findViewById(R.id.btnSignOut).setVisibility(View.INVISIBLE);
                }
            }
        };

        updateStatus();

        //
        Intent intent = getIntent();
        String action = intent.getStringExtra(MainActivity.EXTRA_MESSAGE);
        if(!TextUtils.isEmpty(action) && action != null && action.equals("sign-out")){
            String email = intent.getStringExtra(MainActivity.EXTRA_MESSAGE_USER_EMAIL);
            signUserOut(email);
        }

        // Get last user
        SharedPreferences sharedPref = this.getSharedPreferences(APP_SIGN_IN, Context.MODE_PRIVATE);
        String lastUserEmail = sharedPref.getString(USER_EMAIL, "");
        String lastUserPassword = sharedPref.getString(USER_PASSWORD, "");
        if(!TextUtils.isEmpty(lastUserEmail)){
            etEmail.setText(lastUserEmail);
            etPass.setText(lastUserPassword);
        }

        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();

        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    /**
     * When the Activity starts and stops, the app needs to connect and
     * disconnect the AuthListener
     */
    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnSignIn:
                signUserIn();
                break;

            case R.id.sign_in_button:
                googleSignIn();
                break;

            case R.id.btnCreate:
                createUserAccount();
                break;

            case R.id.btnSignOut:
                signUserOut(null);
                break;
        }
    }

    @Override
    public void onBackPressed()
    {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            mGoogleAccount = completedTask.getResult(ApiException.class);

            // Signed in successfully, show authenticated UI.
            updateStatus("Logged in: " + mGoogleAccount.getEmail());
            switchActivity(mGoogleAccount.getEmail());
        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            // Log.w(TAG, "signInResult:failed code=" + e.getStatusCode());
            updateStatus("signInResult:failed code=" + e.getStatusCode());
        }
    }

    private boolean checkFormFields() {
        String email, password;

        email = etEmail.getText().toString();
        password = etPass.getText().toString();

        if (email.isEmpty()) {
            etEmail.setError("Email Required");
            return false;
        }
        if (password.isEmpty()){
            etPass.setError("Password Required");
            return false;
        }

        return true;
    }

    private void updateStatus() {
        TextView tvStat = (TextView)findViewById(R.id.tvSignInStatus);
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            tvStat.setText("Signed in: " + user.getEmail());
        }
        else {
            tvStat.setText("Signed Out");
        }
    }

    private void updateStatus(String stat) {
        TextView tvStat = (TextView)findViewById(R.id.tvSignInStatus);
        tvStat.setText(stat);
    }

    private void googleSignIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void signUserIn() {
        if (!checkFormFields()) {
            return;
        }

        String email = etEmail.getText().toString();
        String password = etPass.getText().toString();

        // Save user's email
        SharedPreferences sharedPref = this.getSharedPreferences(APP_SIGN_IN, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(USER_EMAIL, email);
        editor.putString(USER_PASSWORD, password);
        editor.commit();

        // Sign the user in with email and password credentials
        mAuth.signInWithEmailAndPassword(email,password)
            .addOnCompleteListener(this,
                new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(SignInActivity.this, "Signed in", Toast.LENGTH_SHORT).show();
                        }
                        else {
                            Toast.makeText(SignInActivity.this, "Sign in failed", Toast.LENGTH_SHORT).show();
                        }

                        switchActivity(null);
                        updateStatus();
                    }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    if (e instanceof FirebaseAuthInvalidCredentialsException) {
                        updateStatus("Invalid password.");
                    }
                    else if (e instanceof FirebaseAuthInvalidUserException) {
                        updateStatus("No account with this email.");
                    }
                    else {
                        updateStatus(e.getLocalizedMessage());
                    }
                }
            });
    }

    private void signUserOut(String email) {
        if(mAuth.getCurrentUser() != null){
            mAuth.signOut();
            updateStatus();
        }
        else if (!TextUtils.isEmpty(email)) {
            updateStatus("Signed out: " + email);
        }
        else {
            updateStatus("Signed out");
        }
    }

    private void switchActivity(String userEmail){
        if (!TextUtils.isEmpty(userEmail)) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra(USER_EMAIL, userEmail);
            startActivity(intent);
        }
        else {
            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null) {
                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra(USER_EMAIL, user.getEmail());
                startActivity(intent);
            }
        }
    }

    private void createUserAccount() {
        if (!checkFormFields()) {
            return;
        }

        String email = etEmail.getText().toString();
        String password = etPass.getText().toString();

        // Create the user account
        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this,
                new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(SignInActivity.this, "User created", Toast.LENGTH_SHORT)
                                    .show();
                        } else {
                            Toast.makeText(SignInActivity.this, "Account creation failed", Toast.LENGTH_SHORT)
                                    .show();
                        }
                    }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e(TAG, e.toString());
                    if (e instanceof FirebaseAuthUserCollisionException) {
                        updateStatus("This email address is already in use.");
                    }
                    else {
                        updateStatus(e.getLocalizedMessage());
                    }
                }
            });
    }
}
