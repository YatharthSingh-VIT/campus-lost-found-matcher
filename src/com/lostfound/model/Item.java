package com.lostfound.model;

import com.lostfound.util.ValidationUtil;
import com.lostfound.util.InvalidInputException;
import java.time.LocalDate;
import java.util.Objects;
public abstract class Item {
    public enum Type { LOST, FOUND }
    public enum Category { ELECTRONICS, ID_CARD, KEYS, BOOKS, CLOTHING, BAGS, OTHER }
    public enum Status { OPEN, RESOLVED }

    private final String id, title, description, location, contact;
    private final Category category;
    private final LocalDate date;
    private final Status status;

    protected Item(String id, String title, String description, Category category,
                   String location, LocalDate date, String contact, Status status) {
        this.id = ValidationUtil.text(id, "ID", 64);
        if (!id.matches("[A-Za-z0-9-]+")) throw new InvalidInputException("Invalid report ID.");
        this.title = ValidationUtil.text(title, "Title", 80);
        this.description = ValidationUtil.text(description, "Description", 500);
        this.category = Objects.requireNonNull(category, "Category is required.");
        this.location = ValidationUtil.text(location, "Location", 80);
        this.date = ValidationUtil.date(Objects.requireNonNull(date, "Date is required.").toString());
        this.contact = ValidationUtil.text(contact, "Contact / help-desk reference", 120);
        this.status = Objects.requireNonNull(status, "Status is required.");
    }

    public abstract Type getType();
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Category getCategory() { return category; }
    public String getLocation() { return location; }
    public LocalDate getDate() { return date; }
    public String getContact() { return contact; }
    public Status getStatus() { return status; }

    public static Item create(Type type, String id, String title, String description,
                              Category category, String location, LocalDate date,
                              String contact, Status status) {
        Objects.requireNonNull(type, "Type is required.");
        return type == Type.LOST
                ? new LostItem(id, title, description, category, location, date, contact, status)
                : new FoundItem(id, title, description, category, location, date, contact, status);
    }
}
