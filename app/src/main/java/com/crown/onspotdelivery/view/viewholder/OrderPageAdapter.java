package com.crown.onspotdelivery.view.viewholder;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.crown.onspotdelivery.page.OrderFragment;

public class OrderPageAdapter extends FragmentStateAdapter {
    public static final String POSITION = "POSITION";
    public static final int MY_DELIVERY_POSITION = 0;
    public static final int DELIVERABLE_ORDER_POSITION = 1;

    public OrderPageAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        Fragment fragment = new OrderFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(POSITION, position);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}
