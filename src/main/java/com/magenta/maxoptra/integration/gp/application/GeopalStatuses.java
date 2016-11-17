package com.magenta.maxoptra.integration.gp.application;

public enum GeopalStatuses {
    UNASSIGNED("Unassigned"),
    ASSIGNED("Assigned"),
    REJECTED("Rejected"),
    COMPLETED("Completed"),
    DELETED("Deleted"),
    INPROGRESS("InProgress"),
    ACCEPTED("Accepted"),
    INCOMPLETE("Incomplete"),
    REVIEW("Review"),
    ARCHIVE("Archive"),
    LINKED("Linked"),
    CANCELLED("Linked"),
    PENDING("Pending"),
    UPDATED("Updated"),
    PLANNED("Planned");

    private String description;

    public String getDescription() {
        return description;
    }

    GeopalStatuses(String description) {
        this.description = description;
    }

}
