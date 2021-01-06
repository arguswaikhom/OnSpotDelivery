package com.crown.onspotdelivery.page;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;

import com.crown.library.onspotlibrary.controller.OSGlideLoader;
import com.crown.library.onspotlibrary.controller.OSPreferences;
import com.crown.library.onspotlibrary.model.OSLocation;
import com.crown.library.onspotlibrary.model.cart.OSCartLite;
import com.crown.library.onspotlibrary.model.order.OSOrder;
import com.crown.library.onspotlibrary.model.user.UserOrder;
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
import com.crown.onspotdelivery.databinding.ActivityOrderDetailsBinding;
import com.crown.onspotdelivery.databinding.IvDestinationPathBinding;
import com.crown.onspotdelivery.databinding.LiOrderDestinationPathBinding;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class OrderDetailsActivity extends AppCompatActivity implements PopupMenu.OnMenuItemClickListener {
    public static final String ORDER = "order";
    private OSOrder order;
    private LoadingBounceDialog loadingDialog;
    private IvDestinationPathBinding pathBinding;
    private LiOrderDestinationPathBinding orderBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityOrderDetailsBinding binding = ActivityOrderDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        orderBinding = binding.orderDestinationPathInclude;
        pathBinding = orderBinding.pathInclude;
        loadingDialog = new LoadingBounceDialog(this);

        String json = getIntent().getStringExtra(ORDER);
        if (json != null) order = new Gson().fromJson(json, OSOrder.class);
        initUi();
        setUpUI();
    }

    private void initUi() {
        orderBinding.moreIbtn.setOnClickListener(this::onClickedMore);
        orderBinding.pathInclude.osbCallIv.setOnClickListener(this::onClickedOsbCall);
        orderBinding.pathInclude.osCallIv.setOnClickListener(this::onClickedOsCall);
        orderBinding.secondaryBtn.setOnClickListener(this::onClickedStartNav);
        orderBinding.primaryBtn.setOnClickListener(this::onClickedAccept);
    }

    private void onClickedMore(View view) {
        PopupMenu menu = new PopupMenu(this, view);
        menu.inflate(R.menu.more_order_details);
        menu.getMenu().findItem(R.id.action_mod_cancel_delivery).setVisible(false);
        menu.getMenu().findItem(R.id.action_mod_order_delivered).setVisible(false);
        menu.setOnMenuItemClickListener(this);
        menu.show();
    }

    void onClickedAccept(View view) {
        new AlertDialog.Builder(this).setTitle("Accept delivery")
                .setMessage("Press accept to confirm").setPositiveButton("Accept", ((dialog, which) -> {
            if (order != null) {
                UserOrder delivery = OSPreferences.getInstance(getApplicationContext()).getObject(OSPreferenceKey.USER, UserOrder.class);
                Map<String, Object> param = new HashMap<>();
                param.put(OSString.fieldDelivery, delivery);
                loadingDialog.show();
                FirebaseFirestore.getInstance().collection(OSString.refOrder).document(order.getOrderId()).update(param).addOnSuccessListener(result -> {
                    loadingDialog.dismiss();
                    OSMessage.showSToast(this, "Update successful");
                    finish();
                }).addOnFailureListener(e -> {
                    loadingDialog.dismiss();
                    e.printStackTrace();
                    OSMessage.showSToast(this, "Failed to update!!");
                });
            }
        })).setNegativeButton("Cancel", null).show();
    }

    private void onClickedOsbCall(View view) {
        FirebaseFirestore.getInstance().collection(OSString.refBusiness).document(order.getBusiness().getBusinessRefId()).get().addOnSuccessListener(documentSnapshot -> {
            if (this.isFinishing()) return;
            String phoneNo = (String) documentSnapshot.get(OSString.fieldMobileNumber);
            if (phoneNo != null) {
                OSCommonIntents.onIntentCallRequest(this, phoneNo);
            } else {
                OSMessage.showSToast(this, "Contact not found");
            }
        }).addOnFailureListener(e -> {
            e.printStackTrace();
            OSMessage.showSToast(this, "Failed to get contact");
        });
    }

    private void onClickedOsCall(View view) {
        OSContactReacher.getUserMobileNumber(this, order.getCustomer().getUserId(),
                (OnStringResponse) value -> OSCommonIntents.onIntentCallRequest(this, value)
                , (e, msg) -> OSMessage.showSToast(this, msg));
    }

    private void onClickedStartNav(View view) {
        GeoPoint osb = order.getBusiness().getLocation().getGeoPoint();
        GeoPoint os = order.getCustomer().getLocation().getGeoPoint();
        OSMapUtils.showDirection(this, osb, os);
    }


    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.nav_mod_customer_location:
                OSLocation cl = order.getCustomer().getLocation();
                OSMapUtils.showLocation(this, cl.getGeoPoint(), cl.getAddressLine());
                break;
        }
        return false;
    }

    // todo: consider shipping charge
    private void setUpUI() {
        if (order == null) return;

        orderBinding.secondaryBtn.setText(getString(R.string.action_btn_show_nav));
        orderBinding.primaryBtn.setText(getString(R.string.action_btn_accept_delivery));

        int color = order.getStatus().getColor(getApplicationContext());
        if (color != 0) {
            orderBinding.statusTv.setBackgroundColor(order.getStatus().getColor(getApplicationContext()));
            orderBinding.secondaryBtn.setTextColor(color);
            orderBinding.primaryBtn.setBackgroundColor(color);
            orderBinding.primaryBtn.setTextColor(getColor(android.R.color.white));
        }

        orderBinding.statusTv.setText(order.getStatus().getStatus());
        orderBinding.customerNameTv.setText(order.getCustomer().getDisplayName());
        OSGlideLoader.loadUserProfileImage(getApplicationContext(), order.getCustomer().getUserId(), orderBinding.imageIv);
        orderBinding.orderTimeTv.setText(String.format("%s - %s", OSTimeUtils.getTime(order.getOrderedAt().getSeconds()), OSTimeUtils.getDay(order.getOrderedAt().getSeconds())));

        int totalPrice = 0;
        for (OSCartLite cart : order.getItems()) {
            int q = (int) (long) cart.getQuantity();
            double itemFinalPrice = BusinessItemUtils.getFinalPrice(cart.getPrice());
            totalPrice += q * itemFinalPrice;
            orderBinding.orderItemOiv.addChild(q, cart.getItemName(), (int) itemFinalPrice * q);
        }

        if (order.getHodAvailable()) {
            orderBinding.orderItemOiv.addChild("Delivery charge", (int) (long) order.getShippingCharge());
        }

        orderBinding.totalItemTv.setText(String.format(Locale.ENGLISH, "%d items", order.getFinalPrice()));
        orderBinding.totalPriceTv.setText(String.format("%s %s", OSString.inrSymbol, totalPrice));
        pathBinding.osbNameTv.setText(order.getBusiness().getDisplayName());
        pathBinding.osbAddressTv.setText(order.getBusiness().getLocation().getAddressLine());
        pathBinding.osNameTv.setText(order.getCustomer().getDisplayName());
        pathBinding.osAddressTv.setText(order.getCustomer().getLocation().getAddressLine());

        CurrentLocation.getInstance(this).get(location -> {
            double osdToOsbDistance = OSLocationUtils.getDistance(new GeoPoint(location.getLatitude(), location.getLongitude()), order.getBusiness().getLocation().getGeoPoint());
            double osbToOsDistance = OSLocationUtils.getDistance(order.getBusiness().getLocation().getGeoPoint(), order.getCustomer().getLocation().getGeoPoint());

            pathBinding.osdToOsbDistanceTv.setText(String.format(Locale.ENGLISH, "%.2f KM", osdToOsbDistance));
            pathBinding.osbToOsDistanceTv.setText(String.format(Locale.ENGLISH, "%.2f KM", osbToOsDistance));
        }, null, null);
    }
}