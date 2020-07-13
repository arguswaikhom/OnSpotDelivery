package com.crown.onspotdelivery.view.viewholder;

import android.content.Context;
import android.content.DialogInterface;
import android.text.Html;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.crown.library.onspotlibrary.controller.OSPreferences;
import com.crown.library.onspotlibrary.model.business.BusinessOSD;
import com.crown.library.onspotlibrary.model.notification.DeliveryPartnershipRequest;
import com.crown.library.onspotlibrary.model.user.UserOSD;
import com.crown.library.onspotlibrary.utils.emun.BusinessRequestStatus;
import com.crown.library.onspotlibrary.utils.emun.OSPreferenceKey;
import com.crown.onspotdelivery.R;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class DeliveryPartnershipRequestVH extends RecyclerView.ViewHolder {

    @BindView(R.id.iv_ndpr_image)
    ImageView imageIV;
    @BindView(R.id.tv_ndpr_body)
    TextView bodyTV;
    @BindView(R.id.btn_ndpr_cancel)
    Button cancelBtn;

    private Context context;
    private DeliveryPartnershipRequest request;

    public DeliveryPartnershipRequestVH(@NonNull View itemView) {
        super(itemView);
        this.context = itemView.getContext();
        ButterKnife.bind(this, itemView);
    }

    public void bind(DeliveryPartnershipRequest request) {
        this.request = request;
        Glide.with(context).load(request.getOsb().getImageUrl()).circleCrop().into(imageIV);

        if (request.getStatus() == BusinessRequestStatus.PENDING) {
            cancelBtn.setVisibility(View.VISIBLE);
            bodyTV.setText(Html.fromHtml("Delivery partnership request sent to <b>" + request.getOsb().getDisplayName() + "</b>"));
        } else if (request.getStatus() == BusinessRequestStatus.ACCEPTED) {
            cancelBtn.setVisibility(View.GONE);
            bodyTV.setText(Html.fromHtml("<b>" + request.getOsb().getDisplayName() + "</b> accepted your delivery partnership request"));
        } else if (request.getStatus() == BusinessRequestStatus.REJECTED) {
            cancelBtn.setVisibility(View.GONE);
            bodyTV.setText(Html.fromHtml("<b>" + request.getOsb().getDisplayName() + "</b> rejected your delivery partnership request"));
        }
    }

    @OnClick(R.id.btn_ndpr_cancel)
    void onClickedCancel() {
        new AlertDialog.Builder(context).setTitle("Cancel Request")
                .setMessage(Html.fromHtml("Are you sure you want to cancel the delivery partnership request with <b>" + request.getOsb().getDisplayName() + "</b>?"))
                .setNegativeButton("No", null)
                .setPositiveButton("Yes", this::cancelRequest)
                .show();
    }

    private void cancelRequest(DialogInterface dialogInterface, int i) {
        UserOSD user = OSPreferences.getInstance(context.getApplicationContext()).getObject(OSPreferenceKey.USER, UserOSD.class);
        Map<String, Object> param = new HashMap<>();
        param.put("businessOSD", FieldValue.arrayRemove(new BusinessOSD(request.getOsb().getBusinessRefId(), BusinessRequestStatus.PENDING.name())));

        FirebaseFirestore.getInstance().collection(context.getString(R.string.ref_user)).document(user.getUserId()).update(param);
        FirebaseFirestore.getInstance().collection(context.getString(R.string.ref_notification)).document(request.getId()).delete();
    }
}
