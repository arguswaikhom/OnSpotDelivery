package com.crown.onspotdelivery.page;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.crown.library.onspotlibrary.controller.OSGoogleSignIn;
import com.crown.library.onspotlibrary.controller.OSPreferences;
import com.crown.library.onspotlibrary.model.user.UserOSD;
import com.crown.library.onspotlibrary.utils.emun.OSPreferenceKey;
import com.crown.onspotdelivery.R;
import com.crown.onspotdelivery.controller.AppController;
import com.google.firebase.firestore.DocumentSnapshot;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class SignInActivity extends AppCompatActivity implements OSGoogleSignIn.OnGoogleSignInResponse {
    public static final int RC_SIGN_IN = 0;
    private OSGoogleSignIn mGoogleSignIn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        if (AppController.getInstance().isAuthenticated()) navHomeActivity();
        ButterKnife.bind(this);
    }

    @OnClick(R.id.gsibtn_asi_sign_in)
    void onClickedSignIn() {
        AppController controller = AppController.getInstance();
        mGoogleSignIn = new OSGoogleSignIn(this, controller.getGoogleSignInClient(), controller.getFirebaseAuth(), RC_SIGN_IN, this);
        mGoogleSignIn.pickAccount();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) mGoogleSignIn.signIn(data);
    }

    @Override
    public void onSuccessGoogleSignIn(DocumentSnapshot doc) {
        UserOSD user = doc.toObject(UserOSD.class);
        if (user == null) {
            Toast.makeText(this, "Can't get user details", Toast.LENGTH_SHORT).show();
            return;
        }
        OSPreferences preferences = OSPreferences.getInstance(getApplicationContext());
        preferences.setObject(user, OSPreferenceKey.USER);
        navHomeActivity();
    }

    @Override
    public void onFailureGoogleSignIn(String response, Exception e) {
        Toast.makeText(this, response, Toast.LENGTH_SHORT).show();
        if (e != null) e.printStackTrace();
    }

    private void navHomeActivity() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}