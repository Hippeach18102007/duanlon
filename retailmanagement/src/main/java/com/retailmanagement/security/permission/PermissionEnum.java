package com.retailmanagement.security.permission;

public enum PermissionEnum {

    //USER
    USER_VIEW,
    USER_CREATE,
    USER_UPDATE,
    USER_DELETE,

    //PRODUCT
    PRODUCT_VIEW,
    PRODUCT_CREATE,
    PRODUCT_UPDATE,
    PRODUCT_DELETE,

    //CATEGORY
    CATEGORY_VIEW,
    CATEGORY_CREATE,
    CATEGORY_UPDATE,
    CATEGORY_DELETE,

    //ORDER
    ORDER_VIEW,
    ORDER_CREATE,
    ORDER_UPDATE,
    ORDER_DELETE,

    //CUSTOMER
    CUSTOMER_VIEW,
    CUSTOMER_CREATE,
    CUSTOMER_UPDATE,
    CUSTOMER_DELETE,

    //REPORT
    REPORT_VIEW;

    //Helper method
    public String getValue() {
        return this.name(); // trùng với DB: MODULE_ACTION
    }
}
