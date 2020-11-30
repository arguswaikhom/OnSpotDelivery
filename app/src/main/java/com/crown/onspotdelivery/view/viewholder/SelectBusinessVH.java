package com.crown.onspotdelivery.view.viewholder;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.crown.library.onspotlibrary.controller.OSPreferences;
import com.crown.library.onspotlibrary.model.business.BusinessOSD;
import com.crown.library.onspotlibrary.model.business.BusinessV0;
import com.crown.library.onspotlibrary.model.business.BusinessV2;
import com.crown.library.onspotlibrary.model.httpresponse.OSHttpResponseV0;
import com.crown.library.onspotlibrary.model.user.UserOSD;
import com.crown.library.onspotlibrary.model.user.UserV1;
import com.crown.library.onspotlibrary.utils.OSListUtils;
import com.crown.library.onspotlibrary.utils.OSMessage;
import com.crown.library.onspotlibrary.utils.OSString;
import com.crown.library.onspotlibrary.utils.emun.BusinessRequestStatus;
import com.crown.library.onspotlibrary.utils.emun.OSPreferenceKey;
import com.crown.library.onspotlibrary.views.LoadingBounceDialog;
import com.crown.onspotdelivery.R;
import com.crown.onspotdelivery.controller.AppController;
import com.crown.onspotdelivery.databinding.LiSelectBusinessBinding;
import com.crown.onspotdelivery.view.ListItemAdapter;

import java.util.HashMap;
import java.util.Map;

public class SelectBusinessVH extends RecyclerView.ViewHolder {
    private ListItemAdapter adapter;
    private BusinessV2 business;
    private Context context;
    private UserOSD user;
    private LiSelectBusinessBinding binding;


    public SelectBusinessVH(ListItemAdapter adapter, View view) {
        super(view);
        this.adapter = adapter;
        this.context = view.getContext();
        this.binding = LiSelectBusinessBinding.bind(view);
        this.user = OSPreferences.getInstance(context.getApplicationContext()).getObject(OSPreferenceKey.USER, UserOSD.class);
        binding.btnLsbSendRequest.setOnClickListener(this::onClickedSendRequest);
    }

    public void bind(BusinessV2 business) {
        this.business = business;

        binding.tvLsbName.setText(business.getDisplayName());
        binding.tvLsbCategory.setText(business.getBusinessType());
        binding.tvLsbLocation.setText(business.getLocation().getAddressLine());
        Glide.with(context).load(business.getImageUrl()).apply(new RequestOptions()
                .transform(new CenterCrop(), new RoundedCorners(32)))
                .into(binding.ivLsbImage);

        BusinessRequestStatus status = getBusinessStatus(business);
        binding.btnLsbSendRequest.setText(status != null ? status.getButtonText(context) : context.getString(R.string.action_btn_send_request));
        if (status == BusinessRequestStatus.PENDING || status == BusinessRequestStatus.ACCEPTED) {
            binding.btnLsbSendRequest.setEnabled(false);
        } else {
            binding.btnLsbSendRequest.setEnabled(true);
        }
    }

    void onClickedSendRequest(View view) {
        LoadingBounceDialog loadingBounceDialog = new LoadingBounceDialog((Activity) context);
        loadingBounceDialog.show();

        AppController.getInstance().addToRequestQueue(new StringRequest(Request.Method.POST, OSString.apiAddBusinessRequest, response -> {
            loadingBounceDialog.dismiss();
            OSHttpResponseV0 res = OSHttpResponseV0.fromJson(response);
            if (res.getStatus() == 200) OSMessage.showSToast(context, "Request sent");
            else if (res.getStatus() == 406) {
                new AlertDialog.Builder(context).setTitle("Request denied")
                        .setMessage("You cannot have more than 10 business partners in your business partner list")
                        .setPositiveButton(context.getString(R.string.action_btn_got_it), (dialog, which) -> dialog.dismiss())
                        .show();
            }
        }, error -> {
            Toast.makeText(context, "Something went wrong!!", Toast.LENGTH_SHORT).show();
            loadingBounceDialog.dismiss();
        }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> param = new HashMap<>();
                param.put("osd", OSPreferences.getInstance(context.getApplicationContext()).getObject(OSPreferenceKey.USER, UserV1.class).toString());
                param.put("osb", new BusinessV0(business).toString());
                return param;
            }
        });
    }

    private BusinessRequestStatus getBusinessStatus(BusinessV2 business) {
        if (OSListUtils.isEmpty(user.getBusinessOSD())) return null;
        for (BusinessOSD b : user.getBusinessOSD()) {
            if (b.getBusinessRefId().equals(business.getBusinessRefId())) {
                return BusinessRequestStatus.valueOf(b.getStatus());
            }
        }
        return null;
    }
}
