package com.crown.onspotdelivery.controller;

import com.crown.library.onspotlibrary.controller.OSPreferences;
import com.crown.library.onspotlibrary.model.user.UserOSD;
import com.crown.library.onspotlibrary.utils.emun.OSPreferenceKey;
import com.crown.onspotdelivery.R;
import com.google.firebase.database.annotations.NotNull;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.RemoteMessage;


public class FirebaseMessagingService extends com.google.firebase.messaging.FirebaseMessagingService {

    private static final String TAG = FirebaseMessagingService.class.getName();

    @Override
    public void onMessageReceived(@NotNull RemoteMessage remoteMessage) {

    }

    @Override
    public void onNewToken(@NotNull String token) {
        updateDeviceToken(token);
    }

    private void updateDeviceToken(String token) {
        UserOSD user = OSPreferences.getInstance(getApplicationContext()).getObject(OSPreferenceKey.USER, UserOSD.class);
        if (user == null) return;
        FirebaseFirestore.getInstance().collection(getString(R.string.ref_user)).document(user.getUserId()).update(getString(R.string.field_device_token_osd), FieldValue.arrayUnion(token));
    }
}