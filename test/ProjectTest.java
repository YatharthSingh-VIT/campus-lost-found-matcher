package com.lostfound;

import com.lostfound.model.Item;
import com.lostfound.service.*;
import com.lostfound.storage.FileManager;
import com.lostfound.util.InvalidInputException;
import com.lostfound.util.ValidationUtil;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;
import static com.lostfound.model.Item.*;
public final class ProjectTest {
    private ProjectTest() { }

    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("lost-found-tests-");
        try {
            testMatching(); testValidation(); testStorage(directory); testReports(directory); testCli(directory);
            List<Item> reports = new ArrayList<>();
            for (int i = 0; i < 10_000; i++) reports.add(item(Item.Type.FOUND, "F" + i, i % 31,
                    "Blue bottle", "Library", Item.Category.OTHER, Item.Status.OPEN));
            Item lost = item(Item.Type.LOST, "L", 0, "Blue bottle", "Library", Item.Category.OTHER, Item.Status.OPEN);
            long start = System.nanoTime();
            int matches = new Matcher().findMatches(lost, reports).size();
            double ms = (System.nanoTime() - start) / 1_000_000.0;
            check(matches == 10_000, "10,000 eligible synthetic candidates ranked");
            System.out.printf(Locale.ROOT, "BENCHMARK single-source 10,000-candidate matching: %.2f ms%n", ms);
            System.out.println("SUCCESS: " + count() + " checks passed.");
        } finally {
            try (var paths = Files.walk(directory)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
    }
    private static int checks;
    public interface CheckedAction { void run() throws Exception; }
    public static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
        System.out.println("PASS " + message);
    }
    public static void fails(Class<? extends Throwable> type, CheckedAction action, String message) throws Exception {
        try { action.run(); }
        catch (Throwable e) {
            if (!type.isInstance(e)) throw new AssertionError(message + ": wrong exception", e);
            check(true, message); return;
        }
        throw new AssertionError(message + ": no exception");
    }
    public static int count() { return checks; }
    public static Item item(Item.Type type, String id, int days, String title, String location,
                            Item.Category category, Item.Status status) {
        return Item.create(type, id, title, title, category, location,
                LocalDate.now().minusDays(60).plusDays(days), "Test desk", status);
    }
    private static void testMatching() {
        Matcher matcher = new Matcher();
        Item lost = item(Type.LOST, "L1", 0, "Blue bottle", "Library", Category.OTHER, Status.OPEN);
        Item found = item(Type.FOUND, "F1", 0, "Blue bottle", " library ", Category.OTHER, Status.OPEN);
        check(matcher.score(lost, found).orElseThrow().score() == 100, "identical evidence scores 100");
        check(matcher.score(found, lost).orElseThrow().score() == 100, "matching works in both directions");
        check(matcher.score(lost, lost).isEmpty(), "report never matches itself");
        check(matcher.score(lost, item(Type.FOUND, "l1", 0, "Blue bottle", "Library", Category.OTHER, Status.OPEN)).isEmpty(), "same ID with different letter case cannot match");
        check(matcher.score(lost, item(Type.LOST, "L2", 0, "Blue bottle", "Library", Category.OTHER, Status.OPEN)).isEmpty(), "same types excluded");
        check(matcher.score(lost, item(Type.FOUND, "F2", 0, "Blue bottle", "Library", Category.BAGS, Status.OPEN)).isEmpty(), "different categories excluded");
        check(matcher.score(lost, item(Type.FOUND, "F2", 0, "Blue bottle", "Library", Category.OTHER, Status.RESOLVED)).isEmpty(), "resolved candidate excluded");
        check(matcher.score(item(Type.LOST, "LR", 0, "Blue bottle", "Library", Category.OTHER, Status.RESOLVED), found).isEmpty(), "resolved source excluded");
        check(matcher.score(lost, item(Type.FOUND, "F2", -1, "Blue bottle", "Library", Category.OTHER, Status.OPEN)).isEmpty(), "found before loss excluded");
        check(matcher.score(lost, item(Type.FOUND, "F2", 31, "Blue bottle", "Library", Category.OTHER, Status.OPEN)).isEmpty(), "31-day gap excluded");
        Item boundary = item(Type.FOUND, "F2", 30, "Red umbrella", "Library", Category.OTHER, Status.OPEN);
        check(matcher.score(lost, boundary).orElseThrow().score() == 60, "30-day boundary has zero date points");
        check(matcher.findMatches(lost, List.of(boundary)).size() == 1, "threshold of 60 is inclusive");
        Item weak = item(Type.FOUND, "F3", 30, "Red umbrella", "Gate", Category.OTHER, Status.OPEN);
        check(matcher.findMatches(lost, List.of(weak)).isEmpty(), "scores below threshold hidden");
        var overlap = matcher.score(lost, item(Type.FOUND, "F4", 1, "blue steel bottle", "Library", Category.OTHER, Status.OPEN)).orElseThrow();
        check(overlap.keywordPoints() == 10 && overlap.datePoints() == 24 && overlap.score() == 94, "Jaccard 2/3 and one-day rounding produce 94");
        check(overlap.sharedKeywords().equals(List.of("blue", "bottle")), "shared keywords sorted and explained");
        check(matcher.score(lost, item(Type.FOUND, "F5", 0, "The BLUE, bottle!", "Library", Category.OTHER, Status.OPEN)).orElseThrow().keywordPoints() == 15, "punctuation case and stop words normalized");
        Item emptyLost = item(Type.LOST, "LE", 0, "the", "Gate", Category.OTHER, Status.OPEN);
        check(matcher.score(emptyLost, item(Type.FOUND, "FE", 0, "and", "Gate", Category.OTHER, Status.OPEN)).orElseThrow().keywordPoints() == 0, "empty keyword union does not divide by zero");
        Item tie = item(Type.FOUND, "F0", 0, "Blue bottle", "Library", Category.OTHER, Status.OPEN);
        check(matcher.findMatches(lost, List.of(boundary, found, tie)).get(0).candidate().getId().equals("F0"), "ranking breaks equal score and date ties by ID");
        check(matcher.findMatches(lost, List.of()).isEmpty(), "empty collection returns no matches");
    }
    private static void testValidation() throws Exception {
        check(ValidationUtil.text("  Blue bottle  ", "Title", 80).equals("Blue bottle"), "text trimmed");
        fails(InvalidInputException.class, () -> ValidationUtil.text(" ", "Title", 80), "blank text rejected");
        fails(InvalidInputException.class, () -> ValidationUtil.text(null, "Title", 80), "null text rejected");
        fails(InvalidInputException.class, () -> ValidationUtil.text("abcd", "Title", 3), "overlong text rejected");
        fails(InvalidInputException.class, () -> ValidationUtil.text("hello\nworld", "Title", 80), "multiline text rejected");
        fails(InvalidInputException.class, () -> ValidationUtil.text("\u001b[31m", "Title", 80), "terminal escape injection rejected");
        check(ValidationUtil.date("2024-02-29").getDayOfMonth() == 29, "valid leap day accepted");
        fails(InvalidInputException.class, () -> ValidationUtil.date("2025-02-29"), "invalid leap day rejected");
        fails(InvalidInputException.class, () -> ValidationUtil.date("2024-2-01"), "non-ISO date format rejected");
        fails(InvalidInputException.class, () -> ValidationUtil.date(LocalDate.now().plusDays(1).toString()), "future incident date rejected");
        check(ValidationUtil.date(LocalDate.now().toString()).equals(LocalDate.now()), "today accepted");
        check(ValidationUtil.choice(Item.Type.class, " lost ") == Item.Type.LOST, "enum input ignores case and outer whitespace");
        fails(InvalidInputException.class, () -> ValidationUtil.choice(Item.Type.class, "missing"), "unknown enum rejected");
        check(ValidationUtil.normalize(" CENTRAL   Library ").equals("central library"), "location spacing normalized");
    }
    private static void testStorage(Path directory) throws Exception {
        Path csv = directory.resolve("storage.csv");
        Item source = item(Type.LOST, "L1", 0, "Blue, \"steel\" café bottle", "Library", Category.OTHER, Status.OPEN);
        try (FileManager file = new FileManager(csv)) {
            check(file.load().isEmpty(), "missing CSV starts empty");
            file.save(List.of(source));
            check(file.load().get(0).getTitle().equals(source.getTitle()), "UTF-8 commas and quotes round trip");
            fails(IOException.class, () -> { try (FileManager ignored = new FileManager(csv)) { ignored.load(); } }, "second instance cannot lock same data file");
            fails(IOException.class, () -> file.save(List.of(source, source)), "duplicate IDs rejected on save");
            Item caseVariant = item(Type.FOUND, "l1", 0, "Other bottle", "Library", Category.OTHER, Status.OPEN);
            fails(IOException.class, () -> file.save(List.of(source, caseVariant)), "case-insensitive duplicate IDs rejected on save");
            check(file.load().size() == 1, "rejected save preserves existing file");
        }
        try (FileManager file = new FileManager(csv)) {
            check(file.load().size() == 1, "reports survive close and reopen");
            file.save(List.of()); check(file.load().isEmpty(), "header-only CSV represents zero reports");
        }
        Files.writeString(csv, "bad header\n");
        try (FileManager file = new FileManager(csv)) {
            fails(IOException.class, file::load, "invalid header fails closed");
            check(Files.readString(csv).equals("bad header\n"), "corrupt input left unchanged");
        }
        Files.writeString(csv, FileManager.HEADER + "\n\"unterminated\n");
        try (FileManager file = new FileManager(csv)) { fails(IOException.class, file::load, "unclosed CSV quote rejected"); }
        try (FileManager file = new FileManager(csv)) { file.save(List.of(source)); }
        String good = Files.readString(csv);
        String row = good.lines().skip(1).findFirst().orElseThrow();
        Files.writeString(csv, good + row + "\n");
        try (FileManager file = new FileManager(csv)) { fails(IOException.class, file::load, "duplicate IDs rejected on load"); }
        Files.writeString(csv, good + row.replace("\"L1\"", "\"l1\"") + "\n");
        try (FileManager file = new FileManager(csv)) { fails(IOException.class, file::load, "case-insensitive duplicate IDs rejected on load"); }
        Files.writeString(csv, FileManager.HEADER + "\nshort,row\n");
        try (FileManager file = new FileManager(csv)) { fails(IOException.class, file::load, "wrong field count rejected"); }
    }
    private static void testReports(Path directory) throws Exception {
        Path csv = directory.resolve("service.csv");
        LocalDate day = LocalDate.now().minusDays(3);
        try (FileManager file = new FileManager(csv)) {
            ReportService service = new ReportService(file);
            Item added = service.add(Type.LOST, "Blue bottle", "Moon sticker", Category.OTHER, "Library", day, "Desk A");
            check(service.get(added.getId()).getTitle().equals("Blue bottle"), "create and retrieve report");
            Item other = service.add(Type.FOUND, "Red keys", "Red ring", Category.KEYS, "Gate", day.plusDays(1), "Desk B");
            check(!added.getId().equals(other.getId()), "new reports receive distinct IDs");
            service.update(Item.create(Type.LOST, added.getId(), "Steel bottle", "Moon sticker", Category.OTHER, "Library", day, "Desk A", Status.OPEN));
            check(service.get(added.getId()).getTitle().equals("Steel bottle"), "edit preserves ID and updates fields");
            check(service.search("STEEL", Category.OTHER, "lib", Type.LOST, Status.OPEN, day, day).size() == 1, "combined filters and inclusive dates work");
            check(service.search("", null, "", null, null, null, null).size() == 2, "blank filters return all reports");
            check(service.search("purple", null, "", null, null, null, null).isEmpty(), "unmatched search returns empty");
            fails(InvalidInputException.class, () -> service.search("", null, "", null, null, day.plusDays(1), day), "reversed date range rejected");
            service.setStatus(added.getId(), Status.RESOLVED);
            check(service.statistics().get("RESOLVED") == 1L, "resolved status counted");
            service.setStatus(added.getId(), Status.OPEN);
            check(service.get(added.getId()).getStatus() == Status.OPEN, "resolved report can reopen");
            check(service.statistics().get("Total reports") == 2L && service.statistics().get("KEYS") == 1L, "total and category statistics correct");
            fails(UnsupportedOperationException.class, () -> service.all().clear(), "callers cannot mutate service collection");
            fails(InvalidInputException.class, () -> service.get("unknown"), "unknown ID produces useful error");
            String original = Files.readString(csv);
            Files.delete(csv); Files.createDirectory(csv); Files.writeString(csv.resolve("blocker"), "test");
            try {
                fails(IOException.class, () -> service.delete(other.getId()), "storage replacement failure propagated");
                check(service.all().size() == 2, "failed save leaves in-memory state unchanged");
            } finally {
                Files.delete(csv.resolve("blocker")); Files.delete(csv); Files.writeString(csv, original);
            }
            service.delete(other.getId());
            check(service.all().size() == 1, "delete removes chosen report");
            check(new ReportService(file).all().size() == 1, "CRUD changes persisted to disk");
        }
    }
    private record Result(int code, String output) { }
    private static Result launch(Path directory, String input, String... args) throws Exception {
        List<String> command = new ArrayList<>(List.of(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-cp", Path.of("out").toAbsolutePath().toString(), "com.lostfound.app.Main", "--plain"));
        command.addAll(List.of(args));
        Path output = directory.resolve("cli-output.txt");
        Process process = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(output.toFile()).start();
        try (var stdin = process.getOutputStream()) { stdin.write(input.getBytes(StandardCharsets.UTF_8)); }
        if (!process.waitFor(15, TimeUnit.SECONDS)) { process.destroyForcibly(); throw new AssertionError("CLI timed out"); }
        return new Result(process.exitValue(), Files.readString(output));
    }
    private static void testCli(Path directory) throws Exception {
        Path csv = directory.resolve("cli.csv");
        String day = LocalDate.now().minusDays(1).toString();
        Result add = launch(directory, "1\nlost\nBlue bottle\nMoon sticker\nother\nLibrary\n" + day + "\nDesk A\nyes\n0\n", "--data", csv.toString());
        check(add.code == 0 && add.output.contains("Saved. Report ID:"), "CLI creates report and exits successfully");
        String id = add.output.split("Saved. Report ID: ")[1].split("\\s")[0];
        Result edit = launch(directory, "2\n" + id + "\n1\nSteel bottle\n\n\n\n\n\nyes\n0\n", "--data", csv.toString());
        check(edit.code == 0 && edit.output.contains("Changes saved.") && Files.readString(csv).contains("Steel bottle"), "CLI edits report with keep-current fields");
        Result search = launch(directory, "4\nsteel\n\n\n\n\n\n\n0\n", "--data", csv.toString());
        check(search.output.contains("1 report(s)") && search.output.contains("Steel bottle"), "CLI search returns saved report");
        Result status = launch(directory, "2\n" + id + "\n2\nyes\n3\n" + id + "\n0\n", "--data", csv.toString());
        check(status.output.contains("Reopen this report"), "CLI resolves report and excludes matching");
        Result reopen = launch(directory, "2\n" + id + "\n2\nyes\n0\n", "--data", csv.toString());
        check(reopen.output.contains("Status saved.") && Files.readString(csv).contains("OPEN"), "CLI reopens report");
        Result cancel = launch(directory, "2\n" + id + "\n3\nno\n0\n", "--data", csv.toString());
        check(cancel.code == 0 && Files.readString(csv).contains(id), "CLI cancelled deletion preserves report");
        Result delete = launch(directory, "2\n" + id + "\n3\nyes\n0\n", "--data", csv.toString());
        check(delete.output.contains("Report deleted.") && Files.readAllLines(csv).size() == 1, "CLI confirmed deletion persists");
        Result eof = launch(directory, "1\nlost\n", "--data", csv.toString());
        check(eof.code == 0 && eof.output.contains("Input ended.") && Files.readAllLines(csv).size() == 1, "EOF during form exits without partial report");
        Result invalid = launch(directory, "banana\n9\n0\n", "--data", csv.toString());
        check(invalid.code == 0 && invalid.output.contains("Choose a number"), "CLI recovers from invalid menu input");
        check(launch(directory, "", "--unknown").code == 1, "unknown command-line option exits nonzero");
        check(launch(directory, "", "--demo", "--data", csv.toString()).code == 1, "conflicting data options rejected");
        check(launch(directory, "", "--data").code == 1, "missing data path rejected");
        check(!add.output.contains("\u001b"), "plain mode emits no ANSI escape sequences");
    }
}
