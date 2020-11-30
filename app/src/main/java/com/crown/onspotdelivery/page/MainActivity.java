package com.crown.onspotdelivery.page;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

import com.crown.library.onspotlibrary.controller.OSPreferences;
import com.crown.library.onspotlibrary.model.user.UserOSD;
import com.crown.library.onspotlibrary.utils.CreateProfileImage;
import com.crown.library.onspotlibrary.utils.OSListUtils;
import com.crown.library.onspotlibrary.utils.OSString;
import com.crown.library.onspotlibrary.utils.emun.OSPreferenceKey;
import com.crown.onspotdelivery.R;
import com.crown.onspotdelivery.databinding.ActivityMainBinding;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.messaging.FirebaseMessaging;

public class MainActivity extends AppCompatActivity implements EventListener<DocumentSnapshot> {

    private ListenerRegistration mUserChangeListener;
    private UserOSD user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupWithNavController(binding.navView, navController);

        user = OSPreferences.getInstance(getApplicationContext()).getObject(OSPreferenceKey.USER, UserOSD.class);
        mUserChangeListener = FirebaseFirestore.getInstance().collection(getString(R.string.ref_user)).document(user.getUserId()).addSnapshotListener(this);

        verifyDeviceToken();

        try {
            if (!user.getProfileImageUrl().contains(user.getUserId()))
                new CreateProfileImage(getApplicationContext()).execute(user.getUserId(), user.getProfileImageUrl());
        } catch (Exception e) {
            e.printStackTrace();
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
        mUserChangeListener.remove();
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

}
