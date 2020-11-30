package com.crown.onspotdelivery.page;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.crown.library.onspotlibrary.controller.OSPreferences;
import com.crown.library.onspotlibrary.model.user.UserOSD;
import com.crown.library.onspotlibrary.page.ContactUsActivity;
import com.crown.library.onspotlibrary.page.EditProfileActivity;
import com.crown.library.onspotlibrary.utils.OSCommonIntents;
import com.crown.library.onspotlibrary.utils.emun.OSPreferenceKey;
import com.crown.onspotdelivery.controller.AppController;
import com.crown.onspotdelivery.controller.BusinessPartnersActivity;
import com.crown.onspotdelivery.databinding.FragmentProfileBinding;

public class ProfileFragment extends Fragment {

    private UserOSD user;
    private FragmentProfileBinding binding;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        user = OSPreferences.getInstance(getContext()).getObject(OSPreferenceKey.USER, UserOSD.class);
        setUpUI();
        initClickListeners();
        return binding.getRoot();
    }

    private void initClickListeners() {
        binding.editProfileOpi.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), EditProfileActivity.class);
            intent.putExtra(EditProfileActivity.USER_ID, user.getUserId());
            startActivity(intent);
        });

        binding.myDeliveryOpi.setOnClickListener(v -> startActivity(new Intent(getContext(), MyDeliveryActivity.class)));
        binding.businessPartnersOpi.setOnClickListener(v -> startActivity(new Intent(getContext(), BusinessPartnersActivity.class)));
        binding.addBusinessPartner.setOnClickListener(v -> startActivity(new Intent(getContext(), AddBusinessActivity.class)));
        binding.commonMenuInclude.contactUsOpi.setOnClickListener(v -> startActivity(new Intent(getContext(), ContactUsActivity.class)));
        binding.commonMenuInclude.shareOpi.setOnClickListener(v -> OSCommonIntents.onIntentShareAppLink(getContext()));
        binding.commonMenuInclude.rateThisAppOpi.setOnClickListener(v -> OSCommonIntents.onIntentAppOnPlayStore(getContext()));
        binding.commonMenuInclude.logoutOpi.setOnClickListener(v -> AppController.getInstance().signOut(getActivity()));
    }

    private void setUpUI() {
        Glide.with(getContext()).load(user.getProfileImageUrl()).apply(new RequestOptions().circleCrop()).into(binding.profileImageIv);
        binding.nameTv.setText(user.getDisplayName());
        binding.emailTv.setText(user.getEmail());
    }
}