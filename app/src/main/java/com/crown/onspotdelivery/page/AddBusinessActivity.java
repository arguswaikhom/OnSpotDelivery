package com.crown.onspotdelivery.page;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.crown.library.onspotlibrary.model.ListItem;
import com.crown.library.onspotlibrary.model.business.BusinessV2;
import com.crown.library.onspotlibrary.utils.OSBroadcastReceiver;
import com.crown.library.onspotlibrary.utils.callback.OnReceiveOSBroadcasts;
import com.crown.library.onspotlibrary.views.OSSearchView;
import com.crown.onspotdelivery.R;
import com.crown.onspotdelivery.view.ListItemAdapter;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class AddBusinessActivity extends AppCompatActivity implements OnReceiveOSBroadcasts {

    private static final String TAG = AddBusinessActivity.class.getName();
    private List<ListItem> mDataset;
    private ListItemAdapter mAdapter;
    private IntentFilter mIntentFilter;
    private RecyclerView mRecyclerView;
    private OSBroadcastReceiver mBroadcastReceiver;

    @BindView(R.id.pbar_aab_loading)
    ProgressBar mLoadingPBar;
    @BindView(R.id.sv_aab_search)
    OSSearchView mSearchSV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_business);
        ButterKnife.bind(this);

        mBroadcastReceiver = new OSBroadcastReceiver(this);
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(getString(R.string.action_osd_changes));
        mSearchSV.getField().addTextChangedListener(mSearchFieldWatcher);

        setUpRecycler();
        loadDataset();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mBroadcastReceiver, mIntentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mBroadcastReceiver);
    }

    private TextWatcher mSearchFieldWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            mAdapter.getFilter().filter(s);
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };

    private void setUpRecycler() {
        mRecyclerView = findViewById(R.id.rv_aab_business_list);
        mRecyclerView.setHasFixedSize(true);

        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        mDataset = new ArrayList<>();
        mAdapter = new ListItemAdapter(this, mDataset);
        mRecyclerView.setAdapter(mAdapter);
    }

    private void loadDataset() {
        mLoadingPBar.setVisibility(View.VISIBLE);
        String collection = getString(R.string.ref_business);
        FirebaseFirestore.getInstance().collection(collection).get()
                .addOnSuccessListener(this::onLoadDatasetSuccessful).addOnFailureListener(this::onLoadDatasetFailure);
    }

    @SuppressWarnings("unchecked")
    private void onLoadDatasetSuccessful(QuerySnapshot queryDocumentSnapshots) {
        mLoadingPBar.setVisibility(View.GONE);
        if (!queryDocumentSnapshots.isEmpty()) {
            mDataset.clear();
            for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                if (doc.exists()) {
                    BusinessV2 v2 = doc.toObject(BusinessV2.class);
                    if (v2 == null) continue;
                    ArrayList<String> imageUrls = (ArrayList<String>) doc.get(getString(R.string.field_image_urls));
                    v2.setImageUrl(imageUrls == null || imageUrls.isEmpty() ? "" : imageUrls.get(0));
                    mDataset.add(v2);
                }
            }
            Collections.sort(mDataset, ((o1, o2) -> ((BusinessV2) o1).getDisplayName().compareTo(((BusinessV2) o2).getDisplayName())));
            mAdapter.notifyDataSetChanged();
        }
    }

    private void onLoadDatasetFailure(Exception e) {
        mLoadingPBar.setVisibility(View.GONE);

    }

    @Override
    public void onReceiveBroadcast(Context context, Intent intent) {
        mRecyclerView.setAdapter(mAdapter);
    }
}