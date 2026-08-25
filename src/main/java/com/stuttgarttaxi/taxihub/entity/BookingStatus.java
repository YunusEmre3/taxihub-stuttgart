package com.stuttgarttaxi.taxihub.entity;

public enum BookingStatus {
    PENDING("Pending", "badge-pending"),
    ASSIGNED("Assigned", "badge-assigned"),
    IN_PROGRESS("In Progress", "badge-in-progress"),
    COMPLETED("Completed", "badge-completed"),
    CANCELLED("Cancelled", "badge-cancelled");

    private final String displayLabel;
    private final String cssClass;

    BookingStatus(String displayLabel, String cssClass) {
        this.displayLabel = displayLabel;
        this.cssClass = cssClass;
    }

    public String getDisplayLabel() {
        return displayLabel;
    }

    public String getCssClass() {
        return cssClass;
    }
}
