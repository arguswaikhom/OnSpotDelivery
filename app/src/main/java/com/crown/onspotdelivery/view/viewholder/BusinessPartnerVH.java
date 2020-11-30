package com.crown.onspotdelivery.view.viewholder;

import android.app.Activity;
import android.text.Html;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.crown.library.onspotlibrary.controller.OSPreferences;
import com.crown.library.onspotlibrary.controller.OSVolley;
import com.crown.library.onspotlibrary.model.business.BusinessPartner;
import com.crown.library.onspotlibrary.model.business.BusinessV2;
import com.crown.library.onspotlibrary.model.user.UserOSD;
import com.crown.library.onspotlibrary.utils.OSMessage;
import com.crown.library.onspotlibrary.utils.OSString;
import com.crown.library.onspotlibrary.utils.emun.OSPreferenceKey;
import com.crown.library.onspotlibrary.views.LoadingBounceDialog;
import com.crown.onspotdelivery.R;
import com.crown.onspotdelivery.databinding.LiBusinessPartnerBinding;

import java.util.HashMap;
import java.util.Map;

public class BusinessPartnerVH extends RecyclerView.ViewHolder {

    private final Activity activity;
    private BusinessPartner partner;
    private final LiBusinessPartnerBinding binding;

    public BusinessPartnerVH(@NonNull View itemView) {
        super(itemView);
        activity = (Activity) itemView.getContext();
        binding = LiBusinessPartnerBinding.bind(itemView);
    }

    public void bind(BusinessPartner partner) {
        this.partner = partner;
        BusinessV2 business = partner.getBusiness();
        binding.businessInclude.tvLsbName.setText(business.getDisplayName());
        binding.businessInclude.tvLsbCategory.setText(business.getBusinessType());
        binding.businessInclude.tvLsbLocation.setText(business.getLocation().getAddressLine());
        Glide.with(activity).load(business.getImageUrl()).apply(new RequestOptions().transform(new CenterCrop(), new RoundedCorners(32))).into(binding.businessInclude.ivLsbImage);
        binding.removeBusinessPartnerIv.setOnClickListener(this::onClickedRemoveBusiness);
        binding.businessInclude.btnLsbSendRequest.setEnabled(false);
        binding.businessInclude.btnLsbSendRequest.setText(partner.getStatus().getButtonText(activity));
    }

    private void onClickedRemoveBusiness(View view) {
        BusinessV2 business = partner.getBusiness();
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(activity).setTitle(Html.fromHtml(activity.getString(R.string.title_cancel_partnership)));
        switch (partner.getStatus()) {
            case PENDING: {
                alertBuilder.setMessage(Html.fromHtml(activity.getString(R.string.msg_cancel_business_partnership_request, business.getDisplayName())));
                alertBuilder.setPositiveButton(activity.getString(R.string.action_btn_remove), ((dialog, which) -> removePartnership(OSString.apiRejectDPRequest))).show();
                break;
            }
            case ACCEPTED: {
                alertBuilder.setMessage(Html.fromHtml(activity.getString(R.string.msg_cancel_business_partnership, business.getDisplayName(), business.getDisplayName())));
                alertBuilder.setPositiveButton(activity.getString(R.string.action_btn_remove), (dialog, which) -> removePartnership(OSString.apiCancelBusinessPartnership)).show();
                break;
            }
        }
    }

    private void removePartnership(String url) {
        LoadingBounceDialog loadingDialog = new LoadingBounceDialog(activity);
        loadingDialog.show();
        OSVolley.getInstance(activity).addToRequestQueue(new StringRequest(Request.Method.POST, url, response -> {
            loadingDialog.dismiss();
            OSMessage.showSToast(activity, "Done");
        }, error -> {
            loadingDialog.dismiss();
            OSMessage.showAIBar(activity, activity.getString(R.string.msg_something_went_wrong), activity.getString(R.string.action_btn_retry), v -> binding.removeBusinessPartnerIv.performClick());
        }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> param = new HashMap<>();
                UserOSD user = OSPreferences.getInstance(activity).getObject(OSPreferenceKey.USER, UserOSD.class);
                param.put(OSString.keyInitiator, OSString.initiatorDelivery);
                param.put(OSString.fieldUserId, user.getUserId());
                param.put(OSString.fieldBusinessRefId, partner.getBusiness().getBusinessRefId());
                return param;
            }
        });
    }
}
