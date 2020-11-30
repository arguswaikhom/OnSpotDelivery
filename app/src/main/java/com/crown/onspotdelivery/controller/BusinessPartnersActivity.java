package com.crown.onspotdelivery.controller;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.crown.library.onspotlibrary.controller.OSPreferences;
import com.crown.library.onspotlibrary.controller.OSViewAnimation;
import com.crown.library.onspotlibrary.model.ListItem;
import com.crown.library.onspotlibrary.model.UnSupportedContent;
import com.crown.library.onspotlibrary.model.business.BusinessOSD;
import com.crown.library.onspotlibrary.model.business.BusinessPartner;
import com.crown.library.onspotlibrary.model.business.BusinessV2;
import com.crown.library.onspotlibrary.model.user.UserOSD;
import com.crown.library.onspotlibrary.utils.OSBroadcastReceiver;
import com.crown.library.onspotlibrary.utils.OSListUtils;
import com.crown.library.onspotlibrary.utils.OSMessage;
import com.crown.library.onspotlibrary.utils.OSString;
import com.crown.library.onspotlibrary.utils.callback.OnReceiveOSBroadcasts;
import com.crown.library.onspotlibrary.utils.emun.BusinessRequestStatus;
import com.crown.library.onspotlibrary.utils.emun.OSPreferenceKey;
import com.crown.library.onspotlibrary.views.LoadingBounceDialog;
import com.crown.onspotdelivery.BuildConfig;
import com.crown.onspotdelivery.R;
import com.crown.onspotdelivery.databinding.ActivityBusinessPartnersBinding;
import com.crown.onspotdelivery.page.AddBusinessActivity;
import com.crown.onspotdelivery.view.ListItemAdapter;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class BusinessPartnersActivity extends AppCompatActivity implements OnReceiveOSBroadcasts {
    private ListItemAdapter adapter;
    private LoadingBounceDialog loadingDialog;
    private QuerySnapshot datasetSnapshot;
    private IntentFilter mIntentFilter;
    private boolean isFirstRefresh = true;
    private OSBroadcastReceiver mBroadcastReceiver;
    private ListenerRegistration businessPartnersListener;
    private ActivityBusinessPartnersBinding binding;
    private List<ListItem> dataset = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBusinessPartnersBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        loadingDialog = new LoadingBounceDialog(this);
        mBroadcastReceiver = new OSBroadcastReceiver(this);
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(getString(R.string.action_osd_changes));
        adapter = new ListItemAdapter(this, dataset);
        binding.businessPartnersListRv.setLayoutManager(new LinearLayoutManager(this));
        binding.businessPartnersListRv.setAdapter(adapter);
        binding.noBusinessInclude.addBusinessBtn.setOnClickListener(v -> startActivity(new Intent(this, AddBusinessActivity.class)));
        getDataset();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mBroadcastReceiver, mIntentFilter);
        if (!isFirstRefresh) rePopulateDataset(datasetSnapshot);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mBroadcastReceiver);
    }

    @SuppressWarnings("unchecked")
    private void getDataset() {
        List<String> bhs = new ArrayList<>();
        UserOSD user = OSPreferences.getInstance(getApplicationContext()).getObject(OSPreferenceKey.USER, UserOSD.class);

        if (!OSListUtils.isEmpty(user.getBusinessOSD())) {
            for (BusinessOSD b : user.getBusinessOSD()) bhs.add(b.getBusinessRefId());
        } else {
            // If the user don't have any business partner or any pending request, show add business layout
            showAddBusinessView();
            return;
        }

        loadingDialog.show();
        businessPartnersListener = FirebaseFirestore.getInstance().collection(OSString.refBusiness)
                .whereIn(OSString.fieldBusinessRefId, bhs)
                .addSnapshotListener((snapshot, e) -> {
                    isFirstRefresh = false;
                    datasetSnapshot = snapshot;
                    rePopulateDataset(snapshot);
                });
    }

    private void rePopulateDataset(QuerySnapshot snapshot) {
        dataset.clear();
        adapter.notifyDataSetChanged();
        showBusinessPartnerView();
        if (snapshot == null) {
            OSMessage.showIBar(this, getString(R.string.msg_something_went_wrong));
        } else if (snapshot.isEmpty()) {
            OSMessage.showIBar(this, getString(R.string.msg_no_date));
        } else {
            boolean hasUnsupportedContent = false;
            UserOSD user = OSPreferences.getInstance(getApplicationContext()).getObject(OSPreferenceKey.USER, UserOSD.class);
            UnSupportedContent us = new UnSupportedContent(BuildConfig.VERSION_CODE, BuildConfig.VERSION_NAME, user.getUserId(), this.getPackageName());
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                try {
                    BusinessV2 business = doc.toObject(BusinessV2.class);
                    assert business != null;
                    ArrayList<String> imageUrls = (ArrayList<String>) doc.get(OSString.fieldImageUrls);
                    business.setImageUrl(OSListUtils.isEmpty(imageUrls) ? "" : imageUrls.get(0));
                    BusinessPartner businessPartner = new BusinessPartner();
                    businessPartner.setBusiness(business);
                    for (BusinessOSD b : user.getBusinessOSD()) {
                        if (business.getBusinessRefId().equals(b.getBusinessRefId())) {
                            businessPartner.setStatus(BusinessRequestStatus.valueOf(b.getStatus()));
                        }
                    }
                    dataset.add(businessPartner);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    us.addItem(doc);
                    us.addException(ex.toString());
                    hasUnsupportedContent = true;
                }
            }
            if (hasUnsupportedContent) dataset.add(us);
            adapter.notifyDataSetChanged();
        }
        loadingDialog.dismiss();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (businessPartnersListener != null) businessPartnersListener.remove();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.option_home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                onBackPressed();
                return true;
            }
            case R.id.nav_oh_add_business: {
                startActivity(new Intent(this, AddBusinessActivity.class));
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onReceiveBroadcast(Context context, Intent intent) {
        getDataset();
    }

    private void showAddBusinessView() {
        OSViewAnimation.collapse(binding.businessPartnersListRv);
        OSViewAnimation.expand(binding.noBusinessInclude.getRoot());
    }

    private void showBusinessPartnerView() {
        OSViewAnimation.collapse(binding.noBusinessInclude.getRoot());
        OSViewAnimation.expand(binding.businessPartnersListRv);
    }
}