package com.shopping.order_service.enums;

import java.util.EnumSet;
import java.util.Set;

public enum OrderStatus {

    PENDING {
        @Override public Set<OrderStatus> allowedTransitions() {
            return EnumSet.of(PROCESSING, CANCELLED);
        }
    },
    PROCESSING {
        @Override public Set<OrderStatus> allowedTransitions() {
            return EnumSet.of(SHIPPED, CANCELLED);
        }
    },
    SHIPPED {
        @Override public Set<OrderStatus> allowedTransitions() {
            return EnumSet.of(DELIVERED);
        }
    },
    DELIVERED {
        @Override public Set<OrderStatus> allowedTransitions() {
            return EnumSet.noneOf(OrderStatus.class); // terminal state
        }
    },
    CANCELLED {
        @Override public Set<OrderStatus> allowedTransitions() {
            return EnumSet.noneOf(OrderStatus.class); // terminal state
        }
    };

    public abstract Set<OrderStatus> allowedTransitions();

    public boolean canTransitionTo(OrderStatus next) {
        return allowedTransitions().contains(next);
    }
}
