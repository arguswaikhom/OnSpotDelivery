package com.crown.onspotdelivery.page;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.crown.library.onspotlibrary.controller.OSPreferences;
import com.crown.library.onspotlibrary.model.ListItem;
import com.crown.library.onspotlibrary.model.UnSupportedContent;
import com.crown.library.onspotlibrary.model.order.OSMyDeliveryOrderOSD;
import com.crown.library.onspotlibrary.model.user.UserOSD;
import com.crown.library.onspotlibrary.utils.OSMessage;
import com.crown.library.onspotlibrary.utils.OSString;
import com.crown.library.onspotlibrary.utils.emun.OSPreferenceKey;
import com.crown.library.onspotlibrary.utils.emun.OrderStatus;
import com.crown.library.onspotlibrary.views.LoadingBounceDialog;
import com.crown.onspotdelivery.BuildConfig;
import com.crown.onspotdelivery.R;
import com.crown.onspotdelivery.databinding.ActivityMyDeliveryBinding;
import com.crown.onspotdelivery.view.ListItemAdapter;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

// todo: if there is no business partner, display add business partner to start deliver product
public class MyDeliveryActivity extends AppCompatActivity {

    private final String TAG = MyDeliveryActivity.class.getName();
    private UserOSD user;
    private ListItemAdapter adapter;
    private LoadingBounceDialog loadingDialog;
    private ActivityMyDeliveryBinding binding;
    private List<ListItem> dataset = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMyDeliveryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        loadingDialog = new LoadingBounceDialog(this);
        user = OSPreferences.getInstance(getApplicationContext()).getObject(OSPreferenceKey.USER, UserOSD.class);
        adapter = new ListItemAdapter(this, dataset);
        binding.myDeliveryListRv.setLayoutManager(new LinearLayoutManager(this));
        binding.myDeliveryListRv.setAdapter(adapter);
        getOrderDataset();
    }

    private void getOrderDataset() {
        loadingDialog.show();
        FirebaseFirestore.getInstance().collection(OSString.refOrder)
                .whereEqualTo(FieldPath.of(OSString.fieldDelivery, OSString.fieldUserId), user.getUserId())
                .whereEqualTo(OSString.fieldStatus, OrderStatus.DELIVERED)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot == null) {
                        OSMessage.showIBar(this, getString(R.string.msg_something_went_wrong));
                    } else if (snapshot.isEmpty()) {
                        OSMessage.showIBar(this, getString(R.string.msg_no_date));
                    } else {
                        boolean hasUnsupportedContent = false;
                        UnSupportedContent us = new UnSupportedContent(BuildConfig.VERSION_CODE, BuildConfig.VERSION_NAME, user.getUserId(), this.getPackageName());
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            try {
                                OSMyDeliveryOrderOSD order = doc.toObject(OSMyDeliveryOrderOSD.class);
                                assert order != null;
                                order.setOrderId(doc.getId());
                                dataset.add(order);
                            } catch (Exception e) {
                                e.printStackTrace();
                                us.addItem(doc);
                                us.addException(e.toString());
                                hasUnsupportedContent = true;
                                Log.d(TAG, "Order id: " + doc.getId());
                            }
                        }
                        if (hasUnsupportedContent) dataset.add(us);
                        adapter.notifyDataSetChanged();
                    }
                    loadingDialog.dismiss();
                })
                .addOnFailureListener(e -> {
                    loadingDialog.dismiss();
                    OSMessage.showIBar(this, getString(R.string.msg_failed_to_load));
                });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) onBackPressed();
        return super.onOptionsItemSelected(item);
    }
}