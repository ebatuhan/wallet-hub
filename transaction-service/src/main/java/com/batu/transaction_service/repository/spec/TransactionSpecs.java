package com.batu.transaction_service.repository.spec;

import com.batu.transaction_service.entity.Transaction;
import com.batu.transaction_service.entity.TransactionDetailedCategory;
import com.batu.transaction_service.entity.TransactionPrimaryCategory;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public class TransactionSpecs {

    public static Specification<Transaction> withDynamicFilter(String userId, String search, String categoryDisplayName) {
        return (root, query, cb) -> {
            
            // 1. Detect if this is a Count Query or a Data Query
            // Count queries return Long; Data queries return Transaction
            boolean isCountQuery = query.getResultType() == Long.class || query.getResultType() == long.class;

            // Variables to hold the joins so we can reuse them for filtering
            Join<Transaction, TransactionDetailedCategory> detailedJoin = null;
            Join<TransactionDetailedCategory, TransactionPrimaryCategory> primaryJoin = null;

            // 2. Handle Fetching (Only for Data Query)
            // We cast Fetch to Join to allow using it in the Predicates later.
            // This prevents the "Redundant Join" issue where Hibernate generates one JOIN for fetch and another for where.
            if (!isCountQuery) {
                Fetch<Transaction, TransactionDetailedCategory> detailedFetch = root.fetch("detailedCategory", JoinType.LEFT);
                detailedJoin = (Join<Transaction, TransactionDetailedCategory>) detailedFetch;

                Fetch<TransactionDetailedCategory, TransactionPrimaryCategory> primaryFetch = detailedFetch.fetch("transactionPrimaryCategory", JoinType.LEFT);
                primaryJoin = (Join<TransactionDetailedCategory, TransactionPrimaryCategory>) primaryFetch;
            }

            var predicate = cb.conjunction();

            // 3. User ID Filter
            if (userId != null) {
                predicate = cb.and(predicate, cb.equal(root.get("userId"), UUID.fromString(userId)));
            }

            // 4. Search Filter
            if (search != null && !search.isBlank()) {
                predicate = cb.and(predicate, cb.like(root.get("transactionName"), "%" + search + "%"));
            }

            // 5. Category Filter (The tricky part)
            if (categoryDisplayName != null && !categoryDisplayName.isBlank()) {
                Path<String> categoryPath;

                if (primaryJoin != null) {
                    // Scenario A: Data Query
                    // Reuse the existing FETCH join so we don't create a second join in SQL
                    categoryPath = primaryJoin.get("displayName");
                } else {
                    // Scenario B: Count Query
                    // We haven't fetched anything, so we must create standard joins now to filter.
                    // We use join() instead of fetch() because fetch is invalid in Count queries.
                    categoryPath = root.join("detailedCategory", JoinType.LEFT)
                                       .join("transactionPrimaryCategory", JoinType.LEFT)
                                       .get("displayName");
                }

                predicate = cb.and(predicate, cb.equal(categoryPath, categoryDisplayName));
            }

            return predicate;
        };
    }
}