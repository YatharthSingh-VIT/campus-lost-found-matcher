package com.lostfound.model;

import java.time.LocalDate;

public final class FoundItem extends Item {
    public FoundItem(String id, String title, String description, Category category,
                     String location, LocalDate date, String contact, Status status) {
        super(id, title, description, category, location, date, contact, status);
    }
    @Override public Type getType() { return Type.FOUND; }
}
