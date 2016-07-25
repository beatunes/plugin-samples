/*
 * =================================================
 * Copyright 2016 tagtraum industries incorporated
 * All rights reserved.
 * =================================================
 */
package com.beatunes.inspection;

import com.tagtraum.beatunes.BeaTunes;
import com.tagtraum.beatunes.inspection.*;
import com.tagtraum.beatunes.library.ITunesMusicLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.Collator;
import java.util.*;
import java.util.List;

/**
 * <p>
 *     Inspects a collection of songs, to see whether any songs by the same
 *     artist have more than one grouping, which could mean that the songs
 *     are in the wrong grouping.
 * </p>
 * <p>
 *     {@link BeaTunes} typically calls {@link #inspect(InspectionProgressListener)} and
 *     expected it to create and add {@link Issue}s. Issues are added by calling
 *     {@link #addIssue(Issue)}.
 *     Each found issue must offer one or more possible {@link Solution}s.
 * </p>
 * <p>
 *     This sample plugin is very similar to
 *     {@link DifferentGenreInspector}.
 * </p>
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 * @see DifferentGenreInspector
 */
public class SameArtistDifferentGroupingInspector extends Inspector {

    private static final Logger LOG = LoggerFactory.getLogger(SameArtistDifferentGroupingInspector.class);
    static volatile int id;

    public SameArtistDifferentGroupingInspector(final BeaTunes application) {
        super(application);
    }

    @Override
    public String getCategory() {
        return getApplication().localize("Consistency_issues");
    }

    @Override
    public String getPropertyName() {
        return "grouping";
    }

    @Override
    public String getName() {
        return "Same artist, different grouping";
    }

    @Override
    public String getDescription() {
        return "This inspection reports any songs that have the same artist or album artist, but different groupings.";
    }

    @Override
    public void inspect(final InspectionProgressListener inspectionProgressListener) {
        clearIssues();
        final Set<String> artists = new HashSet<>();
        final ITunesMusicLibrary library = getApplication().getiTunesMusicLibrary();
        if (LOG.isDebugEnabled()) LOG.debug("Getting all artists...");
        artists.addAll(library.getSongPropertyValues("artist", new ArrayList<>()));
        if (LOG.isDebugEnabled()) LOG.debug("Getting all albumArtists...");
        artists.addAll(library.getSongPropertyValues("albumArtist", new ArrayList<>()));
        // remove various artists as it simply does not count
        artists.remove("Various Artists");

        // re-order artists, so that the issues are ordered alphabetically
        final List<String> artistList = new ArrayList<>(artists);
        final Collator collator = Collator.getInstance();
        collator.setStrength(Collator.SECONDARY);
        Collections.sort(artistList, collator);

        int i = 0;
        for (final String artist : artistList) {
            inspectionProgressListener.progress(this, artist, i / (float) artistList.size());
            i++;
            final Set<String> groupings = new HashSet<>();
            groupings.addAll(library.getSongPropertyValues("grouping", "artist", artist));
            groupings.addAll(library.getSongPropertyValues("grouping", "albumArtist", artist));
            if (groupings.size() > 1) {
                // we have an issue: more than one grouping per artist
                addIssue(new DifferentGroupingIssue(this, artist, groupings));
            }
        }
    }

}
