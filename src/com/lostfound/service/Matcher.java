package com.lostfound.service;

import com.lostfound.model.Item;
import com.lostfound.util.ValidationUtil;
import java.time.temporal.ChronoUnit;
import java.util.*;
public final class Matcher {
    public static final int THRESHOLD = 60;
    public static final int MAX_DAYS = 30;
    private static final Set<String> STOP_WORDS = Set.of("a", "an", "the", "and", "or", "in", "on",
            "at", "to", "of", "with", "my", "lost", "found", "item");

    public record Match(Item candidate, int score, int categoryPoints, int locationPoints,
                        int datePoints, int keywordPoints, long days, List<String> sharedKeywords) { }

    public Optional<Match> score(Item source, Item candidate) {
        if (source.getId().equalsIgnoreCase(candidate.getId()) || source.getType() == candidate.getType()
                || source.getStatus() != Item.Status.OPEN || candidate.getStatus() != Item.Status.OPEN
                || source.getCategory() != candidate.getCategory()) return Optional.empty();
        Item lost = source.getType() == Item.Type.LOST ? source : candidate;
        Item found = source.getType() == Item.Type.FOUND ? source : candidate;
        long days = ChronoUnit.DAYS.between(lost.getDate(), found.getDate());
        if (days < 0 || days > MAX_DAYS) return Optional.empty();
        int location = ValidationUtil.normalize(source.getLocation())
                .equals(ValidationUtil.normalize(candidate.getLocation())) ? 25 : 0;
        int date = (int) Math.round(25.0 * (MAX_DAYS - days) / MAX_DAYS);
        Set<String> left = tokens(source), right = tokens(candidate);
        Set<String> union = new HashSet<>(left); union.addAll(right);
        Set<String> shared = new TreeSet<>(left); shared.retainAll(right);
        int keywords = union.isEmpty() ? 0 : (int) Math.round(15.0 * shared.size() / union.size());
        return Optional.of(new Match(candidate, 35 + location + date + keywords,
                35, location, date, keywords, days, List.copyOf(shared)));
    }

    public List<Match> findMatches(Item source, List<Item> reports) {
        return reports.stream().map(item -> score(source, item)).flatMap(Optional::stream)
                .filter(match -> match.score() >= THRESHOLD)
                .sorted(Comparator.comparingInt(Match::score).reversed()
                        .thenComparingLong(Match::days).thenComparing(m -> m.candidate().getId()))
                .toList();
    }

    private Set<String> tokens(Item item) {
        Set<String> result = new HashSet<>(Arrays.asList(ValidationUtil.normalize(
                item.getTitle() + " " + item.getDescription()).split("[^\\p{L}\\p{N}]+")));
        result.removeIf(word -> word.length() < 2 || STOP_WORDS.contains(word));
        return result;
    }
}
