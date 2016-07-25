/*
 * =================================================
 * Copyright 2016 tagtraum industries incorporated
 * All rights reserved.
 * =================================================
 */
package com.beatunes.inspection;

import com.tagtraum.audiokern.AudioSong;
import com.tagtraum.beatunes.BeaTunes;
import com.tagtraum.beatunes.inspection.AsynchronousSolution;
import com.tagtraum.beatunes.inspection.Issue;
import com.tagtraum.beatunes.inspection.Solution;
import com.tagtraum.beatunes.library.LibraryDescriptor;
import com.tagtraum.beatunes.library.Song;
import com.tagtraum.beatunes.library.itunes.ITunesLibraryDescriptor;
import com.tagtraum.japlscript.JaplScript;
import com.tagtraum.japlscript.Session;
import com.tagtraum.tunes.Track;
import com.tagtraum.tunes.TunesUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Different Grouping Solution.
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public class DifferentGroupingSolution implements Solution {

    private static final Logger LOG = LoggerFactory.getLogger(DifferentGroupingSolution.class);
    private SameArtistDifferentGroupingInspector inspector;
    private final String id = getClass().getName() + "#" + SameArtistDifferentGroupingInspector.id++;
    private String grouping;
    private Issue issue;

    /**
     * @param issue issue
     */
    protected DifferentGroupingSolution(final Issue issue) {
        this.inspector = (SameArtistDifferentGroupingInspector)issue.getInspector();
        this.issue = issue;
    }

    /**
     * @param grouping grouping to set to all the songs to
     * @param issue    issue
     */
    public DifferentGroupingSolution(final Issue issue, final String grouping) {
        this.inspector = (SameArtistDifferentGroupingInspector)issue.getInspector();
        this.grouping = grouping;
        this.issue = issue;
    }

    public BeaTunes getApplication() {
        return inspector.getApplication();
    }

    /**
     * Indicated to the user whether this solution is a 'preferred' solution, i.e.
     * will most likely lead to the desired, correct result.
     * Preferred solutions may be specially marked in the UI by the application.
     *
     * @return true, if this is a preferred solution.
     */
    @Override
    public boolean isPreferred() {
        return false;
    }

    @Override
    public Issue getIssue() {
        return issue;
    }

    public String getGrouping() {
        return grouping;
    }

    public void setGrouping(final String grouping) {
        this.grouping = grouping;
    }

    /**
     * <b>NOTE: The API for this will change significantly in beaTunes 5!</b>
     */
    @Override
    public boolean solveIssue(final Collection<AudioSong> songs, final boolean allowUserInteraction) {
        if (LOG.isInfoEnabled()) LOG.info("Changing grouping to: " + grouping);
        final String grouping = getGrouping();
        // compute list before changing things...
        final List<Long> songList = createSongList(songs);
        if (!songList.isEmpty()) {
            final AsynchronousSolution asynchronousSolution = new AsynchronousSolution(
                    this, "Change grouping to '" + grouping + "'",
                    "Changing grouping to '" + grouping + "'",
                    songList.size() * 2) {

                @Override
                public Void call() throws Exception {
                    final LibraryDescriptor descriptor = inspector.getApplication().getiTunesMusicLibrary().getLibraryDescriptor();
                    if (descriptor instanceof ITunesLibraryDescriptor) {
                        for (final Long id : getSongIds()) {
                            final AudioSong song = inspector.getApplication().getiTunesMusicLibrary().getSong(id);
                            if (song != null) {
                                song.getImplementation(Song.class).setGrouping(grouping);
                                inspector.getApplication().getiTunesMusicLibrary().store(song, "grouping");
                            }
                        }
                        try {
                            TunesUtilities.invokeAndWait(() -> {
                                final Session session = JaplScript.startSession();
                                try {
                                    // persist changes
                                    final List<Track> tracks = getTracks(inspector.getApplication(), getSongIds());
                                    session.setIgnoreReturnValues(true);
                                    for (int i = 0; i < tracks.size(); i++) {
                                        final Track track = tracks.get(i);
                                        track.setGrouping(grouping);
                                        fireProgress(1f / 2f + i / (float) getSteps());
                                    }
                                    return null;
                                } finally {
                                    session.commit();
                                }
                            }, 5, TimeUnit.MINUTES);
                        } catch (ExecutionException e) {
                            if (e.getCause() instanceof Exception) throw (Exception) e.getCause();
                            if (e.getCause() instanceof Error) throw (Error) e.getCause();
                        }
                        fireProgress(1f);
                    } else {
                        for (int i = 0; i < getSongIds().size(); i++) {
                            final AudioSong song = inspector.getApplication().getiTunesMusicLibrary().getSong(getSongIds().get(i));
                            if (song != null) {
                                song.setGrouping(grouping);
                                fireProgress(i / (float) getSongIds().size());
                            }
                        }
                        fireProgress(1f);
                    }
                    return null;
                }
            };
            asynchronousSolution.setSongIds(songList);
            return inspector.getInspection().submitAsynchronousSolution(asynchronousSolution);
        }
        return true;
    }

    private List<Long> createSongList(final Collection<AudioSong> songs) {
        final List<Long> songList = new ArrayList<>();
        for (final AudioSong song : songs) {
            // only include songs we are actually changing the grouping of
            if (!grouping.equals(song.getGrouping())) {
                if (LOG.isDebugEnabled())
                    LOG.debug("Added to song list    : " + song.getName()
                            + ", grouping: " + song.getGrouping());
                songList.add(song.getId());
            } else {
                if (LOG.isDebugEnabled())
                    LOG.debug("NOT added to song list    : " + song.getName()
                            + ", grouping: " + song.getGrouping());
            }
        }
        if (LOG.isDebugEnabled()) LOG.debug("Song list: " + songList);
        return songList;
    }


    @Override
    public String getDescription() {
        return "Change the grouping of the selected songs to '" + grouping + "'.";
    }

    @Override
    public String getClassDescription() {
        return null;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getClassId() {
        return null;
    }
}
