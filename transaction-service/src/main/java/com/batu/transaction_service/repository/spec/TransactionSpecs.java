package com.batu.transaction_service.repository.spec;

import com.batu.transaction_service.entity.Transaction;
import com.batu.transaction_service.entity.TransactionDetailedCategory;
import com.batu.transaction_service.entity.TransactionPrimaryCategory;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Fetch;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TransactionSpecs {

 public static Specification<Transaction> withDynamicFilters(UUID userId,UUID accountId, String primaryCategoryCode) {
    return (root, query, cb) -> {
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("userId"), userId));
        predicates.add(cb.equal(root.get("isActive"), true));
        
        if(accountId != null){
            predicates.add(cb.equal(root.get("accountId"), accountId));
        }

        if (Transaction.class.equals(query.getResultType())) {
            
            Fetch<Transaction, TransactionDetailedCategory> detailedFetch = 
                root.fetch("detailedCategory", JoinType.LEFT);
            Fetch<TransactionDetailedCategory, TransactionPrimaryCategory> primaryFetch = 
                detailedFetch.fetch("transactionPrimaryCategory", JoinType.LEFT);


            if (primaryCategoryCode != null && !primaryCategoryCode.isBlank()) {
 
                Join<TransactionDetailedCategory, TransactionPrimaryCategory> primaryJoin = 
                    (Join<TransactionDetailedCategory, TransactionPrimaryCategory>) primaryFetch;

                predicates.add(cb.equal(primaryJoin.get("categoryCode"), primaryCategoryCode));
            }
        } else {
            if (primaryCategoryCode != null && !primaryCategoryCode.isBlank()) {
                predicates.add(cb.equal(
                    root.join("detailedCategory").join("transactionPrimaryCategory").get("categoryCode"), 
                    primaryCategoryCode
                ));
            }
        }

        return cb.and(predicates.toArray(new Predicate[0]));
    };
}
}