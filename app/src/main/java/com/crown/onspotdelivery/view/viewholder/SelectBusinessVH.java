package com.crown.onspotdelivery.view.viewholder;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.AuthFailureError;
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
import com.crown.library.onspotlibrary.model.user.UserOSD;
import com.crown.library.onspotlibrary.model.user.UserV1;
import com.crown.library.onspotlibrary.utils.ListItemType;
import com.crown.library.onspotlibrary.utils.emun.BusinessRequestStatus;
import com.crown.library.onspotlibrary.utils.emun.OSPreferenceKey;
import com.crown.library.onspotlibrary.views.LoadingBounceDialog;
import com.crown.onspotdelivery.R;
import com.crown.onspotdelivery.controller.AppController;
import com.crown.onspotdelivery.view.ListItemAdapter;

import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class SelectBusinessVH extends RecyclerView.ViewHolder {
    @BindView(R.id.iv_lsb_image)
    ImageView imageIV;
    @BindView(R.id.tv_lsb_name)
    TextView nameTV;
    @BindView(R.id.tv_lsb_category)
    TextView categoryTV;
    @BindView(R.id.tv_lsb_location)
    TextView locationTV;
    @BindView(R.id.tv_lsb_distance_btw)
    TextView distanceBtwTV;
    @BindView(R.id.btn_lsb_send_request)
    Button sendRequestBtn;

    private ListItemAdapter adapter;
    private BusinessV2 business;
    private Context context;
    private UserOSD user;

    public SelectBusinessVH(ListItemAdapter adapter, View view) {
        super(view);
        this.adapter = adapter;
        this.context = view.getContext();
        ButterKnife.bind(this, view);
        this.user = OSPreferences.getInstance(context.getApplicationContext()).getObject(OSPreferenceKey.USER, UserOSD.class);
    }

    public void bind(BusinessV2 business) {
        this.business = business;

        nameTV.setText(business.getDisplayName());
        categoryTV.setText(business.getBusinessType());
        locationTV.setText(business.getLocation().getAddressLine());
        Glide.with(context).load(business.getImageUrl()).apply(new RequestOptions().transform(new CenterCrop(), new RoundedCorners(32))).into(imageIV);

        BusinessRequestStatus status = getBusinessStatus(business);
        if (status == BusinessRequestStatus.PENDING) {
            sendRequestBtn.setText("Request Sent");
            sendRequestBtn.setEnabled(false);
        } else if (status == BusinessRequestStatus.ACCEPTED) {
            sendRequestBtn.setText("Accepted");
            sendRequestBtn.setEnabled(false);
        } else {
            sendRequestBtn.setText("Send Request");
            sendRequestBtn.setEnabled(true);
        }

    }

    @OnClick(R.id.btn_lsb_send_request)
    void onClickedSendRequest() {
        LoadingBounceDialog loadingBounceDialog = new LoadingBounceDialog((Activity) context);
        loadingBounceDialog.show();

        String url = context.getString(R.string.domain) + "/addBusinessRequest/";
        AppController.getInstance().addToRequestQueue(new StringRequest(Request.Method.POST, url, response -> {
            Toast.makeText(context, response, Toast.LENGTH_SHORT).show();
            loadingBounceDialog.dismiss();
        }, error -> {
            Toast.makeText(context, "Something went wrong!!", Toast.LENGTH_SHORT).show();
            loadingBounceDialog.dismiss();
        }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> param = new HashMap<>();
                param.put("osd", OSPreferences.getInstance(context.getApplicationContext()).getObject(OSPreferenceKey.USER, UserV1.class).toString());
                param.put("osb", new BusinessV0(business).toString());
                param.put("type", String.valueOf(ListItemType.DELIVERY_PARTNERSHIP_REQUEST));
                param.put("status", BusinessRequestStatus.PENDING.name());
                return param;
            }
        });
    }

    private BusinessRequestStatus getBusinessStatus(BusinessV2 business) {
        if (user.getBusinessOSD() == null || user.getBusinessOSD().size() == 0) return null;
        for (BusinessOSD b : user.getBusinessOSD()) {
            if (b.getBusinessRefId().equals(business.getBusinessRefId())) {
                return BusinessRequestStatus.valueOf(b.getStatus());
            }
        }
        return null;
    }
}
