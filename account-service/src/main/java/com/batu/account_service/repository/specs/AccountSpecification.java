package com.batu.account_service.repository.specs;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.batu.account_service.entity.Account;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AccountSpecification {

    public static Specification<Account> filter(
            UUID userId,
            String accountName,
            String institutionId,
            String accountType,
            String accountSubtype) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("userId"), userId));
            predicates.add(cb.equal(root.get("isActive"), true));

            if (accountName != null && !accountName.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("accountName")), "%" + accountName.toLowerCase() + "%"));
            }

            if (institutionId != null && !institutionId.isBlank()) {
                try {

                    UUID connectionUuid = UUID.fromString(institutionId);

                    predicates.add(cb.equal(root.get("connectionId"), connectionUuid));
                } catch (IllegalArgumentException e) {

                    predicates.add(cb.disjunction());
                }
            }

            if (accountType != null && !accountType.isBlank()) {
                predicates.add(cb.equal(root.get("accountType"), accountType));
            }

            if (accountSubtype != null && !accountSubtype.isBlank()) {
                predicates.add(cb.equal(root.get("accountSubtype"), accountSubtype));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
