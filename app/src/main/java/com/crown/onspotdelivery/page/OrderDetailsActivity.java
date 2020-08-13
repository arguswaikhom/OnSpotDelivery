package com.crown.onspotdelivery.page;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;

import com.crown.library.onspotlibrary.model.OSLocation;
import com.crown.library.onspotlibrary.model.OSOrder;
import com.crown.library.onspotlibrary.model.cart.OSCartLite;
import com.crown.library.onspotlibrary.utils.BusinessItemUtils;
import com.crown.library.onspotlibrary.utils.CurrentLocation;
import com.crown.library.onspotlibrary.utils.OSContactReacher;
import com.crown.library.onspotlibrary.utils.OSLocationUtils;
import com.crown.library.onspotlibrary.utils.OSMapUtils;
import com.crown.library.onspotlibrary.utils.OSTimeUtils;
import com.crown.library.onspotlibrary.utils.callback.OnStringResponse;
import com.crown.library.onspotlibrary.views.OrderItemView;
import com.crown.onspotdelivery.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.gson.Gson;

import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class OrderDetailsActivity extends AppCompatActivity implements PopupMenu.OnMenuItemClickListener {

    public static final String ORDER = "order";
    private OSOrder order;

    @BindView(R.id.tv_lodd_order_time)
    TextView orderTimeTV;
    @BindView(R.id.ibtn_lodd_more)
    ImageButton moreIBtn;
    @BindView(R.id.oiv_lodd_order_item)
    OrderItemView itemsOIV;
    @BindView(R.id.tv_lodd_total_item)
    TextView totalItemTV;
    @BindView(R.id.tv_lodd_total_price)
    TextView totalAmountTV;
    @BindView(R.id.tv_idp_osd_to_osb_distance)
    TextView osdToOsbDistanceTV;
    @BindView(R.id.tv_idp_osb_to_os_distance)
    TextView osbToOsDistanceTV;
    @BindView(R.id.tv_idp_osb_name)
    TextView businessNameTV;
    @BindView(R.id.tv_idp_osb_address)
    TextView businessAddressTV;
    @BindView(R.id.tv_idp_os_name)
    TextView customerNameTV;
    @BindView(R.id.tv_idp_os_address)
    TextView customerAddressTV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_details);
        ButterKnife.bind(this);

        String json = getIntent().getStringExtra(ORDER);
        if (json != null) order = new Gson().fromJson(json, OSOrder.class);
        setUpUI();
    }

    @OnClick(R.id.ibtn_lodd_more)
    void onClickedMore(View view) {
        PopupMenu menu = new PopupMenu(this, view);
        menu.inflate(R.menu.more_order_details);
        menu.setOnMenuItemClickListener(this);
        menu.show();
    }

    @OnClick(R.id.iv_idp_osb_call)
    void onClickedCallOsb() {
        FirebaseFirestore.getInstance().collection(getString(R.string.ref_business)).document(order.getBusiness().getBusinessRefId()).get().addOnSuccessListener(documentSnapshot -> {
            if (this.isFinishing()) return;
            String phoneNo = (String) documentSnapshot.get(getString(R.string.field_mobile_number));
            if (phoneNo != null) {
                Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phoneNo));
                this.startActivity(intent);
            } else {
                Toast.makeText(this, "Contact not found", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to get contact", Toast.LENGTH_SHORT).show();
        });
    }

    @OnClick(R.id.iv_idp_os_call)
    void onClickedCallOs() {
        OSContactReacher.getUserMobileNumber(this, order.getCustomer().getUserId(), (OnStringResponse) value -> {
            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + value));
            this.startActivity(intent);
        }, (e, msg) -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show());
    }

    @OnClick(R.id.btn_lodd_navigation)
    void onClickedNavigate() {
        GeoPoint osb = order.getBusiness().getLocation().getGeoPoint();
        GeoPoint os = order.getCustomer().getLocation().getGeoPoint();
        OSMapUtils.showDirection(this, osb, os);
    }


    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.nav_mod_open_destination:
                OSLocation cl = order.getCustomer().getLocation();
                OSMapUtils.showLocation(this, cl.getGeoPoint(), cl.getAddressLine());
                break;
        }
        return false;
    }

    private void setUpUI() {
        if (order == null) return;

        orderTimeTV.setText(String.format("%s - %s", OSTimeUtils.getDay(order.getOrderedAt().getSeconds()), OSTimeUtils.getTime(order.getOrderedAt().getSeconds())));

        int totalItems = 0;
        double totalPrice = 0;
        for (OSCartLite cart : order.getItems()) {
            int q = (int) (long) cart.getQuantity();
            double itemFinalPrice = BusinessItemUtils.getFinalPrice(cart.getPrice());
            totalItems += q;
            totalPrice += q * itemFinalPrice;
            itemsOIV.addChild(q, cart.getItemName(), itemFinalPrice * q);
        }

        totalItemTV.setText(String.format(Locale.ENGLISH, "%d items", totalItems));
        totalAmountTV.setText(String.format("%s %s", getString(R.string.inr), totalPrice));
        businessNameTV.setText(order.getBusiness().getDisplayName());
        businessAddressTV.setText(order.getBusiness().getLocation().getAddressLine());
        customerNameTV.setText(order.getCustomer().getDisplayName());
        customerAddressTV.setText(order.getCustomer().getLocation().getAddressLine());

        CurrentLocation.getInstance(this).get(location -> {
            double osdToOsbDistance = OSLocationUtils.getDistance(new GeoPoint(location.getLatitude(), location.getLongitude()), order.getBusiness().getLocation().getGeoPoint());
            double osbToOsDistance = OSLocationUtils.getDistance(order.getBusiness().getLocation().getGeoPoint(), order.getCustomer().getLocation().getGeoPoint());

            osdToOsbDistanceTV.setText(String.format(Locale.ENGLISH, "%.2f KM", osdToOsbDistance));
            osbToOsDistanceTV.setText(String.format(Locale.ENGLISH, "%.2f KM", osbToOsDistance));
        }, null);
    }
}