package com.crown.onspotdelivery.page;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.crown.library.onspotlibrary.utils.emun.OSPreferenceKey;
import com.crown.library.onspotlibrary.controller.OSPreferences;
import com.crown.library.onspotlibrary.model.business.BusinessOSD;
import com.crown.library.onspotlibrary.model.user.UserOSD;
import com.crown.onspotdelivery.R;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class CurrentOrderFragment extends Fragment {

    @BindView(R.id.include_add_business)
    View mAddBusinessLayout;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_current_order, container, false);
        ButterKnife.bind(this, root);
        prepareDataset();
        return root;
    }

    @OnClick({R.id.btn_iab_add_business})
    void onClickedAddBusiness() {
        startActivity(new Intent(getActivity(), AddBusinessActivity.class));
    }

    private void prepareDataset() {
        assert getActivity() != null;
        UserOSD user = OSPreferences.getInstance(getActivity().getApplicationContext()).getObject(OSPreferenceKey.USER, UserOSD.class);
        List<BusinessOSD> businesses = user.getBusinessOSD();
        if (businesses == null || businesses.isEmpty()) {
            mAddBusinessLayout.setVisibility(View.VISIBLE);
        } else {
            // TODO: debug mode
            mAddBusinessLayout.setVisibility(View.VISIBLE);
            getCurrentOrder(user, businesses);
        }
    }

    private void getCurrentOrder(UserOSD user, List<BusinessOSD> businesses) {

    }
}
