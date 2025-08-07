package com.app.backend.entity;

public enum AdmissionGender {
    MALE("Male"),
    FEMALE("Female"),
    PREFER_NOT_TO_SAY("Prefer not to say");

    private final String displayName;

    AdmissionGender(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static AdmissionGender fromDisplayName(String displayName) {
        for (AdmissionGender gender : AdmissionGender.values()) {
            if (gender.getDisplayName().equalsIgnoreCase(displayName)) {
                return gender;
            }
        }
        throw new IllegalArgumentException("Invalid gender: " + displayName);
    }
}

