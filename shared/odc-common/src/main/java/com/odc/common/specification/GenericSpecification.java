package com.odc.common.specification;

import com.odc.common.dto.FilterRequest;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public record GenericSpecification<T>(List<FilterRequest> filters) implements Specification<T> {

    @Override
    public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        if (filters == null || filters.isEmpty()) {
            return cb.conjunction();
        }

        List<Predicate> predicates = new ArrayList<>();

        for (FilterRequest filter : filters) {
            switch (filter.getOperator()) {
                case EQUAL:
                    predicates.add(cb.equal(root.get(filter.getKey()), filter.getValue()));
                    break;
                case LIKE:
                    predicates.add(cb.like(cb.lower(root.get(filter.getKey())), "%" + String.valueOf(filter.getValue()).toLowerCase() + "%"));
                    break;
                case IN:
                    predicates.add(root.get(filter.getKey()).in((List<Object>) filter.getValue()));
                    break;
                case GREATER_THAN:
                    predicates.add(cb.greaterThan(root.get(filter.getKey()), (Comparable) filter.getValue()));
                    break;
                case LESS_THAN:
                    predicates.add(cb.lessThan(root.get(filter.getKey()), (Comparable) filter.getValue()));
                    break;
                case BETWEEN:
                    predicates.add(cb.between(root.get(filter.getKey()), (Comparable) filter.getValue(), (Comparable) filter.getValueTo()));
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported operator: " + filter.getOperator());
            }
        }

        return cb.and(predicates.toArray(new Predicate[0]));
    }
}
