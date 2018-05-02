/*
 * =================================================
 * Copyright 2018 tagtraum industries incorporated
 * All rights reserved.
 * =================================================
 */
package com.beatunes.bandcamptags;

import com.tagtraum.audiokern.AudioId;
import com.tagtraum.audiokern.AudioSong;
import com.tagtraum.beatunes.analysis.SongAnalysisTask;
import com.tagtraum.beatunes.analysis.Task;
import com.tagtraum.beatunes.messages.Message;
import com.tagtraum.beatunes.onlinedb.ReferenceSong;
import com.tagtraum.ubermusic.Tag;
import com.tagtraum.ubermusic.bandcamp.Bandcamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.Entity;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Copies tags from <a href="https://bandcamp.com">bandcamp</a> to the
 * tags field.
 * At some point this functionality will probably be included in beaTunes itself.
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */

// ============================================================================== //
// It is *essential* for this class to be annotated as Entity.                    //
// Otherwise it will not be saved in the analysis queue and cannot be processed.  //
// ============================================================================== //
@Entity
public class BandcampTags extends SongAnalysisTask {

    private static final Logger LOG = LoggerFactory.getLogger(BandcampTags.class);

    public BandcampTags() {
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
        return "<h1>Bandcamp Tags</h1><p>Imports tags from <a href=\"https://bandcamp.com\">bandcamp</a> into the tags field.</p>";
    }

    /**
     * This will be the displayed name of the analysis task.
     *
     * @return HTML string
     */
    @Override
    public String getName() {
        return "<html>Import<br>bandcamp tags</html>";
    }

    /**
     * Attempt to import Bandcamp ids from {@link com.tagtraum.beatunes.onlinedb.OnlineDB}.
     *
     * @param task task
     */
    @Override
    public void processBefore(final Task task) {
        // referenceSong contains reference values from OnlineDB.
        final ReferenceSong referenceSong = getReferenceSong();
        // import Bandcamp ids to local song object
        getSong().getTrackIds().addAll(referenceSong.getTrackIds()
            .stream()
            .filter(id -> id.getGeneratorName().equals(AudioId.BANDCAMP_TRACK_URL))
            .collect(Collectors.toSet()));
        getSong().getAlbumIds().addAll(referenceSong.getAlbumIds()
            .stream()
            .filter(id -> id.getGeneratorName().equals(AudioId.BANDCAMP_ALBUM_URL))
            .collect(Collectors.toSet()));
        getSong().getArtistIds().addAll(referenceSong.getArtistIds()
            .stream()
            .filter(id -> id.getGeneratorName().equals(AudioId.BANDCAMP_ARTIST_URL))
            .collect(Collectors.toSet()));
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
            // get bandcamp component
            final Bandcamp bandcamp = getApplication().getPluginManager().getImplementation(Bandcamp.class);
            final List<Tag> tags = bandcamp.getTags(song);
            // convert tag list to string list
            final Set<String> stringTags = tags.stream().map(Tag::getName).collect(Collectors.toSet());
            if (!stringTags.isEmpty()) {
                if (LOG.isDebugEnabled()) LOG.debug("Bandcamp tags for " + song + ": " + stringTags);
                song.getTags().addAll(stringTags);
            } else {
                if (LOG.isDebugEnabled()) LOG.debug("Found no bandcamp tags for " + song + ".");
            }
        } catch (Exception e) {
            LOG.error("Failed to fetch bandcamp tags for " + song, e);

            // both "Analysis" and "Failed_to_fetch_tags_for_X_Y" happen
            // to be strings that are already localized in beaTunes
            final String message = e.getLocalizedMessage() == null ? e.toString() : e.getLocalizedMessage();
            getMessagePanel().addMessage(new Message(getApplication().localize("Analysis"),
                getApplication().localize("Failed_to_fetch_tags_for_X_Y", song.getName(), message), song.getId()));
        }
    }

    /**
     * Indicates, whether this task can be skipped.
     *
     * @return {@code true} or {@code false}
     */
    @Override
    public boolean skip() {
        final AudioSong song = getSong();
        boolean bandcampId = song.getArtistIds().stream().anyMatch(id -> id.getGeneratorName().equals(AudioId.BANDCAMP_ARTIST_URL));
        bandcampId |= song.getAlbumIds().stream().anyMatch(id -> id.getGeneratorName().equals(AudioId.BANDCAMP_ALBUM_URL));
        bandcampId |= song.getTrackIds().stream().anyMatch(id -> id.getGeneratorName().equals(AudioId.BANDCAMP_TRACK_URL));
        final String comments = song.getComments();
        // bandcamp songs usually have a comment like this:
        // "Visit https://sebo-k.bandcamp.com"
        final boolean visitBandcamp = comments != null
            && comments.startsWith(AudioId.BANDCAMP_ARTIST_URL_PREFIX)
            && comments.endsWith(".bandcamp.com");
        // we need some sort of artist URL as id.
        return !(bandcampId || visitBandcamp);
    }
}
