package com.crown.onspotdelivery.view.viewholder;

import android.content.Context;
import android.content.DialogInterface;
import android.text.Html;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.crown.library.onspotlibrary.controller.OSPreferences;
import com.crown.library.onspotlibrary.model.business.BusinessOSD;
import com.crown.library.onspotlibrary.model.business.BusinessV0;
import com.crown.library.onspotlibrary.model.notification.OSDeliveryPartnershipRequest;
import com.crown.library.onspotlibrary.model.user.UserOSD;
import com.crown.library.onspotlibrary.utils.OSFirebaseDocUtils;
import com.crown.library.onspotlibrary.utils.emun.BusinessRequestStatus;
import com.crown.library.onspotlibrary.utils.emun.OSPreferenceKey;
import com.crown.onspotdelivery.R;
import com.crown.onspotdelivery.databinding.NotiDeliveryPartnershipRequestBinding;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class DeliveryPartnershipRequestVH extends RecyclerView.ViewHolder {
    private Context context;
    private BusinessV0 business;
    private OSDeliveryPartnershipRequest request;
    private NotiDeliveryPartnershipRequestBinding binding;

    public DeliveryPartnershipRequestVH(@NonNull View itemView) {
        super(itemView);
        this.context = itemView.getContext();
        this.binding = NotiDeliveryPartnershipRequestBinding.bind(itemView);
        binding.btnNdprCancel.setOnClickListener(this::onClickedCancel);
    }

    public void bind(OSDeliveryPartnershipRequest request) {
        this.request = request;

        OSFirebaseDocUtils.getBusiness(request.getOsb(), (doc, e) -> {
            if (doc == null) return;
            business = doc.toObject(BusinessV0.class);

            Glide.with(context).load(business.getImageUrl()).circleCrop().into(binding.ivNdprImage);

            if (request.getStatus() == BusinessRequestStatus.PENDING) {
                binding.btnNdprCancel.setVisibility(View.VISIBLE);
                binding.tvNdprBody.setText(Html.fromHtml("Delivery partnership request sent to <b>" + business.getDisplayName() + "</b>"));
            } else if (request.getStatus() == BusinessRequestStatus.ACCEPTED) {
                binding.btnNdprCancel.setVisibility(View.GONE);
                binding.tvNdprBody.setText(Html.fromHtml("<b>" + business.getDisplayName() + "</b> accepted your delivery partnership request"));
            } else if (request.getStatus() == BusinessRequestStatus.REJECTED) {
                binding.btnNdprCancel.setVisibility(View.GONE);
                binding.tvNdprBody.setText(Html.fromHtml("<b>" + business.getDisplayName() + "</b> rejected your delivery partnership request"));
            }
        });
    }

    void onClickedCancel(View view) {
        if (business == null) return;
        new AlertDialog.Builder(context).setTitle("Cancel Request")
                .setMessage(Html.fromHtml("Are you sure you want to cancel the delivery partnership request with <b>" + business.getDisplayName() + "</b>?"))
                .setNegativeButton("No", null)
                .setPositiveButton("Yes", this::cancelRequest)
                .show();
    }

    private void cancelRequest(DialogInterface dialogInterface, int i) {
        if (business == null) return;
        // todo: change with HTTP call
        UserOSD user = OSPreferences.getInstance(context.getApplicationContext()).getObject(OSPreferenceKey.USER, UserOSD.class);
        Map<String, Object> param = new HashMap<>();
        param.put("businessOSD", FieldValue.arrayRemove(new BusinessOSD(business.getBusinessRefId(), BusinessRequestStatus.PENDING.name())));

        FirebaseFirestore.getInstance().collection(context.getString(R.string.ref_user)).document(user.getUserId()).update(param);
        FirebaseFirestore.getInstance().collection(context.getString(R.string.ref_notification)).document(request.getId()).delete();
    }
}
