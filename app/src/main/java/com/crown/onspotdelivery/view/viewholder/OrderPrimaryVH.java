package com.crown.onspotdelivery.view.viewholder;

import android.content.Context;
import android.content.Intent;
import android.text.Html;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.crown.library.onspotlibrary.controller.OSGlideLoader;
import com.crown.library.onspotlibrary.model.OSOrder;
import com.crown.library.onspotlibrary.model.cart.OSCartLite;
import com.crown.library.onspotlibrary.utils.BusinessItemUtils;
import com.crown.library.onspotlibrary.utils.CurrentLocation;
import com.crown.library.onspotlibrary.utils.OSLocationUtils;
import com.crown.library.onspotlibrary.utils.OSTimeUtils;
import com.crown.library.onspotlibrary.views.OrderItemView;
import com.crown.onspotdelivery.R;
import com.crown.onspotdelivery.page.OrderDetailsActivity;
import com.google.firebase.firestore.GeoPoint;

import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class OrderPrimaryVH extends RecyclerView.ViewHolder {
    @BindView(R.id.iv_lop_image)
    ImageView imageIV;
    @BindView(R.id.tv_lop_customer)
    TextView customerTV;
    @BindView(R.id.tv_lop_business)
    TextView businessTV;
    @BindView(R.id.tv_lop_order_time)
    TextView orderTimeTV;
    @BindView(R.id.tv_lop_drive_distance)
    TextView driveDistanceTV;
    @BindView(R.id.oiv_lop_order_item)
    OrderItemView ordersOIV;
    @BindView(R.id.tv_lop_item_count)
    TextView itemCountTV;
    @BindView(R.id.tv_lop_total_price)
    TextView totalPriceTV;

    private Context context;
    private OSOrder order;

    public OrderPrimaryVH(@NonNull View itemView) {
        super(itemView);
        this.context = itemView.getContext();
        ButterKnife.bind(this, itemView);
    }

    @OnClick(R.id.btn_lop_accept_delivery)
    void onClickedAccept() {
        new AlertDialog.Builder(context).setMessage(Html.fromHtml("Your <b>live location</b> will be shared to the customer as well as the business partner until you deliver the order to the customer.")).setPositiveButton("Accept", ((dialog, which) -> {
            /* TODO: Add delivery to the order
             *  notify user and business
             * */
        })).setNegativeButton("Cancel", null).show();
    }

    @OnClick(R.id.btn_lop_details)
    void onClickedDetails() {
        Intent intent = new Intent(context, OrderDetailsActivity.class);
        intent.putExtra(OrderDetailsActivity.ORDER, order.toString());
        context.startActivity(intent);
    }

    public void bind(OSOrder order) {
        this.order = order;

        // Get the user image url from the userId and display
        OSGlideLoader.loadUserProfileImage(context, order.getCustomer().getUserId(), imageIV);

        customerTV.setText(Html.fromHtml("<b>" + order.getCustomer().getDisplayName() + "</b>"));
        businessTV.setText(Html.fromHtml("from <b>" + order.getBusiness().getDisplayName() + "</b>"));
        if (order.getOrderedAt() != null)
            orderTimeTV.setText(OSTimeUtils.getTimeAgo(order.getOrderedAt().getSeconds()));

        // Get the device current location; display the total distance from device -> business -> customer
        CurrentLocation.getInstance(context).get(location -> {
            double distance = OSLocationUtils.getDistance(new GeoPoint(location.getLatitude(), location.getLongitude()), order.getBusiness().getLocation().getGeoPoint());
            distance += OSLocationUtils.getDistance(order.getBusiness().getLocation().getGeoPoint(), order.getCustomer().getLocation().getGeoPoint());

            driveDistanceTV.setText(String.format(Locale.ENGLISH, "%.2f KM drives", distance));
        }, null);

        int totalItems = 0;
        double totalPrice = 0;
        ordersOIV.removeAllViews();
        for (OSCartLite cart : order.getItems()) {
            int q = (int) (long) cart.getQuantity();
            double itemFinalPrice = BusinessItemUtils.getFinalPrice(cart.getPrice());
            totalItems += q;
            totalPrice += q * itemFinalPrice;
            ordersOIV.addChild(q, cart.getItemName(), itemFinalPrice * q);
        }

        itemCountTV.setText(String.format(Locale.ENGLISH, "%d items", totalItems));
        totalPriceTV.setText(String.format("%s %s", context.getString(R.string.inr), totalPrice));
    }
}
