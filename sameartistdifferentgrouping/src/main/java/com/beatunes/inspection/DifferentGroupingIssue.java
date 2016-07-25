/*
 * =================================================
 * Copyright 2016 tagtraum industries incorporated
 * All rights reserved.
 * =================================================
 */
package com.beatunes.inspection;

import com.tagtraum.audiokern.AudioSong;
import com.tagtraum.beatunes.inspection.Inspector;
import com.tagtraum.beatunes.inspection.Issue;
import com.tagtraum.beatunes.inspection.Solution;
import com.tagtraum.beatunes.library.ITunesMusicLibrary;

import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Different Grouping Issue.
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public class DifferentGroupingIssue implements Issue {

    private SameArtistDifferentGroupingInspector inspector;
    private Set<String> groupings;
    private String artist;
    private String description;
    private Solution[] solutions;

    /**
     * @param artist    artist that has more than one grouping
     * @param groupings groupings
     */
    public DifferentGroupingIssue(final SameArtistDifferentGroupingInspector inspector, final String artist, final Set<String> groupings) {
        this.inspector = inspector;
        this.groupings = groupings;
        this.artist = artist;
        this.description = createDescription();
        this.solutions = createSolutions();
    }

    private String createDescription() {
        final StringBuilder sb = new StringBuilder();
        for (Iterator<String> groupingIterator = groupings.iterator(); groupingIterator.hasNext(); ) {
            final String grouping = groupingIterator.next();
            if (grouping != null && grouping.length() > 0) sb.append(grouping);
            else sb.append("&lt;no grouping&gt;");
            if (groupingIterator.hasNext()) sb.append(", ");
        }
        return MessageFormat.format("Groupings for {0}: {1}", artist, sb.toString());
    }

    private Solution[] createSolutions() {

        // count number of songs per grouping
        final Map<String, Integer> songsPerGrouping = new HashMap<>();
        String mostUsedGrouping = null;
        int mostUsedCount = 0;
        for (final AudioSong song : getSongs()) {
            final String grouping = song.getGrouping();
            if (grouping != null && grouping.length() > 0) {
                Integer integer = songsPerGrouping.get(grouping);
                if (integer == null) {
                    integer = 0;
                }
                final int newCount = integer + 1;
                songsPerGrouping.put(grouping, newCount);
                if (newCount > mostUsedCount) {
                    mostUsedCount = newCount;
                    mostUsedGrouping = grouping;
                } else if (newCount == mostUsedCount) {
                    mostUsedGrouping = null;
                }
            }
        }
        final List<Solution> solutions = groupings.stream()
                .filter(grouping -> grouping != null && grouping.length() > 0)
                .map(grouping -> new DifferentGroupingSolution(this, grouping))
                .collect(Collectors.toList());

        solutions.add(new DifferentGroupingInputDialogSolution(this));
        if (mostUsedGrouping != null) {
            solutions.add(new MostUsedGroupingSolution(this, mostUsedGrouping));
        }
        return solutions.toArray(new Solution[solutions.size()]);
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public Collection<Long> getSongIds() {
        final Set<Long> ids = new HashSet<>();
        final ITunesMusicLibrary library = inspector.getApplication().getiTunesMusicLibrary();
        ids.addAll(library.getSongIdsWithPropertyValue("artist", artist));
        ids.addAll(library.getSongIdsWithPropertyValue("albumArtist", artist));
        return ids;
    }

    @Override
    public Solution[] getSolutions() {
        return solutions;
    }

    @Override
    public Inspector getInspector() {
        return inspector;
    }
}
