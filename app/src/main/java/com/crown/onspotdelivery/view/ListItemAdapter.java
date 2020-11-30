package com.crown.onspotdelivery.view;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.crown.library.onspotlibrary.model.ListItem;
import com.crown.library.onspotlibrary.model.OSListHeader;
import com.crown.library.onspotlibrary.model.UnSupportedContent;
import com.crown.library.onspotlibrary.model.business.BusinessPartner;
import com.crown.library.onspotlibrary.model.business.BusinessV2;
import com.crown.library.onspotlibrary.model.notification.DeliveryPartnershipRequest;
import com.crown.library.onspotlibrary.model.notification.OSDeliveryPartnershipRequest;
import com.crown.library.onspotlibrary.model.order.OSOrder;
import com.crown.library.onspotlibrary.utils.ListItemType;
import com.crown.library.onspotlibrary.views.viewholder.ListHeaderVH;
import com.crown.library.onspotlibrary.views.viewholder.UnSupportedContentVH;
import com.crown.onspotdelivery.R;
import com.crown.onspotdelivery.view.viewholder.BusinessPartnerVH;
import com.crown.onspotdelivery.view.viewholder.DeliveryPartnershipRequestVH;
import com.crown.onspotdelivery.view.viewholder.MyDeliveryVH;
import com.crown.onspotdelivery.view.viewholder.OrderPrimaryVH;
import com.crown.onspotdelivery.view.viewholder.OrderWithDestinationPathVH;
import com.crown.onspotdelivery.view.viewholder.SelectBusinessVH;

import java.util.ArrayList;
import java.util.List;

public class ListItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements Filterable {
    private static final String TAG = ListItemAdapter.class.getName();
    private List<ListItem> mDataset;
    private List<ListItem> mArchiveDataset;

    public ListItemAdapter(Context context, List<ListItem> dataset) {
        this.mDataset = dataset;
        this.mArchiveDataset = dataset;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View rootView;
        switch (viewType) {
            case ListItemType.BUSINESS_PARTNER: {
                rootView = LayoutInflater.from(parent.getContext()).inflate(R.layout.li_business_partner, parent, false);
                return new BusinessPartnerVH(rootView);
            }
            case ListItemType.OS_MY_DELIVERY_ORDER_OSD: {
                rootView = LayoutInflater.from(parent.getContext()).inflate(R.layout.li_my_delivery, parent, false);
                return new MyDeliveryVH(rootView);
            }
            case ListItemType.LIST_HEADER: {
                rootView = LayoutInflater.from(parent.getContext()).inflate(R.layout.li_list_header, parent, false);
                return new ListHeaderVH(rootView);
            }
            case ListItemType.OS_ORDER_PATH_DETAILS: {
                rootView = LayoutInflater.from(parent.getContext()).inflate(R.layout.li_order_destination_path, parent, false);
                return new OrderWithDestinationPathVH(rootView);
            }
            case ListItemType.OS_ORDER: {
                rootView = LayoutInflater.from(parent.getContext()).inflate(R.layout.li_order_primary, parent, false);
                return new OrderPrimaryVH(rootView);
            }
            case ListItemType.NOTI_DELIVERY_PARTNERSHIP_REQUEST: {
                rootView = LayoutInflater.from(parent.getContext()).inflate(R.layout.noti_delivery_partnership_request, parent, false);
                return new DeliveryPartnershipRequestVH(rootView);
            }
            case ListItemType.BUSINESS_V2: {
                rootView = LayoutInflater.from(parent.getContext()).inflate(R.layout.li_select_business, parent, false);
                return new SelectBusinessVH(this, rootView);
            }
            case ListItemType.UNSUPPORTED_CONTENT:
            default: {
                rootView = LayoutInflater.from(parent.getContext()).inflate(R.layout.li_unsupported_content, parent, false);
                return new UnSupportedContentVH(rootView);
            }
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        switch (getItemViewType(position)) {
            case ListItemType.BUSINESS_PARTNER: {
                ((BusinessPartnerVH) holder).bind(((BusinessPartner) mDataset.get(position)));
                break;
            }
            case ListItemType.LIST_HEADER: {
                ((ListHeaderVH) holder).bind(((OSListHeader) mDataset.get(position)));
                break;
            }
            case ListItemType.OS_ORDER_PATH_DETAILS: {
                ((OrderWithDestinationPathVH) holder).bind(((OSOrder) mDataset.get(position)));
                break;
            }
            case ListItemType.OS_ORDER: {
                ((OrderPrimaryVH) holder).bind(((OSOrder) mDataset.get(position)));
                break;
            }
            case ListItemType.NOTI_DELIVERY_PARTNERSHIP_REQUEST: {
                ((DeliveryPartnershipRequestVH) holder).bind(((OSDeliveryPartnershipRequest) mDataset.get(position)));
                break;
            }
            case ListItemType.BUSINESS_V2: {
                ((SelectBusinessVH) holder).bind((BusinessV2) mDataset.get(position));
                break;
            }
            case ListItemType.OS_MY_DELIVERY_ORDER_OSD: {
                ((MyDeliveryVH) holder).bind((OSOrder) mDataset.get(position));
                break;
            }
            case ListItemType.UNSUPPORTED_CONTENT: {
                ((UnSupportedContentVH) holder).bind((UnSupportedContent) mDataset.get(position));
                break;
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        return mDataset.get(position).getItemType();
    }

    @Override
    public int getItemCount() {
        return mDataset.size();
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                String input = constraint.toString();
                if (TextUtils.isEmpty(input)) {
                    mDataset = mArchiveDataset;
                } else {
                    List<ListItem> filteredList = new ArrayList<>();
                    for (ListItem item : mArchiveDataset) {
                        if (item.getItemType() == ListItemType.BUSINESS_V2) {
                            if (((BusinessV2) item).getDisplayName().toLowerCase().contains(input) ||
                                    ((BusinessV2) item).getBusinessType().toLowerCase().contains(input)) {
                                filteredList.add(item);
                            }
                        }
                    }
                    mDataset = filteredList;
                }
                FilterResults filterResults = new FilterResults();
                filterResults.values = mDataset;
                return filterResults;
            }

            @Override
            @SuppressWarnings("unchecked")
            protected void publishResults(CharSequence constraint, FilterResults results) {
                mDataset = (List<ListItem>) results.values;
                notifyDataSetChanged();
            }
        };
    }
}
