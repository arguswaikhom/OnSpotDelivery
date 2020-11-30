package com.crown.onspotdelivery.page;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.crown.library.onspotlibrary.controller.OSPreferences;
import com.crown.library.onspotlibrary.model.user.UserOSD;
import com.crown.library.onspotlibrary.utils.OSBroadcastReceiver;
import com.crown.library.onspotlibrary.utils.OSListUtils;
import com.crown.library.onspotlibrary.utils.OSString;
import com.crown.library.onspotlibrary.utils.callback.OnReceiveOSBroadcasts;
import com.crown.library.onspotlibrary.utils.emun.OSPreferenceKey;
import com.crown.onspotdelivery.R;
import com.crown.onspotdelivery.databinding.FragmentHomeBinding;
import com.crown.onspotdelivery.model.OrderViewModel;
import com.crown.onspotdelivery.utils.BusinessUtils;
import com.crown.onspotdelivery.view.viewholder.OrderPageAdapter;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment implements OnReceiveOSBroadcasts {

    private UserOSD user;
    private int myDeliveryOrderCount;
    private int deliverableOrderCount;
    private OrderViewModel orderViewModel;
    private FragmentHomeBinding binding;
    private IntentFilter mIntentFilter;
    private OSBroadcastReceiver mBroadcastReceiver;
    private ListenerRegistration myDeliveryListener;
    private ListenerRegistration deliverableOrderListener;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        ((AppCompatActivity) getActivity()).setSupportActionBar(binding.toolbar);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        orderViewModel = new ViewModelProvider(this).get(OrderViewModel.class);
        user = OSPreferences.getInstance(getContext()).getObject(OSPreferenceKey.USER, UserOSD.class);
        OrderPageAdapter adapter = new OrderPageAdapter(this);
        binding.pager.setAdapter(adapter);
        setHasOptionsMenu(true);

        mBroadcastReceiver = new OSBroadcastReceiver(this);
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(getString(R.string.action_osd_changes));
        requireActivity().registerReceiver(mBroadcastReceiver, mIntentFilter);

        prepareDataset();
        updateTabLayout();
    }

    private void updateTabLayout() {
        new TabLayoutMediator(binding.tabLayout, binding.pager, (tab, position) -> {
            if (position == 0) {
                tab.setText("My delivery" + (myDeliveryOrderCount == 0 ? "" : String.format(Locale.ENGLISH, " (%d)", myDeliveryOrderCount)));
            } else if (position == 1) {
                tab.setText("Active order" + (deliverableOrderCount == 0 ? "" : String.format(Locale.ENGLISH, " (%d)", deliverableOrderCount)));
            }
        }).attach();
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

    private void prepareDataset() {
        // Get all the accepted business if there is any
        List<String> bussList = BusinessUtils.getAcceptedBusinessIds(user.getBusinessOSD());

        // If there is no accepted business, show no business layout and guide the user to send business request
        if (OSListUtils.isEmpty(bussList)) {
            if (binding.noBusinessInclude.getRoot().getVisibility() != View.VISIBLE) {
                binding.noBusinessInclude.getRoot().setVisibility(View.VISIBLE);
                binding.tabLayout.setVisibility(View.GONE);
                binding.pager.setVisibility(View.GONE);
                binding.noBusinessInclude.addBusinessBtn.setOnClickListener(v -> startActivity(new Intent(getActivity(), AddBusinessActivity.class)));
            }
            return;
        }

        Query query = FirebaseFirestore.getInstance().collection(OSString.refOrder)
                .whereIn(FieldPath.of(OSString.fieldBusiness, OSString.fieldBusinessRefId), bussList);

        // Listen to all the current order which the current user accepted to deliver
        myDeliveryListener = query.whereEqualTo(OSString.fieldIsActiveOrder, true)
                .whereEqualTo(FieldPath.of(OSString.fieldDelivery, OSString.fieldUserId), user.getUserId())
                .addSnapshotListener((snap, e) -> {
                    myDeliveryOrderCount = snap != null && !snap.isEmpty() ? snap.size() : 0;
                    updateTabLayout();
                    orderViewModel.setMyDelivery(snap);
                });

        // Listen to all the current order which has no delivery assign
        deliverableOrderListener = query.whereEqualTo(OSString.fieldIsDeliverableOrder, true)
                .whereEqualTo(OSString.fieldDelivery, null)
                .addSnapshotListener((snap, e) -> {
                    deliverableOrderCount = snap != null && !snap.isEmpty() ? snap.size() : 0;
                    updateTabLayout();
                    orderViewModel.setDeliverableOrder(snap);
                });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (deliverableOrderListener != null) deliverableOrderListener.remove();
        if (myDeliveryListener != null) myDeliveryListener.remove();
        requireActivity().unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    public void onReceiveBroadcast(Context context, Intent intent) {
        // Get accepted business partner list from the current user object
        List<String> oldBussPartners = BusinessUtils.getAcceptedBusinessIds(user.getBusinessOSD());

        // Get accepted business partner list from the updated user object
        UserOSD updatedUser = OSPreferences.getInstance(getContext()).getObject(OSPreferenceKey.USER, UserOSD.class);
        List<String> newBussPartners = BusinessUtils.getAcceptedBusinessIds(updatedUser.getBusinessOSD());

        user = updatedUser;

        if (!oldBussPartners.equals(newBussPartners)) {
            prepareDataset();
        }
    }
}
