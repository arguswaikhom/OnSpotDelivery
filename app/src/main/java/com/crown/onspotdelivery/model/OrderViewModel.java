package com.crown.onspotdelivery.model;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.firestore.QuerySnapshot;

public class OrderViewModel extends ViewModel {
    private final MutableLiveData<QuerySnapshot> myDelivery = new MutableLiveData<>();
    private final MutableLiveData<QuerySnapshot> deliverableOrder = new MutableLiveData<>();

    public void setMyDelivery(QuerySnapshot myDelivery) {
        this.myDelivery.setValue(myDelivery);
    }

    public void setDeliverableOrder(QuerySnapshot deliverableOrder) {
        this.deliverableOrder.setValue(deliverableOrder);
    }

    public MutableLiveData<QuerySnapshot> getMyDelivery() {
        return myDelivery;
    }

    public MutableLiveData<QuerySnapshot> getDeliverableOrder() {
        return deliverableOrder;
    }
}