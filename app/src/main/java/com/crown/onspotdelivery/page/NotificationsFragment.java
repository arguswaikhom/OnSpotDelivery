package com.crown.onspotdelivery.page;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.crown.library.onspotlibrary.controller.OSPreferences;
import com.crown.library.onspotlibrary.model.ListItem;
import com.crown.library.onspotlibrary.model.notification.DeliveryPartnershipRequest;
import com.crown.library.onspotlibrary.model.user.UserOSD;
import com.crown.library.onspotlibrary.utils.ListItemType;
import com.crown.library.onspotlibrary.utils.emun.OSPreferenceKey;
import com.crown.onspotdelivery.R;
import com.crown.onspotdelivery.view.ListItemAdapter;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class NotificationsFragment extends Fragment implements EventListener<QuerySnapshot> {

    @BindView(R.id.include_fn_info_no_activity)
    View mNoActivityLayout;
    private List<ListItem> mDataset;
    private ListItemAdapter mAdapter;
    private ListenerRegistration mNotificationsChangeListener;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_notifications, container, false);
        ButterKnife.bind(this, root);

        Toolbar toolbar = root.findViewById(R.id.tbar_fn_tool_bar);
        toolbar.setTitle(getString(R.string.nav_activity));
        setHasOptionsMenu(true);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);

        setUpRecycler(root);
        return root;
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

    private void setUpRecycler(View root) {
        RecyclerView mRecyclerView = root.findViewById(R.id.rv_fn_notifications);
        mRecyclerView.setHasFixedSize(true);

        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(mLayoutManager);

        mDataset = new ArrayList<>();
        mAdapter = new ListItemAdapter(getContext(), mDataset);
        mRecyclerView.setAdapter(mAdapter);
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
            mNoActivityLayout.setVisibility(View.VISIBLE);
            mDataset.clear();
            mAdapter.notifyDataSetChanged();
        } else {
            if (mNoActivityLayout.getVisibility() == View.VISIBLE)
                mNoActivityLayout.setVisibility(View.GONE);
            showNotifications(queryDocumentSnapshots.getDocuments());
        }
    }

    private void showNotifications(List<DocumentSnapshot> documents) {
        mDataset.clear();
        for (DocumentSnapshot doc : documents) {
            if (doc.exists()) {
                Long type = (Long) doc.get(getString(R.string.field_type));
                if (type == null) continue;

                if (type == ListItemType.DELIVERY_PARTNERSHIP_REQUEST) {
                    DeliveryPartnershipRequest request = doc.toObject(DeliveryPartnershipRequest.class);
                    if (request == null) continue;
                    request.setId(doc.getId());
                    mDataset.add(request);
                }
            }
        }
        mAdapter.notifyDataSetChanged();
    }
}
