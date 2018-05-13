/*
 * =================================================
 * Copyright 2018 tagtraum industries incorporated
 * All rights reserved.
 * =================================================
 */
package com.beatunes.discogsgenre;

import com.tagtraum.audiokern.AudioId;
import com.tagtraum.audiokern.AudioSong;
import com.tagtraum.beatunes.analysis.SongAnalysisTask;
import com.tagtraum.beatunes.analysis.Task;
import com.tagtraum.beatunes.messages.Message;
import com.tagtraum.beatunes.onlinedb.ReferenceSong;
import com.tagtraum.ubermusic.discogs.Discogs;
import com.tagtraum.ubermusic.discogs.DiscogsTrack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.Entity;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Imports genre tags from Discogs, if possible.
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */

// ============================================================================== //
// It is *essential* for this class to be annotated as Entity.                    //
// Otherwise it will not be saved in the analysis queue and cannot be processed.  //
// ============================================================================== //
@Entity
public class DiscogsGenre extends SongAnalysisTask {

    private static final Logger LOG = LoggerFactory.getLogger(DiscogsGenre.class);

    public DiscogsGenre() {
        // this task does not take long - therefore we ignore it in per task progress bars
        setProgressRelevant(false);
        // signal to beaTunes that we want it to lookup reference songs
        setUseOnlineResources(true);
    }

    /**
     * Returns a verbose description of the task in HTML format. This is shown in the
     * Analysis Options dialog (left pane).
     *
     * @return verbose HTML description.
     */
    @Override
    public String getDescription() {
        return "<h1>Discogs Genre</h1><p>Imports genre tags from <a href=\"https://www.discogs.com\">Discogs</a>. Existing values will be overwritten.</p>";
    }

    /**
     * This will be the displayed name of the analysis task.
     *
     * @return HTML string
     */
    @Override
    public String getName() {
        return "<html>Import<br>Discogs genre</html>";
    }

    /**
     * Attempt to import Discogs ids from {@link com.tagtraum.beatunes.onlinedb.OnlineDB}.
     *
     * @param task task
     */
    @Override
    public void processBefore(final Task task) {
        // referenceSong contains reference values from OnlineDB.
        final ReferenceSong referenceSong = getReferenceSong();
        if (referenceSong != null) {
            // import discogs ids to local song object
            getSong().getAlbumIds().addAll(referenceSong.getAlbumIds()
                .stream()
                .filter(id -> id.getGeneratorName().equals(AudioId.DISCOGS_MASTER_URL)
                    || id.getGeneratorName().equals(AudioId.DISCOGS_RELEASE_URL))
                .collect(Collectors.toSet()));
        }
        // make sure that we still execute the method runBefore()
        setSucceeded(false);
    }

    /**
     * This is where the actual work occurs. The method is called by beaTunes when
     * this task is processed in the analysis/task queue after the OnlineDB lookup happened.
     *
     * @param task task
     */
    @Override
    public void runBefore(final Task task) {
        final AudioSong song = getSong();
        // check whether we can skip this step altogether
        if (skip()) {
            if (LOG.isDebugEnabled()) LOG.debug("Skipped " + song);
            return;
        }
        try {
            // get discogs component
            final Discogs discogs = getApplication().getPluginManager().getImplementation(Discogs.class);
            final List<AudioSong> songs = discogs.lookup(song);
            if (!songs.isEmpty()) {
                final AudioSong firstMatch = songs.get(0);

                final DiscogsTrack discogsTrack = (DiscogsTrack) firstMatch;
                final List<String> styles = (List<String>)discogsTrack.getRelease().getDocument().get("styles");
                if (styles != null && !styles.isEmpty()) {
                    final String style = styles.get(0).trim();
                    if (LOG.isDebugEnabled()) LOG.debug("Discogs style for " + song + ": " + style);
                    song.setGenre(style);
                } else {
                    if (LOG.isDebugEnabled()) LOG.debug("Found no discogs style for " + song + ". Trying genre...");
                    if (firstMatch.getGenre() != null && !firstMatch.getGenre().trim().isEmpty()) {
                        final String discogsGenre = firstMatch.getGenre().trim();
                        if (LOG.isDebugEnabled()) LOG.debug("Discogs genre for " + song + ": " + discogsGenre);
                        song.setGenre(discogsGenre);
                    } else {
                        if (LOG.isDebugEnabled()) LOG.debug("Found no discogs genre for " + song);
                        getMessagePanel().addMessage(new Message(getApplication().localize("Analysis"),
                            "Failed to find genre for '" + song.getName() + "' on Discogs.", song.getId()));
                    }
                }
            } else {
                if (LOG.isDebugEnabled()) LOG.debug("Failed to find track on discogs: " + song);
                getMessagePanel().addMessage(new Message(getApplication().localize("Analysis"),
                    "Failed to find track '" + song.getName() + "' on Discogs.", song.getId()));
            }
        } catch (Exception e) {
            LOG.error("Failed to fetch discogs genre for " + song, e);
            final String message = e.getLocalizedMessage() == null ? e.toString() : e.getLocalizedMessage();
            getMessagePanel().addMessage(new Message(getApplication().localize("Analysis"),
                "Failed to find Discogs genre tag for '" + song.getName() + "': " + message, song.getId()));
        }
    }

}
