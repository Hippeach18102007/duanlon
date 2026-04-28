package com.retailmanagement.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

@Getter
public enum CustomerType {
    REGULAR("Khách hàng thường", 0),
    VIP("Khách hàng VIP", 1);
    private final String displayname;
    private final int level;
    CustomerType(String displayname, int level) {
        this.displayname = displayname;
        this.level = level;
    }
    public boolean isVip() {
        return this == VIP;
    }
    public static CustomerType fromString(String value) {
        if (value == null) {
            return REGULAR;
        }
        try {
            return CustomerType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return REGULAR;
        }
    }
    public static CustomerType fromDisplayName(String displayname) {
        for (CustomerType type : CustomerType.values()) {
            if (type.displayname.equalsIgnoreCase(displayname)) {
                return type;
            }
        }
        return REGULAR;
    }
    @JsonCreator
    public static CustomerType fromJson(String value) {
        if (value == null) return REGULAR;
        return CustomerType.valueOf(value.trim().toUpperCase());
    }

}
