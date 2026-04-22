package com.batu.transaction_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "transaction_primary_category")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TransactionPrimaryCategory {

    @Id
    @UuidGenerator
    @Column(name = "transaction_primary_category_id", nullable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private UUID transactionPrimaryCategoryId;

    @Column(name = "category_code", unique = true, nullable = false)
    private String categoryCode;

    @Column(name = "display_name", unique = true, nullable = false)
    private String displayName;

    @Column(name = "icon_url")
    private String iconUrl = "default";

    public TransactionPrimaryCategory(String categoryCode, String displayName, String iconUrl) {
        this.categoryCode = categoryCode;
        this.displayName = displayName;
        this.iconUrl = iconUrl;
    }
}
