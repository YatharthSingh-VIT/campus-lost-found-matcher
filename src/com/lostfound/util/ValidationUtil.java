package com.lostfound.util;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;

public final class ValidationUtil {
    private ValidationUtil() { }

    public static String text(String value, String field, int max) {
        if (value == null || value.isBlank())
            throw new InvalidInputException(field + " is required.");
        String clean = value.strip();
        if (clean.length() > max)
            throw new InvalidInputException(field + " must be at most " + max + " characters.");
        if (value.codePoints().anyMatch(c -> Character.isISOControl(c)
                || Character.getType(c) == Character.FORMAT))
            throw new InvalidInputException(field + " must be a single line without control characters.");
        return clean;
    }

    public static LocalDate date(String value) {
        try {
            if (value == null || !value.matches("\\d{4}-\\d{2}-\\d{2}"))
                throw new DateTimeParseException("format", "", 0);
            LocalDate date = LocalDate.parse(value);
            if (date.isAfter(LocalDate.now()))
                throw new InvalidInputException("The incident date cannot be in the future.");
            return date;
        } catch (DateTimeParseException e) {
            throw new InvalidInputException("Enter a real date in YYYY-MM-DD format.");
        }
    }

    public static <E extends Enum<E>> E choice(Class<E> type, String value) {
        try { return Enum.valueOf(type, value.strip().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException | NullPointerException e) {
            throw new InvalidInputException("Choose one of: "
                    + java.util.Arrays.toString(type.getEnumConstants()));
        }
    }

    public static String normalize(String value) {
        return value.strip().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }
}
