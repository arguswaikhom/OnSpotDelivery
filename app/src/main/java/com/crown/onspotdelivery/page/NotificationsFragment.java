package com.crown.onspotdelivery.page;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.crown.library.onspotlibrary.controller.OSPreferences;
import com.crown.library.onspotlibrary.model.ListItem;
import com.crown.library.onspotlibrary.model.notification.OSDeliveryPartnershipRequest;
import com.crown.library.onspotlibrary.model.user.UserOSD;
import com.crown.library.onspotlibrary.utils.ListItemType;
import com.crown.library.onspotlibrary.utils.OSString;
import com.crown.library.onspotlibrary.utils.emun.OSPreferenceKey;
import com.crown.onspotdelivery.R;
import com.crown.onspotdelivery.databinding.FragmentNotificationsBinding;
import com.crown.onspotdelivery.view.ListItemAdapter;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class NotificationsFragment extends Fragment implements EventListener<QuerySnapshot> {

    private final List<ListItem> mDataset = new ArrayList<>();
    private ListItemAdapter mAdapter;
    private FragmentNotificationsBinding binding;
    private ListenerRegistration mNotificationsChangeListener;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentNotificationsBinding.inflate(inflater, container, false);

        binding.tbarFnToolBar.setTitle(getString(R.string.nav_activity));
        setHasOptionsMenu(true);
        ((AppCompatActivity) getActivity()).setSupportActionBar(binding.tbarFnToolBar);
        setUpRecycler();
        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        getNotifications();
    }

    @Override
    public void onStop() {
        super.onStop();
        mNotificationsChangeListener.remove();
    }

    private void setUpRecycler() {
        binding.rvFnNotifications.setHasFixedSize(true);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getContext());
        binding.rvFnNotifications.setLayoutManager(mLayoutManager);
        mAdapter = new ListItemAdapter(getContext(), mDataset);
        binding.rvFnNotifications.setAdapter(mAdapter);
    }

    private void getNotifications() {
        UserOSD user = OSPreferences.getInstance(getContext().getApplicationContext()).getObject(OSPreferenceKey.USER, UserOSD.class);
        mNotificationsChangeListener = FirebaseFirestore.getInstance().collection(getString(R.string.ref_notification))
                .whereArrayContains(getString(R.string.field_account), "osd::" + user.getUserId())
                .addSnapshotListener(this);
    }

    @Override
    public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
        if (queryDocumentSnapshots == null) {

        } else if (queryDocumentSnapshots.isEmpty()) {
            binding.includeFnInfoNoActivity.getRoot().setVisibility(View.VISIBLE);
            mDataset.clear();
            mAdapter.notifyDataSetChanged();
        } else {
            if (binding.includeFnInfoNoActivity.getRoot().getVisibility() == View.VISIBLE)
                binding.includeFnInfoNoActivity.getRoot().setVisibility(View.GONE);
            showNotifications(queryDocumentSnapshots.getDocuments());
        }
    }

    private void showNotifications(List<DocumentSnapshot> documents) {
        mDataset.clear();
        for (DocumentSnapshot doc : documents) {
            try {
                if (doc.exists()) {
                    Long type = (Long) doc.get(OSString.fieldType);
                    if (type == null) continue;

                    if (type == ListItemType.NOTI_DELIVERY_PARTNERSHIP_REQUEST) {
                        OSDeliveryPartnershipRequest request = doc.toObject(OSDeliveryPartnershipRequest.class);
                        if (request == null) continue;
                        request.setId(doc.getId());
                        mDataset.add(request);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        mAdapter.notifyDataSetChanged();
    }
}
