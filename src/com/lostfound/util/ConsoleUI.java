package com.lostfound.util;

import com.lostfound.model.Item;
import com.lostfound.service.Matcher;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
public final class ConsoleUI {
    private final BufferedReader input = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
    private final boolean styled;
    private static final int WIDTH = 70;
    public ConsoleUI(boolean plain, boolean color) {
        styled = !plain && System.getenv("NO_COLOR") == null && (color || System.console() != null);
    }
    public void heading(String title, String subtitle) {
        System.out.println();
        String border = styled ? "─".repeat(WIDTH) : "-".repeat(WIDTH);
        System.out.println((styled ? "\u001b[36m╭" : "+") + border + (styled ? "╮\u001b[0m" : "+"));
        line(title.toUpperCase(Locale.ROOT));
        line(subtitle);
        System.out.println((styled ? "\u001b[36m╰" : "+") + border + (styled ? "╯\u001b[0m" : "+"));
    }
    public void line(String text) {
        for (String paragraph : text.split("\n", -1)) {
            String remaining = paragraph;
            while (remaining.length() > WIDTH - 4) {
                int cut = remaining.lastIndexOf(' ', WIDTH - 4);
                if (cut <= 0) cut = WIDTH - 4;
                System.out.println("  " + remaining.substring(0, cut));
                remaining = remaining.substring(cut).stripLeading();
            }
            System.out.println("  " + remaining);
        }
    }
    public String ask(String label) throws IOException {
        System.out.print("  " + label + " > "); System.out.flush();
        String value = input.readLine();
        if (value == null) throw new EOFException();
        return value.strip();
    }
    public int menu(String... options) throws IOException {
        for (String option : options) line(option);
        while (true) {
            String value = ask("Choose");
            try {
                int number = Integer.parseInt(value);
                if (number >= 0 && number < options.length) return number;
            } catch (NumberFormatException ignored) { }
            error("Choose a number from 0 to " + (options.length - 1) + ".");
        }
    }
    public String required(String label, int max) throws IOException {
        while (true) {
            try { return ValidationUtil.text(ask(label), label, max); }
            catch (InvalidInputException e) { error(e.getMessage()); }
        }
    }
    public java.time.LocalDate date(String label, boolean optional) throws IOException {
        while (true) {
            String value = ask(label + " (YYYY-MM-DD" + (optional ? ", blank = any" : "") + ")");
            if (optional && value.isEmpty()) return null;
            try { return ValidationUtil.date(value); }
            catch (InvalidInputException e) { error(e.getMessage()); }
        }
    }
    public <E extends Enum<E>> E choice(Class<E> type, String label, boolean optional) throws IOException {
        line(Arrays.toString(type.getEnumConstants()));
        while (true) {
            String value = ask(label + (optional ? " (blank = any)" : ""));
            if (optional && value.isEmpty()) return null;
            try { return ValidationUtil.choice(type, value); }
            catch (InvalidInputException e) { error(e.getMessage()); }
        }
    }
    public String edit(String label, String old, int max) throws IOException {
        line(label + " now: " + old);
        while (true) {
            String value = ask(label + " (blank = keep)");
            if (value.isEmpty()) return old;
            try { return ValidationUtil.text(value, label, max); }
            catch (InvalidInputException e) { error(e.getMessage()); }
        }
    }
    public boolean confirm(String action) throws IOException { return ask(action + " (type yes)").equalsIgnoreCase("yes"); }
    public void error(String text) { line((styled ? "\u001b[33m" : "") + "Please check: " + text + (styled ? "\u001b[0m" : "")); }
    public void success(String text) { line((styled ? "\u001b[32m" : "") + text + (styled ? "\u001b[0m" : "")); }

    public void reports(List<Item> items) throws IOException {
        if (items.isEmpty()) { line("No reports to show. Try another filter or add a report."); return; }
        line(items.size() + " report(s)");
        for (int i = 0; i < items.size(); i++) {
            Item item = items.get(i);
            line(""); line(item.getType() + " / " + item.getStatus() + "   " + item.getTitle());
            line("ID: " + item.getId());
            line(item.getCategory() + " | " + item.getLocation() + " | " + item.getDate());
            if ((i + 1) % 5 == 0 && i + 1 < items.size()
                    && ask("Enter = next page, q = stop listing").equalsIgnoreCase("q")) break;
        }
    }
    public void details(Item item) {
        heading(item.getTitle(), item.getType() + " / " + item.getStatus());
        line("ID: " + item.getId()); line("Category: " + item.getCategory());
        line("Location: " + item.getLocation()); line("Incident date: " + item.getDate());
        line("Description: " + item.getDescription()); line("Contact / reference: " + item.getContact());
    }
    public void matches(List<Matcher.Match> matches) throws IOException {
        if (matches.isEmpty()) { line("No candidates reached 60/100. Try again when new reports arrive."); return; }
        line("Similarity scores are suggestions, not proof of ownership.");
        for (int index = 0; index < matches.size(); index++) {
            Matcher.Match match = matches.get(index);
            details(match.candidate());
            line("SIMILARITY  " + match.score() + "/100");
            line("Category " + match.categoryPoints() + "/35 | Location " + match.locationPoints()
                    + "/25 | Date " + match.datePoints() + "/25 | Keywords " + match.keywordPoints() + "/15");
            line("Found " + match.days() + " day(s) after loss. Shared keywords: "
                    + (match.sharedKeywords().isEmpty() ? "none" : String.join(", ", match.sharedKeywords())));
            if ((index + 1) % 5 == 0 && index + 1 < matches.size()
                    && ask("Enter = next page, q = stop listing").equalsIgnoreCase("q")) break;
        }
    }
}
