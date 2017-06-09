/*
 * =================================================
 * Copyright 2015 tagtraum industries incorporated
 * All rights reserved.
 * =================================================
 */
package com.beatunes.acousticbrainzmood;

import com.tagtraum.audiokern.AudioId;
import com.tagtraum.audiokern.AudioSong;
import com.tagtraum.audiokern.mood.Mood;
import com.tagtraum.beatunes.action.standard.EmbedSpecialFieldsAction;
import com.tagtraum.beatunes.analysis.AnalysisException;
import com.tagtraum.beatunes.analysis.SongAnalysisTask;
import com.tagtraum.beatunes.analysis.Task;
import com.tagtraum.beatunes.messages.Message;
import com.tagtraum.ubermusic.acousticbrainz.AcousticBrainz;
import com.tagtraum.ubermusic.acousticbrainz.AcousticBrainzSong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.Entity;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;

/**
 * AcousticBrainz-based mood estimation.
 * As {@link com.tagtraum.beatunes.analysis.TaskEditor}, {@link AcousticBrainzMoodEditor} is used.
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 * @see <a href="https://acousticbrainz.org">AcousticBrainz.org</a>
 */
// ============================================================================== //
// It is *essential* for this class to be annotated as Entity.                    //
// Otherwise it will not be saved in the analysis queue and cannot be processed.  //
// ============================================================================== //
@Entity
public class AcousticBrainzMood extends SongAnalysisTask {

    private static final Logger LOG = LoggerFactory.getLogger(AcousticBrainzMood.class);
    private static final boolean DEFAULT_EMBED_MOOD_TAGS = true;
    private static final String EMBED_MOOD_TAGS = "embedMoodTags";

    private boolean replaceExistingValue;

    public AcousticBrainzMood() {
        setProgressRelevant(false);
    }

    @Override
    public String getDescription() {
        return "<h1>Import Moods</h1><p>Attempts to derive mood values based in <a href=\"http://AcousticBrainz.org\">AcousticBrainz.org</a> data. Requires MusicBrainz ids for this to work.</p>";
    }

    @Override
    public String getName() {
        return "<html>Derive mood from<br>AcousticBrainz</html>";
    }

    public boolean isReplaceExistingValue() {
        return replaceExistingValue;
    }

    public void setReplaceExistingValue(final boolean replaceExistingValue) {
        this.replaceExistingValue = replaceExistingValue;
    }

    public boolean isEmbedMoodTags() {
        final String s = getProperty(EMBED_MOOD_TAGS);
        if (s != null) {
            try {
                return Boolean.valueOf(s);
            } catch (Exception e) {
                LOG.error(e.toString(), e);
            }
        }
        return DEFAULT_EMBED_MOOD_TAGS;
    }

    /**
     * Persistently stores, whether we should embed mood tags or not in this task's properties.
     *
     * @param embedMoodTags {@code true} or {@code false}
     */
    public void setEmbedMoodTags(final boolean embedMoodTags) {
        setProperty(EMBED_MOOD_TAGS, Boolean.toString(embedMoodTags));
    }

    @Override
    public void runBefore(final Task task) throws AnalysisException {
        final AudioSong song = getSong();
        if (skip()) {
            if (LOG.isDebugEnabled()) LOG.debug("Skipped " + getSong() + " because the mood is already set.");
            return;
        }
        try {
            final AcousticBrainz acousticBrainz = getApplication().getPluginManager().getImplementation(AcousticBrainz.class);
            final List<AudioSong> results = acousticBrainz.lookup(song);
            if (!results.isEmpty()) {
                final AcousticBrainzSong result = (AcousticBrainzSong)results.get(0);
                final Mood mood = result.getMood();
                if (LOG.isDebugEnabled()) LOG.debug("Found " + result + " with " + mood + " for " + song);
                song.setMood(mood);
                song.setMoodAlgorithm(song.getMoodAlgorithm());
                // !!! the toMoodKeywords()-method will move somewhere else in beaTunes5
                if (isEmbedMoodTags()) {
                    song.setMoodKeywords(new HashSet<>(EmbedSpecialFieldsAction.toMoodKeywords(getApplication(), song.getMood())));
                }
            } else {
                if (LOG.isDebugEnabled()) LOG.debug("Lookup failed for " + song);
                getMessagePanel().addMessage(
                        new Message(getApplication().localize("Analysis"),
                                "Failed to find data on AcousticBrainz.org for '" + song.getName() + "' (" + getTrackMbid(song) + ").")
                );
            }
        } catch (FileNotFoundException e) {
            getMessagePanel().addMessage(
                    new Message(getApplication().localize("Analysis"),
                    "Failed to find data on AcousticBrainz.org for '" + song.getName() + "' (" + getTrackMbid(song) + ").")
            );
            LOG.info(e.toString(), e);
        } catch (IOException e) {
            LOG.error(e.toString(), e);
            throw new AnalysisException(e);
        }
    }

    @Override
    public boolean skip() {
        final AudioSong s = getSong();
        // if we have the mood already and don't want to replace it, don't bother
        return s.getMood() != null && !replaceExistingValue;
    }

    @Override
    public Task createDeepCopy() {
        final AcousticBrainzMood copy = new AcousticBrainzMood();
        copy.replaceExistingValue = replaceExistingValue;
        copy.setEmbedMoodTags(isEmbedMoodTags());
        copy.setUseOnlineResources(isUseOnlineResources());
        for (final Task subTask:getTasks()) {
            copy.add(subTask.createDeepCopy());
        }
        return copy;
    }

    /**
     * Extract MusicBrainz id from song object.
     *
     * @param song song
     * @return id
     */
    private String getTrackMbid(final AudioSong song) {
        return song.getTrackIds().stream()
            .filter(id -> AudioId.MUSIC_BRAINZ_TRACK.equals(id.getGeneratorName()))
            .map(AudioId::getId)
            .findFirst()
            .orElse("mbid_unavailable");
    }



}
