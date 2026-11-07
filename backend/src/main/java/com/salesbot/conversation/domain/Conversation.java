package com.salesbot.conversation.domain;

import com.salesbot.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** A chat between one business number and one customer. */
@Entity
@Table(name = "conversations")
public class Conversation extends BaseEntity {

    @Column(name = "phone_number_id", nullable = false)
    private String phoneNumberId;

    @Column(name = "customer_wa_id", nullable = false)
    private String customerWaId;

    @Column(name = "customer_name")
    private String customerName;

    protected Conversation() {
    }

    public Conversation(String phoneNumberId, String customerWaId, String customerName) {
        this.phoneNumberId = phoneNumberId;
        this.customerWaId = customerWaId;
        this.customerName = customerName;
    }

    /** Keeps the latest profile name the customer uses. */
    public void updateCustomerName(String name) {
        if (name != null && !name.isBlank()) {
            this.customerName = name;
        }
    }

    public String getPhoneNumberId() {
        return phoneNumberId;
    }

    public String getCustomerWaId() {
        return customerWaId;
    }

    public String getCustomerName() {
        return customerName;
    }
}
