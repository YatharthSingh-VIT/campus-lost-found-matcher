package com.lostfound.service;

import com.lostfound.model.Item;
import com.lostfound.storage.FileManager;
import com.lostfound.util.InvalidInputException;
import com.lostfound.util.ValidationUtil;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
public final class ReportService {
    private final FileManager storage;
    private List<Item> items;
    public ReportService(FileManager storage) throws IOException {
        this.storage = storage;
        items = storage.load();
    }
    public List<Item> all() { return List.copyOf(items); }
    public Item get(String id) {
        return items.stream().filter(item -> item.getId().equalsIgnoreCase(id.strip())).findFirst()
                .orElseThrow(() -> new InvalidInputException("No report has that ID."));
    }
    public Item add(Item.Type type, String title, String description, Item.Category category,
                    String location, LocalDate date, String contact) throws IOException {
        Item item = Item.create(type, UUID.randomUUID().toString(), title, description,
                category, location, date, contact, Item.Status.OPEN);
        List<Item> next = new ArrayList<>(items); next.add(item); persist(next); return item;
    }
    public void update(Item replacement) throws IOException {
        Item old = get(replacement.getId());
        if (old.getType() != replacement.getType())
            throw new InvalidInputException("Report type cannot be changed; create a new report instead.");
        List<Item> next = new ArrayList<>(items);
        next.set(next.indexOf(old), replacement); persist(next);
    }
    public void setStatus(String id, Item.Status status) throws IOException {
        Item old = get(id);
        update(Item.create(old.getType(), old.getId(), old.getTitle(), old.getDescription(),
                old.getCategory(), old.getLocation(), old.getDate(), old.getContact(), status));
    }
    public void delete(String id) throws IOException {
        Item old = get(id);
        List<Item> next = new ArrayList<>(items); next.remove(old); persist(next);
    }
    private void persist(List<Item> next) throws IOException {
        storage.save(next); items = List.copyOf(next);
    }
    public List<Item> search(String keyword, Item.Category category, String location,
                             Item.Type type, Item.Status status, LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to))
            throw new InvalidInputException("Start date must be on or before end date.");
        String query = ValidationUtil.normalize(keyword == null ? "" : keyword);
        String place = ValidationUtil.normalize(location == null ? "" : location);
        return items.stream()
                .filter(i -> ValidationUtil.normalize(i.getTitle() + " " + i.getDescription()).contains(query))
                .filter(i -> category == null || i.getCategory() == category)
                .filter(i -> ValidationUtil.normalize(i.getLocation()).contains(place))
                .filter(i -> type == null || i.getType() == type)
                .filter(i -> status == null || i.getStatus() == status)
                .filter(i -> from == null || !i.getDate().isBefore(from))
                .filter(i -> to == null || !i.getDate().isAfter(to))
                .toList();
    }

    public Map<String, Long> statistics() {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("Total reports", (long) items.size());
        for (Item.Type type : Item.Type.values())
            counts.put(type.name(), items.stream().filter(i -> i.getType() == type).count());
        for (Item.Status status : Item.Status.values())
            counts.put(status.name(), items.stream().filter(i -> i.getStatus() == status).count());
        for (Item.Category category : Item.Category.values())
            counts.put(category.name(), items.stream().filter(i -> i.getCategory() == category).count());
        return Collections.unmodifiableMap(counts);
    }
}
