package com.lostfound.app;

import com.lostfound.model.Item;
import com.lostfound.service.*;
import com.lostfound.storage.FileManager;
import com.lostfound.util.*;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;

public final class Main {
    private Main() { }
    public static void main(String[] args) {
        boolean plain = false, color = false, demo = false, customData = false;
        Path data = Path.of("data", "items.csv");
        try {
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--plain" -> plain = true;
                    case "--color" -> color = true;
                    case "--demo" -> demo = true;
                    case "--data" -> {
                        if (++i == args.length) throw new InvalidInputException("--data needs a file path.");
                        data = Path.of(args[i]); customData = true;
                    }
                    case "--help" -> {
                        System.out.println("Campus Lost & Found Matcher\nUsage: java -cp out com.lostfound.app.Main"
                                + " [--plain | --color] [--demo | --data PATH]\n"
                                + "--demo copies fictional examples to data/demo-items.csv only on first use.\n"
                                + "Run from the repository root. Java 21 or newer is required.");
                        return;
                    }
                    default -> throw new InvalidInputException("Unknown option: " + args[i]);
                }
            }
            if (demo && customData) throw new InvalidInputException("Use either --demo or --data, not both.");
            if (demo) data = Path.of("data", "demo-items.csv");
            ConsoleUI ui = new ConsoleUI(plain, color);
            try (FileManager storage = new FileManager(data)) {
                if (demo && !Files.exists(storage.getPath()))
                    Files.copy(Path.of("examples", "items.csv"), storage.getPath());
                ReportService service = new ReportService(storage);
                ui.heading("Campus Lost & Found", "A little structure. A better chance of finding it.");
                ui.line(demo ? "DEMO | Fictional reports in a separate data file." : "LOCAL HELP DESK | Reports save after every change.");
                ui.line("Data: " + storage.getPath());
                try { run(ui, service); }
                catch (EOFException e) { ui.line("Input ended. Completed changes are already saved. Goodbye."); }
            }
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("Cannot continue: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void run(ConsoleUI ui, ReportService service) throws IOException {
        while (true) {
            ui.heading("Home", "Report. Search. Reconnect.");
            int action = ui.menu("1  Add a report", "2  View & manage reports", "3  Find potential matches",
                    "4  Search reports", "5  Statistics", "0  Exit");
            try {
                switch (action) {
                    case 0 -> { ui.line("Your saved reports will be here next time. Goodbye."); return; }
                    case 1 -> add(ui, service);
                    case 2 -> manage(ui, service);
                    case 3 -> {
                        String id = ui.ask("Report ID (blank = back)");
                        if (!id.isEmpty()) {
                            Item source = service.get(id); ui.details(source);
                            if (source.getStatus() == Item.Status.RESOLVED) ui.line("Reopen this report to look for matches.");
                            else ui.matches(new Matcher().findMatches(source, service.all()));
                        }
                    }
                    case 4 -> search(ui, service);
                    case 5 -> {
                        ui.heading("Campus snapshot", "Counts describe reports, not verified item returns.");
                        service.statistics().forEach((label, count) -> ui.line(label + ": " + count));
                    }
                    default -> throw new AssertionError("Unexpected menu choice.");
                }
            } catch (InvalidInputException e) { ui.error(e.getMessage()); }
            catch (EOFException e) { throw e; }
            catch (IOException e) { ui.error("Operation failed: " + e.getMessage()); }
        }
    }

    private static void add(ConsoleUI ui, ReportService service) throws IOException {
        ui.heading("New report", "Use a help-desk reference if you prefer not to store personal contact details.");
        Item.Type type = ui.choice(Item.Type.class, "Type", false);
        String title = ui.required("Title", 80), description = ui.required("Description", 500);
        Item.Category category = ui.choice(Item.Category.class, "Category", false);
        String location = ui.required("Location", 80);
        LocalDate date = ui.date("Incident date", false);
        String contact = ui.required("Contact / help-desk reference", 120);
        if (!ui.confirm("Save this report?")) { ui.line("Report cancelled."); return; }
        Item item = service.add(type, title, description, category, location, date, contact);
        ui.success("Saved. Report ID: " + item.getId());
        ui.matches(new Matcher().findMatches(item, service.all()));
    }

    private static void manage(ConsoleUI ui, ReportService service) throws IOException {
        ui.heading("Your reports", "Use the full ID shown below to open a report.");
        ui.reports(service.all());
        if (service.all().isEmpty()) return;
        String id = ui.ask("Report ID (blank = back)");
        if (id.isEmpty()) return;
        Item item = service.get(id); ui.details(item);
        int action = ui.menu("1  Edit details", "2  Resolve / reopen", "3  Delete report", "0  Back");
        switch (action) {
            case 1 -> edit(ui, service, item);
            case 2 -> {
                Item.Status next = item.getStatus() == Item.Status.OPEN ? Item.Status.RESOLVED : Item.Status.OPEN;
                if (ui.confirm("Change status to " + next + "?")) {
                    service.setStatus(item.getId(), next); ui.success("Status saved.");
                }
            }
            case 3 -> {
                if (ui.confirm("Permanently delete this report?")) {
                    service.delete(item.getId()); ui.success("Report deleted.");
                }
            }
            default -> { }
        }
    }

    private static void edit(ConsoleUI ui, ReportService service, Item old) throws IOException {
        String title = ui.edit("Title", old.getTitle(), 80);
        String description = ui.edit("Description", old.getDescription(), 500);
        Item.Category category = ui.choice(Item.Category.class, "Category (blank = keep current)", true);
        String location = ui.edit("Location", old.getLocation(), 80);
        LocalDate date = ui.date("Incident date (blank = keep current)", true);
        String contact = ui.edit("Contact / reference", old.getContact(), 120);
        Item replacement = Item.create(old.getType(), old.getId(), title, description,
                category == null ? old.getCategory() : category, location,
                date == null ? old.getDate() : date, contact, old.getStatus());
        if (ui.confirm("Save these changes?")) { service.update(replacement); ui.success("Changes saved."); }
    }

    private static void search(ConsoleUI ui, ReportService service) throws IOException {
        ui.heading("Search", "Leave a filter blank to include any value. Dates are inclusive.");
        String keyword = ui.ask("Title / description contains"), location = ui.ask("Location contains");
        Item.Category category = ui.choice(Item.Category.class, "Category", true);
        Item.Type type = ui.choice(Item.Type.class, "Type", true);
        Item.Status status = ui.choice(Item.Status.class, "Status", true);
        LocalDate from = ui.date("From", true), to = ui.date("To", true);
        ui.reports(service.search(keyword, category, location, type, status, from, to));
    }
}
