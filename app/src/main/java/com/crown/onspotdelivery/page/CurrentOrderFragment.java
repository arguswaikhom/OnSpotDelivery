package com.crown.onspotdelivery.page;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.crown.library.onspotlibrary.controller.OSPreferences;
import com.crown.library.onspotlibrary.model.ListItem;
import com.crown.library.onspotlibrary.model.OSOrder;
import com.crown.library.onspotlibrary.model.business.BusinessOSD;
import com.crown.library.onspotlibrary.model.user.UserOSD;
import com.crown.library.onspotlibrary.utils.emun.OSPreferenceKey;
import com.crown.library.onspotlibrary.utils.emun.OrderStatus;
import com.crown.onspotdelivery.R;
import com.crown.onspotdelivery.view.ListItemAdapter;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class CurrentOrderFragment extends Fragment {

    @BindView(R.id.include_add_business)
    View mAddBusinessLayout;
    @BindView(R.id.ll_fco_main)
    View mainView;

    private List<ListItem> mDataset;
    private ListItemAdapter mAdapter;
    private ListenerRegistration mOrderListener;
    private ListenerRegistration mMyOrderListener;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_current_order, container, false);
        ButterKnife.bind(this, root);
        Toolbar toolbar = root.findViewById(R.id.tbar_fco);
        toolbar.setTitle("Order");
        setHasOptionsMenu(true);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        prepareDataset();
        setUpRecycler(root);
        return root;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mOrderListener != null) mOrderListener.remove();
    }

    @OnClick({R.id.btn_iab_add_business})
    void onClickedAddBusiness() {
        startActivity(new Intent(getActivity(), AddBusinessActivity.class));
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.option_home, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.nav_oh_add_business) {
            startActivity(new Intent(getActivity(), AddBusinessActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setUpRecycler(View root) {
        RecyclerView mRecyclerView = root.findViewById(R.id.rv_fco_notifications);
        mRecyclerView.setHasFixedSize(true);

        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(mLayoutManager);

        mDataset = new ArrayList<>();
        mAdapter = new ListItemAdapter(getContext(), mDataset);
        mRecyclerView.setAdapter(mAdapter);
    }

    private void prepareDataset() {
        assert getActivity() != null;
        UserOSD user = OSPreferences.getInstance(getActivity().getApplicationContext()).getObject(OSPreferenceKey.USER, UserOSD.class);
        List<BusinessOSD> businesses = user.getBusinessOSD();
        if (businesses == null || businesses.isEmpty()) {
            mAddBusinessLayout.setVisibility(View.VISIBLE);
            mainView.setVisibility(View.GONE);
        } else {
            List<String> bussList = new ArrayList<>();
            for (BusinessOSD b : businesses) bussList.add(b.getBusinessRefId());
            String[] filter = new String[]{OrderStatus.ORDERED.name(), OrderStatus.ACCEPTED.name(), OrderStatus.PREPARING.name(), OrderStatus.READY.name(), OrderStatus.ON_THE_WAY.name()};

            Query query = FirebaseFirestore.getInstance().collection(getString(R.string.ref_order))
                    .whereIn(FieldPath.of(getString(R.string.field_business), getString(R.string.field_business_ref_id)), bussList)
                    .whereIn(getString(R.string.field_status), Arrays.asList(filter));
            mOrderListener = query.whereEqualTo(getString(R.string.field_delivery), null).addSnapshotListener(this::onOrderEvent);
            mMyOrderListener = query.whereEqualTo(FieldPath.of(getString(R.string.field_delivery), getString(R.string.field_user_id)), user.getUserId()).addSnapshotListener(this::onMyEvent);
        }
    }

    private void onMyEvent(QuerySnapshot snapshots, FirebaseFirestoreException e) {
        if (snapshots == null) {

        } else if (snapshots.isEmpty()) {
            // TODO: Display no current order info
        } else {
            if (mainView.getVisibility() != View.VISIBLE) mainView.setVisibility(View.VISIBLE);

        }
    }

    private void onOrderEvent(QuerySnapshot snapshots, FirebaseFirestoreException e) {
        if (snapshots == null) {

        } else if (snapshots.isEmpty()) {
            // TODO: Display no current order info
        } else {
            if (mainView.getVisibility() != View.VISIBLE) mainView.setVisibility(View.VISIBLE);
            showOrder(snapshots.getDocuments());
            Log.d("debug", mDataset.toString());
        }
    }

    private void getCurrentOrder(UserOSD user, List<BusinessOSD> businesses) {

    }

    private void showOrder(List<DocumentSnapshot> documents) {
        mDataset.clear();
        for (DocumentSnapshot doc : documents) {
            try {
                OSOrder order = doc.toObject(OSOrder.class);
                order.setOrderId(doc.getId());
                mDataset.add(order);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        mAdapter.notifyDataSetChanged();
    }
}
