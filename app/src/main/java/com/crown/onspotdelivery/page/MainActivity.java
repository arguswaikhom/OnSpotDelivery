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
import com.crown.library.onspotlibrary.utils.emun.OSPreferenceKey;
import com.crown.onspotdelivery.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.iid.FirebaseInstanceId;

public class MainActivity extends AppCompatActivity implements EventListener<DocumentSnapshot> {

    private ListenerRegistration mUserChangeListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BottomNavigationView navView = findViewById(R.id.nav_view);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupWithNavController(navView, navController);

        UserOSD user = OSPreferences.getInstance(getApplicationContext()).getObject(OSPreferenceKey.USER, UserOSD.class);
        mUserChangeListener = FirebaseFirestore.getInstance().collection(getString(R.string.ref_user)).document(user.getUserId()).addSnapshotListener(this);
        // todo: check for the current token first
        if (user.getDeviceTokenOSD() == null || user.getDeviceTokenOSD().isEmpty()) {
            FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(instanceIdResult -> FirebaseFirestore.getInstance().collection(getString(R.string.ref_user)).document(user.getUserId()).update(getString(R.string.field_device_token_osd), FieldValue.arrayUnion(instanceIdResult.getToken())));
        }

        try {
            if (!user.getProfileImageUrl().contains(user.getUserId()))
                new CreateProfileImage(getApplicationContext()).execute(user.getUserId(), user.getProfileImageUrl());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mUserChangeListener.remove();
    }

    @Override
    public void onEvent(@Nullable DocumentSnapshot doc, @Nullable FirebaseFirestoreException e) {
        if (doc != null && doc.exists()) {
            UserOSD user = doc.toObject(UserOSD.class);
            if (user != null) {
                OSPreferences preferences = OSPreferences.getInstance(getApplicationContext());
                preferences.setObject(user, OSPreferenceKey.USER);

                Intent intent = new Intent(getString(R.string.action_osd_changes));
                sendBroadcast(intent);
            }
        }
    }

}
