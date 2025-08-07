package com.app.backend.entity;

public enum ClassLevel {
    NURSERY("Nursery"),
    LKG("LKG"),
    UKG("UKG"),
    FIRST_CLASS("1st Class"),
    SECOND_CLASS("2nd Class"),
    THIRD_CLASS("3rd Class"),
    FOURTH_CLASS("4th Class"),
    FIFTH_CLASS("5th Class"),
    SIXTH_CLASS("6th Class"),
    SEVENTH_CLASS("7th Class"),
    EIGHTH_CLASS("8th Class"),
    NINTH_CLASS("9th Class"),
    TENTH_CLASS("10th Class");

    private final String displayName;

    ClassLevel(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static ClassLevel fromDisplayName(String displayName) {
        for (ClassLevel classLevel : ClassLevel.values()) {
            if (classLevel.getDisplayName().equalsIgnoreCase(displayName)) {
                return classLevel;
            }
        }
        throw new IllegalArgumentException("Invalid class level: " + displayName);
    }
}
