package com.crown.onspotdelivery.page;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.crown.library.onspotlibrary.controller.OSPreferences;
import com.crown.library.onspotlibrary.model.ListItem;
import com.crown.library.onspotlibrary.model.UnSupportedContent;
import com.crown.library.onspotlibrary.model.order.OSOrder;
import com.crown.library.onspotlibrary.model.order.OSOrderPathDetails;
import com.crown.library.onspotlibrary.model.user.UserOSD;
import com.crown.library.onspotlibrary.utils.emun.OSPreferenceKey;
import com.crown.onspotdelivery.BuildConfig;
import com.crown.onspotdelivery.databinding.FragmentOrderBinding;
import com.crown.onspotdelivery.model.OrderViewModel;
import com.crown.onspotdelivery.view.ListItemAdapter;
import com.crown.onspotdelivery.view.viewholder.OrderPageAdapter;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class OrderFragment extends Fragment {
    private int position;
    private UserOSD user;
    private ListItemAdapter mAdapter;
    private final List<ListItem> mDataset = new ArrayList<>();
    private FragmentOrderBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentOrderBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        OrderViewModel orderViewModel = new ViewModelProvider(requireParentFragment()).get(OrderViewModel.class);
        user = OSPreferences.getInstance(getContext()).getObject(OSPreferenceKey.USER, UserOSD.class);
        binding.orderListRv.setHasFixedSize(true);
        binding.orderListRv.setLayoutManager(new LinearLayoutManager(getContext()));
        mAdapter = new ListItemAdapter(getContext(), mDataset);
        binding.orderListRv.setAdapter(mAdapter);

        Bundle bundle = getArguments();
        if (bundle == null) return;
        position = bundle.getInt(OrderPageAdapter.POSITION);
        if (position == OrderPageAdapter.MY_DELIVERY_POSITION) {
            orderViewModel.getMyDelivery().observe(getActivity(), this::handleOrder);
        } else if (position == OrderPageAdapter.DELIVERABLE_ORDER_POSITION) {
            orderViewModel.getDeliverableOrder().observe(getActivity(), this::handleOrder);
        }
    }

    private void handleOrder(QuerySnapshot snapshots) {
        if (snapshots == null || snapshots.isEmpty()) {
            mDataset.clear();
            mAdapter.notifyDataSetChanged();
            if (binding.noDataIllv.getVisibility() != View.VISIBLE) {
                binding.noDataIllv.setVisibility(View.VISIBLE);
            }
        } else {
            showOrder(snapshots);
        }
    }

    private void showOrder(QuerySnapshot snapshots) {
        mDataset.clear();
        boolean hasUnsupportedContent = false;
        List<ListItem> tempList = new ArrayList<>();
        UnSupportedContent unSupportedContent = new UnSupportedContent(BuildConfig.VERSION_CODE, BuildConfig.VERSION_NAME, user.getUserId(), OrderFragment.class.getName());

        if (binding.noDataIllv.getVisibility() == View.VISIBLE) {
            binding.noDataIllv.setVisibility(View.GONE);
        }

        if (position == OrderPageAdapter.MY_DELIVERY_POSITION) {
            for (DocumentSnapshot doc : snapshots.getDocuments()) {
                try {
                    OSOrderPathDetails order = doc.toObject(OSOrderPathDetails.class);
                    order.setOrderId(doc.getId());
                    tempList.add(order);
                } catch (Exception e) {
                    hasUnsupportedContent = true;
                    unSupportedContent.addItem(doc);
                    unSupportedContent.addException(e.getMessage());
                    e.printStackTrace();
                }
            }
        } else if (position == OrderPageAdapter.DELIVERABLE_ORDER_POSITION) {
            for (DocumentSnapshot doc : snapshots.getDocuments()) {
                try {
                    OSOrder order = doc.toObject(OSOrder.class);
                    order.setOrderId(doc.getId());
                    tempList.add(order);
                } catch (Exception e) {
                    hasUnsupportedContent = true;
                    unSupportedContent.addItem(doc);
                    unSupportedContent.addException(e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        if (hasUnsupportedContent) mDataset.add(unSupportedContent);
        mDataset.addAll(tempList);
        mAdapter.notifyDataSetChanged();
    }
}
