package com.crown.onspotdelivery.page;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

import com.crown.library.onspotlibrary.controller.OSPreferences;
import com.crown.library.onspotlibrary.model.user.UserOSD;
import com.crown.library.onspotlibrary.page.PhoneVerificationActivity;
import com.crown.library.onspotlibrary.utils.CreateProfileImage;
import com.crown.library.onspotlibrary.utils.OSListUtils;
import com.crown.library.onspotlibrary.utils.OSMessage;
import com.crown.library.onspotlibrary.utils.OSString;
import com.crown.library.onspotlibrary.utils.emun.OSPreferenceKey;
import com.crown.onspotdelivery.R;
import com.crown.onspotdelivery.controller.AppController;
import com.crown.onspotdelivery.databinding.ActivityMainBinding;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.messaging.FirebaseMessaging;

public class MainActivity extends AppCompatActivity implements EventListener<DocumentSnapshot> {
    private final int RC_VERIFY_PHONE_NO = 100;

    private ListenerRegistration mUserChangeListener;
    private UserOSD user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupWithNavController(binding.navView, navController);

        if (!AppController.getInstance().isAuthenticated()) {
            AppController.getInstance().signOut(this);
            return;
        }

        user = OSPreferences.getInstance(getApplicationContext()).getObject(OSPreferenceKey.USER, UserOSD.class);
        mUserChangeListener = FirebaseFirestore.getInstance().collection(OSString.refUser).document(user.getUserId()).addSnapshotListener(this);

        verifyDeviceToken();
        verifyUserContact();

        try {
            if (!user.getProfileImageUrl().contains(user.getUserId()))
                new CreateProfileImage(getApplicationContext()).execute(user.getUserId(), user.getProfileImageUrl());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void verifyUserContact() {
        if (TextUtils.isEmpty(user.getPhoneNumber())) {
            startActivityForResult(new Intent(this, PhoneVerificationActivity.class), RC_VERIFY_PHONE_NO);
        }
    }

    private void verifyDeviceToken() {
        if (user == null) return;
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(token -> {
            if (OSListUtils.isEmpty(user.getDeviceTokenOSD()) || !user.getDeviceTokenOSD().contains(token)) {
                FirebaseFirestore.getInstance().collection(OSString.refUser).document(user.getUserId())
                        .update(OSString.fieldDeviceTokenOSD, FieldValue.arrayUnion(token));
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mUserChangeListener != null) mUserChangeListener.remove();
    }

    @Override
    public void onEvent(@Nullable DocumentSnapshot doc, @Nullable FirebaseFirestoreException e) {
        if (doc != null && doc.exists()) {
            user = doc.toObject(UserOSD.class);
            if (user != null) {
                OSPreferences preferences = OSPreferences.getInstance(getApplicationContext());
                preferences.setObject(user, OSPreferenceKey.USER);
                sendBroadcast(new Intent(getString(R.string.action_osd_changes)));
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == RC_VERIFY_PHONE_NO && data != null) {
                String verifiedNumber = data.getStringExtra(PhoneVerificationActivity.PHONE_NO);
                if (!TextUtils.isEmpty(verifiedNumber)) updateUserPhoneNumber(verifiedNumber);
            }
        }
    }

    private void updateUserPhoneNumber(String verifiedNumber) {
        if (user == null || TextUtils.isEmpty(user.getUserId())) return;
        FirebaseFirestore.getInstance().collection(OSString.refUser).document(user.getUserId())
                .update(OSString.fieldPhoneNumber, verifiedNumber)
                .addOnFailureListener(e -> OSMessage.showSToast(this, "Phone number update failed!!"));
    }
}
