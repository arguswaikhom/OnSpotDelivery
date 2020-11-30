package com.crown.onspotdelivery.view.viewholder;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.text.Html;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.crown.library.onspotlibrary.controller.OSGlideLoader;
import com.crown.library.onspotlibrary.controller.OSPreferences;
import com.crown.library.onspotlibrary.controller.OSVolley;
import com.crown.library.onspotlibrary.model.cart.OSCartLite;
import com.crown.library.onspotlibrary.model.httpresponse.OSHttpResponseV0;
import com.crown.library.onspotlibrary.model.order.OSOrder;
import com.crown.library.onspotlibrary.model.user.UserV0;
import com.crown.library.onspotlibrary.utils.BusinessItemUtils;
import com.crown.library.onspotlibrary.utils.CurrentLocation;
import com.crown.library.onspotlibrary.utils.OSLocationUtils;
import com.crown.library.onspotlibrary.utils.OSMessage;
import com.crown.library.onspotlibrary.utils.OSString;
import com.crown.library.onspotlibrary.utils.OSTimeUtils;
import com.crown.library.onspotlibrary.utils.emun.OSPreferenceKey;
import com.crown.library.onspotlibrary.views.LoadingBounceDialog;
import com.crown.onspotdelivery.R;
import com.crown.onspotdelivery.databinding.LiOrderPrimaryBinding;
import com.crown.onspotdelivery.page.OrderDetailsActivity;
import com.google.firebase.firestore.GeoPoint;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class OrderPrimaryVH extends RecyclerView.ViewHolder {
    private final Context context;
    private OSOrder order;
    private final LiOrderPrimaryBinding binding;
    private final LoadingBounceDialog loadingDialog;

    public OrderPrimaryVH(@NonNull View itemView) {
        super(itemView);
        this.context = itemView.getContext();
        loadingDialog = new LoadingBounceDialog((Activity) context);
        this.binding = LiOrderPrimaryBinding.bind(itemView);
        binding.detailsBtn.setOnClickListener(this::onClickedDetails);
        binding.acceptDeliveryBtn.setOnClickListener(this::onClickedAccept);
    }

    void onClickedAccept(View view) {
        if (order == null) return;
        loadingDialog.show();
        OSVolley.getInstance(context).addToRequestQueue(new StringRequest(Request.Method.POST, OSString.apiAcceptOrderDeliver, response -> {
            loadingDialog.dismiss();
            OSHttpResponseV0 res = OSHttpResponseV0.fromJson(response);
            if (res.getStatus() == 200) OSMessage.showSToast(context, "Updated successfully");
            else if (res.getStatus() == 403) OSMessage.showSToast(context, "Access denied!!");
        }, error -> {
            loadingDialog.dismiss();
            OSMessage.showSToast(context, context.getString(R.string.msg_something_went_wrong));
        }) {
            @Override
            protected Map<String, String> getParams() {
                UserV0 user = OSPreferences.getInstance(context).getObject(OSPreferenceKey.USER, UserV0.class);
                Map<String, String> param = new HashMap<>();
                param.put(OSString.fieldUserId, user.getUserId());
                param.put(OSString.fieldOrderId, order.getOrderId());
                return param;
            }
        });
    }

    void onClickedDetails(View view) {
        Intent intent = new Intent(context, OrderDetailsActivity.class);
        intent.putExtra(OrderDetailsActivity.ORDER, order.toString());
        context.startActivity(intent);
    }

    // todo: consider shipping charge
    public void bind(OSOrder order) {
        this.order = order;

        int color = order.getStatus().getColor(context);
        if (color != 0) {
            binding.statusTv.setBackgroundColor(order.getStatus().getColor(context));
            binding.detailsBtn.setTextColor(color);
            binding.acceptDeliveryBtn.setBackgroundColor(color);
            binding.acceptDeliveryBtn.setTextColor(context.getColor(android.R.color.white));
        }

        binding.statusTv.setText(order.getStatus().getStatus());

        // Get the user image url from the userId and display
        OSGlideLoader.loadUserProfileImage(context, order.getCustomer().getUserId(), binding.imageIv);

        binding.customerNameTv.setText(order.getCustomer().getDisplayName());
        binding.businessTv.setText(Html.fromHtml("from <b>" + order.getBusiness().getDisplayName() + "</b>"));
        if (order.getOrderedAt() != null) {
            binding.orderTimeTv.setText(OSTimeUtils.getTimeAgo(order.getOrderedAt().getSeconds()));
        }

        // Get the device current location; display the total distance from device -> business -> customer
        CurrentLocation.getInstance(context).get(location -> {
            double distance = OSLocationUtils.getDistance(new GeoPoint(location.getLatitude(), location.getLongitude()), order.getBusiness().getLocation().getGeoPoint());
            distance += OSLocationUtils.getDistance(order.getBusiness().getLocation().getGeoPoint(), order.getCustomer().getLocation().getGeoPoint());

            binding.driveDistanceTv.setText(String.format(Locale.ENGLISH, "%.2f KM drives", distance));
        }, null);

        int totalItems = 0;
        int totalPrice = 0;
        binding.orderItemOiv.removeAllViews();
        for (OSCartLite cart : order.getItems()) {
            int q = (int) (long) cart.getQuantity();
            double itemFinalPrice = BusinessItemUtils.getFinalPrice(cart.getPrice());
            totalItems += q;
            totalPrice += q * itemFinalPrice;
            binding.orderItemOiv.addChild(q, cart.getItemName(), (int) itemFinalPrice * q);
        }

        binding.itemCountTv.setText(String.format(Locale.ENGLISH, "%d items", totalItems));
        binding.totalPriceTv.setText(String.format("%s %s", context.getString(R.string.inr), totalPrice));
    }
}
