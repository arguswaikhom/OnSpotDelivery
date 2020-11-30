package com.crown.onspotdelivery.view.viewholder;

import android.content.Context;
import android.text.Html;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.crown.library.onspotlibrary.model.order.OSOrder;
import com.crown.library.onspotlibrary.utils.OSTimeUtils;
import com.crown.onspotdelivery.R;
import com.crown.onspotdelivery.databinding.LiMyDeliveryBinding;

public class MyDeliveryVH extends RecyclerView.ViewHolder {

    private Context context;
    private LiMyDeliveryBinding binding;

    public MyDeliveryVH(@NonNull View itemView) {
        super(itemView);
        context = itemView.getContext();
        binding = LiMyDeliveryBinding.bind(itemView);
    }

    public void bind(OSOrder order) {
        binding.customerNameTv.setText(order.getCustomer().getDisplayName());
        binding.businessNameTv.setText(Html.fromHtml(context.getString(R.string.text_from_fill, order.getBusiness().getDisplayName())));
        binding.timeAgoTv.setText(OSTimeUtils.getTimeAgo(order.getOrderedAt().getSeconds()));
    }
}
