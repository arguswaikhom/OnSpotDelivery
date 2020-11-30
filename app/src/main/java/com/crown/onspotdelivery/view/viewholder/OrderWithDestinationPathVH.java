package com.crown.onspotdelivery.view.viewholder;

import android.app.Activity;
import android.text.Html;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.crown.library.onspotlibrary.controller.OSGlideLoader;
import com.crown.library.onspotlibrary.controller.OSPreferences;
import com.crown.library.onspotlibrary.controller.OSVolley;
import com.crown.library.onspotlibrary.model.OSLocation;
import com.crown.library.onspotlibrary.model.cart.OSCartLite;
import com.crown.library.onspotlibrary.model.httpresponse.OSHttpResponseV0;
import com.crown.library.onspotlibrary.model.order.OSOrder;
import com.crown.library.onspotlibrary.model.user.UserOSD;
import com.crown.library.onspotlibrary.utils.BusinessItemUtils;
import com.crown.library.onspotlibrary.utils.CurrentLocation;
import com.crown.library.onspotlibrary.utils.OSCommonIntents;
import com.crown.library.onspotlibrary.utils.OSContactReacher;
import com.crown.library.onspotlibrary.utils.OSLocationUtils;
import com.crown.library.onspotlibrary.utils.OSMapUtils;
import com.crown.library.onspotlibrary.utils.OSMessage;
import com.crown.library.onspotlibrary.utils.OSString;
import com.crown.library.onspotlibrary.utils.OSTimeUtils;
import com.crown.library.onspotlibrary.utils.callback.OnStringResponse;
import com.crown.library.onspotlibrary.utils.emun.OSPreferenceKey;
import com.crown.library.onspotlibrary.views.LoadingBounceDialog;
import com.crown.onspotdelivery.R;
import com.crown.onspotdelivery.databinding.IvDestinationPathBinding;
import com.crown.onspotdelivery.databinding.LiOrderDestinationPathBinding;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class OrderWithDestinationPathVH extends RecyclerView.ViewHolder implements PopupMenu.OnMenuItemClickListener {
    private final UserOSD user;
    private OSOrder order;
    private final Activity activity;
    private final LoadingBounceDialog loadingDialog;
    private final IvDestinationPathBinding pathBinding;
    private final LiOrderDestinationPathBinding binding;

    public OrderWithDestinationPathVH(@NonNull View itemView) {
        super(itemView);
        activity = (Activity) itemView.getContext();
        binding = LiOrderDestinationPathBinding.bind(itemView);
        pathBinding = binding.pathInclude;
        loadingDialog = new LoadingBounceDialog(activity);
        binding.moreIbtn.setOnClickListener(this::onClickedMore);
        binding.pathInclude.osbCallIv.setOnClickListener(this::onClickedOsbCall);
        binding.pathInclude.osCallIv.setOnClickListener(this::onClickedOsCall);
        binding.primaryBtn.setOnClickListener(this::onClickedStartNav);
        binding.secondaryBtn.setOnClickListener(this::onClickedOrderDelivered);
        user = OSPreferences.getInstance(activity.getApplicationContext()).getObject(OSPreferenceKey.USER, UserOSD.class);
    }

    private void onClickedOrderDelivered(View view) {
        if (user == null || order == null) return;

        // Show a dialog to confirm the order delivered to prevent from the misclicks
        new AlertDialog.Builder(activity).setTitle("Order Delivered")
                .setMessage(Html.fromHtml("Confirm order deliver to <b>" + order.getCustomer().getDisplayName() + "</b>"))
                .setNegativeButton(activity.getString(R.string.action_btn_no), ((dialog, which) -> dialog.dismiss()))
                .setPositiveButton(activity.getString(R.string.action_btn_yes), ((dialog, which) -> {
                    // Hold the user interaction by displaying the loading dialog
                    loadingDialog.show();

                    // Send and HTTP request to confirm the order delivery
                    OSVolley.getInstance(activity).addToRequestQueue(new StringRequest(Request.Method.POST, OSString.apiConfirmOrderDeliver, response -> {
                        loadingDialog.dismiss();
                        OSHttpResponseV0 res = OSHttpResponseV0.fromJson(response);
                        if (res.getStatus() == 200)
                            OSMessage.showSToast(activity, "Updated successfully");
                        else if (res.getStatus() == 403)
                            OSMessage.showSToast(activity, "Access denied!!");
                    }, error -> {
                        loadingDialog.dismiss();
                        OSMessage.showSToast(activity, activity.getString(R.string.msg_something_went_wrong));
                    }) {
                        @Override
                        protected Map<String, String> getParams() {
                            Map<String, String> param = new HashMap<>();
                            param.put(OSString.fieldUserId, user.getUserId());
                            param.put(OSString.fieldOrderId, order.getOrderId());
                            return param;
                        }
                    });
                })).show();
    }

    private void onClickedMore(View view) {
        PopupMenu menu = new PopupMenu(activity, view);
        menu.inflate(R.menu.more_order_details);
        menu.setOnMenuItemClickListener(this);
        menu.show();
    }

    private void onClickedOsbCall(View view) {
        FirebaseFirestore.getInstance().collection(OSString.refBusiness).document(order.getBusiness().getBusinessRefId()).get().addOnSuccessListener(documentSnapshot -> {
            if (activity.isFinishing()) return;
            String phoneNo = (String) documentSnapshot.get(OSString.fieldMobileNumber);
            if (phoneNo != null) {
                OSCommonIntents.onIntentCallRequest(activity, phoneNo);
            } else {
                OSMessage.showSToast(activity, "Contact not found");
            }
        }).addOnFailureListener(e -> {
            e.printStackTrace();
            OSMessage.showSToast(activity, "Failed to get contact");
        });
    }

    private void onClickedOsCall(View view) {
        OSContactReacher.getUserMobileNumber(activity, order.getCustomer().getUserId(),
                (OnStringResponse) value -> OSCommonIntents.onIntentCallRequest(activity, value)
                , (e, msg) -> OSMessage.showSToast(activity, msg));
    }

    private void onClickedStartNav(View view) {
        GeoPoint osb = order.getBusiness().getLocation().getGeoPoint();
        GeoPoint os = order.getCustomer().getLocation().getGeoPoint();
        OSMapUtils.showDirection(activity, osb, os);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        final int id = item.getItemId();
        if (id == R.id.nav_mod_customer_location) {
            OSLocation cl = order.getCustomer().getLocation();
            OSMapUtils.showLocation(activity, cl.getGeoPoint(), cl.getAddressLine());
            return true;
        } else if (id == R.id.action_mod_order_delivered) {
            binding.secondaryBtn.performClick();
            return true;
        } else if (id == R.id.action_mod_cancel_delivery) {
            cancelOrderDelivery();
            return true;
        }
        return false;
    }

    private void cancelOrderDelivery() {
        if (user == null || order == null) return;

        // Let the user confirm whether they want to confirm the cancellation process or not
        new AlertDialog.Builder(activity).setTitle(activity.getString(R.string.text_title_alert_cancel_delivery))
                .setMessage(activity.getString(R.string.text_body_alert_cancel_confirm))
                .setNegativeButton(activity.getString(R.string.action_btn_no), ((dialog, which) -> dialog.dismiss()))
                .setPositiveButton(activity.getString(R.string.action_btn_yes), ((dialog, which) -> {
                    // Hold the user interaction by displaying the loading dialog
                    loadingDialog.show();

                    // Send and HTTP request to cancel the order delivery
                    OSVolley.getInstance(activity).addToRequestQueue(new StringRequest(Request.Method.POST, OSString.apiCancelOrderDeliver, response -> {
                        loadingDialog.dismiss();
                        OSHttpResponseV0 res = OSHttpResponseV0.fromJson(response);
                        if (res.getStatus() == 200)
                            OSMessage.showSToast(activity, "Updated successfully");
                        else if (res.getStatus() == 403)
                            OSMessage.showSToast(activity, "Access denied!!");
                    }, error -> {
                        loadingDialog.dismiss();
                        OSMessage.showSToast(activity, activity.getString(R.string.msg_something_went_wrong));
                    }) {
                        @Override
                        protected Map<String, String> getParams() {
                            Map<String, String> param = new HashMap<>();
                            param.put(OSString.fieldUserId, user.getUserId());
                            param.put(OSString.fieldOrderId, order.getOrderId());
                            return param;
                        }
                    });
                })).show();
    }

    // todo: consider shipping charge
    public void bind(OSOrder order) {
        this.order = order;

        int color = order.getStatus().getColor(activity);
        if (color != 0) {
            binding.statusTv.setBackgroundColor(order.getStatus().getColor(activity));
            binding.secondaryBtn.setTextColor(color);
            binding.primaryBtn.setBackgroundColor(color);
            binding.primaryBtn.setTextColor(activity.getColor(android.R.color.white));
        }

        binding.statusTv.setText(order.getStatus().getStatus());

        // Re-enable the button if it was disable before; this button is disable on update order delivered
        if (!binding.secondaryBtn.isEnabled()) binding.secondaryBtn.setEnabled(true);

        binding.customerNameTv.setText(order.getCustomer().getDisplayName());
        OSGlideLoader.loadUserProfileImage(activity, order.getCustomer().getUserId(), binding.imageIv);
        binding.orderTimeTv.setText(String.format("%s - %s", OSTimeUtils.getTime(order.getOrderedAt().getSeconds()), OSTimeUtils.getDay(order.getOrderedAt().getSeconds())));

        int totalItems = 0;
        int totalPrice = 0;
        binding.orderItemOiv.clear();
        for (OSCartLite cart : order.getItems()) {
            int q = (int) (long) cart.getQuantity();
            double itemFinalPrice = BusinessItemUtils.getFinalPrice(cart.getPrice());
            totalItems += q;
            totalPrice += q * itemFinalPrice;
            binding.orderItemOiv.addChild(q, cart.getItemName(), (int) itemFinalPrice * q);
        }

        binding.totalItemTv.setText(String.format(Locale.ENGLISH, "%d items", totalItems));
        binding.totalPriceTv.setText(String.format("%s %s", OSString.inrSymbol, totalPrice));
        pathBinding.osbNameTv.setText(order.getBusiness().getDisplayName());
        pathBinding.osbAddressTv.setText(order.getBusiness().getLocation().getAddressLine());
        pathBinding.osNameTv.setText(order.getCustomer().getDisplayName());
        pathBinding.osAddressTv.setText(order.getCustomer().getLocation().getAddressLine());
        if (TextUtils.isEmpty(order.getCustomer().getLocation().getHowToReach())) {
            pathBinding.osHowToReachTv.setVisibility(View.GONE);
        } else {
            pathBinding.osHowToReachTv.setVisibility(View.VISIBLE);
            String howToReach = "<b>" + activity.getString(R.string.header_how_to_reach) + ":</b>\t" + order.getCustomer().getLocation().getHowToReach();
            pathBinding.osHowToReachTv.setText(Html.fromHtml(howToReach));
        }

        CurrentLocation.getInstance(activity).get(location -> {
            double osdToOsbDistance = OSLocationUtils.getDistance(new GeoPoint(location.getLatitude(), location.getLongitude()), order.getBusiness().getLocation().getGeoPoint());
            double osbToOsDistance = OSLocationUtils.getDistance(order.getBusiness().getLocation().getGeoPoint(), order.getCustomer().getLocation().getGeoPoint());

            pathBinding.osdToOsbDistanceTv.setText(String.format(Locale.ENGLISH, "%.2f KM", osdToOsbDistance));
            pathBinding.osbToOsDistanceTv.setText(String.format(Locale.ENGLISH, "%.2f KM", osbToOsDistance));
        }, null, null);
    }
}
