package com.lostfound.storage;

import com.lostfound.model.Item;
import com.lostfound.util.ValidationUtil;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
public final class FileManager implements AutoCloseable {
    public static final String HEADER = "id,type,title,description,category,location,date,contact,status";
    private final Path path;
    private final FileChannel channel;
    private final FileLock lock;

    public FileManager(Path requestedPath) throws IOException {
        Path absolute = requestedPath.toAbsolutePath().normalize();
        Files.createDirectories(absolute.getParent());
        path = absolute.getParent().toRealPath().resolve(absolute.getFileName());
        if (Files.isSymbolicLink(path)) throw new IOException("Use the real CSV path, not a symbolic link.");
        channel = FileChannel.open(path.resolveSibling(path.getFileName() + ".lock"),
                StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        FileLock acquired;
        try {
            acquired = channel.tryLock();
            if (acquired == null) throw new IOException("This data file is already open in another instance.");
        } catch (IOException | OverlappingFileLockException e) {
            channel.close();
            throw new IOException("Cannot lock data file; close other instances. " + e.getMessage(), e);
        }
        lock = acquired;
    }

    public Path getPath() { return path; }

    public List<Item> load() throws IOException {
        if (!Files.exists(path)) return List.of();
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        if (lines.isEmpty() || !HEADER.equals(lines.get(0)))
            throw new IOException("CSV header missing or invalid. Original file was left unchanged.");
        List<Item> items = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        for (int i = 1; i < lines.size(); i++) {
            try {
                List<String> f = parseLine(lines.get(i));
                if (f.size() != 9) throw new IllegalArgumentException("Expected 9 fields.");
                Item item = Item.create(ValidationUtil.choice(Item.Type.class, f.get(1)),
                        f.get(0), f.get(2), f.get(3), ValidationUtil.choice(Item.Category.class, f.get(4)),
                        f.get(5), ValidationUtil.date(f.get(6)), f.get(7),
                        ValidationUtil.choice(Item.Status.class, f.get(8)));
                if (!ids.add(item.getId().toUpperCase(Locale.ROOT))) throw new IllegalArgumentException("Duplicate ID (case-insensitive).");
                items.add(item);
            } catch (IllegalArgumentException e) {
                throw new IOException("Invalid CSV row " + (i + 1) + ": " + e.getMessage()
                        + " Original file was left unchanged.", e);
            }
        }
        return List.copyOf(items);
    }

    public void save(List<Item> items) throws IOException {
        List<String> lines = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        lines.add(HEADER);
        for (Item item : items) {
            if (!ids.add(item.getId().toUpperCase(Locale.ROOT))) throw new IOException("Cannot save duplicate IDs (case-insensitive).");
            lines.add(String.join(",", List.of(item.getId(), item.getType().name(), item.getTitle(),
                    item.getDescription(), item.getCategory().name(), item.getLocation(),
                    item.getDate().toString(), item.getContact(), item.getStatus().name())
                    .stream().map(FileManager::escape).toList()));
        }
        Path temporary = Files.createTempFile(path.getParent(), "items-", ".tmp");
        try {
            Files.write(temporary, lines, StandardCharsets.UTF_8);
            try (FileChannel written = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
                written.force(true);
            }
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } finally { Files.deleteIfExists(temporary); }
    }

    private static String escape(String value) { return "\"" + value.replace("\"", "\"\"") + "\""; }
    private static List<String> parseLine(String line) {
        List<String> fields = new ArrayList<>();
        int index = 0;
        while (true) {
            StringBuilder field = new StringBuilder();
            if (index < line.length() && line.charAt(index) == '"') {
                index++;
                boolean closed = false;
                while (index < line.length()) {
                    char c = line.charAt(index++);
                    if (c == '"') {
                        if (index < line.length() && line.charAt(index) == '"') {
                            field.append('"'); index++;
                        } else { closed = true; break; }
                    } else field.append(c);
                }
                if (!closed) throw new IllegalArgumentException("Unclosed quote.");
                if (index < line.length() && line.charAt(index) != ',')
                    throw new IllegalArgumentException("Unexpected text after quote.");
            } else {
                while (index < line.length() && line.charAt(index) != ',') {
                    char c = line.charAt(index++);
                    if (c == '"') throw new IllegalArgumentException("Quote inside an unquoted field.");
                    field.append(c);
                }
            }
            fields.add(field.toString());
            if (index == line.length()) break;
            index++;
        }
        return fields;
    }

    @Override public void close() throws IOException {
        try { lock.release(); } finally { channel.close(); }
    }
}
