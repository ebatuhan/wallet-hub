package com.batu.transaction_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "transaction_detailed_category")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TransactionDetailedCategory {

    @Id
    @UuidGenerator
    @Column(name = "transaction_detailed_category_id", nullable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private UUID transactionDetailedCategoryId;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "category_code", nullable = false)
    private String categoryCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "primary_category_id")
    private TransactionPrimaryCategory transactionPrimaryCategory;

    @Column(name = "description")
    private String description = "";

    public TransactionDetailedCategory(String displayName, String categoryCode,
            TransactionPrimaryCategory transactionPrimaryCategory, String description) {
        this.displayName = displayName;
        this.categoryCode = categoryCode;
        this.transactionPrimaryCategory = transactionPrimaryCategory;
        this.description = description;
    }
}
