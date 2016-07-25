/*
 * =================================================
 * Copyright 2009 tagtraum industries incorporated
 * All rights reserved.
 * =================================================
 */
package com.beatunes.keytocomment;

import com.tagtraum.audiokern.AudioSong;
import com.tagtraum.audiokern.key.Key;
import com.tagtraum.beatunes.KeyTextRenderer;
import com.tagtraum.beatunes.analysis.AnalysisException;
import com.tagtraum.beatunes.analysis.SongAnalysisTask;
import com.tagtraum.beatunes.analysis.Task;
import com.tagtraum.beatunes.keyrenderer.DefaultKeyTextRenderer;
import org.jruby.RubyObject;
import org.python.core.PyProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.Entity;
import java.util.List;

/**
 * Copies tonal key info to the comments field using the configured renderer.
 * Note that this functionality is already built into beaTunes
 * (starting with <a href="http://blog.beatunes.com/2015/08/looking-good-beatunes-45.html">version 4.5</a>).
 * This plugin therefore only serves demo purposes.
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */

// ============================================================================== //
// It is *essential* for this class to be annotated as Entity.                    //
// Otherwise it will not be saved in the analysis queue and cannot be processed.  //
// ============================================================================== //
@Entity
public class KeyToComment extends SongAnalysisTask {

    private static final Logger LOG = LoggerFactory.getLogger(KeyToComment.class);
    private static final String KEY_START_MARKER = "KEY:";
    private static final String KEY_END_MARKER = ";";


    public KeyToComment() {
        // this task does not take long - therefore we ignore it in per task progress bars
        setProgressRelevant(false);
    }

    public void setRendererClass(final String klass) {
        setProperty("renderer", klass);
    }

    public String getRendererClass() {
        final String renderer = getProperty("renderer");
        return renderer == null ? DefaultKeyTextRenderer.class.getName() : renderer;
    }

    public KeyTextRenderer getRenderer() {
        final String desiredRenderer = getRendererClass();
        final List<KeyTextRenderer> renderers = getApplication().getPluginManager().getImplementations(KeyTextRenderer.class);
        for (final KeyTextRenderer renderer : renderers) {
            final String rendererClass = getClassName(renderer);
            if (rendererClass.equals(desiredRenderer)) return renderer;
        }
        // default to DefaultKeyTextRenderer
        return getApplication().getPluginManager().getImplementation(DefaultKeyTextRenderer.class);
    }

    /**
     * Returns a verbose description of the task in HTML format. This is shown in the
     * Analysis Options dialog (left pane).
     *
     * @return verbose HTML description.
     */
    @Override
    public String getDescription() {
        return "<h1>Key To Comment</h1><p>Copies the tonal key (if it exists) to the comment field using the configured format.</p>";
    }

    /**
     * This will be the displayed name of the analysis task.
     *
     * @return HTML string
     */
    @Override
    public String getName() {
        return "<html>Copy key to<br>comment field</html>";
    }

    /**
     * This is where the actual work occurs. This method is called by beaTunes when
     * this task is processed in the analysis/task queue.
     *
     * @throws AnalysisException if something goes wrong.
     */
    @Override
    public void runBefore(final Task task) throws AnalysisException {
        // check whether we can skip this step altogether
        if (skip()) {
            if (LOG.isDebugEnabled()) LOG.debug("Skipped " + getSong());
            return;
        }
        // get the song object
        final AudioSong song = getSong();
        // get the new comment
        final String newComments = getNewComments(song);
        if (LOG.isDebugEnabled()) LOG.debug("Setting new comments to: " + newComments);
        // store new comment - the new value is automatically persisted and the UI is updated.
        song.setComments(newComments);
    }

    /**
     * Indicates, whether this task can be skipped.
     *
     * @return true or false
     */
    @Override
    public boolean skip() {
        final AudioSong song = getSong();
        final String comments = song.getComments();
        final String commentsKey = getKey(comments);
        final String renderedKey = getRenderer().toKeyString(song.getKey());
        final boolean skip = commentsKey != null && commentsKey.equals(renderedKey);
        if (LOG.isDebugEnabled()) LOG.debug("Skipping " + song + " ...");
        return skip;
    }

    /**
     * Creates a new comment string.
     *
     * @param song song
     * @return new comment (with key, if the song has a key)
     */
    private String getNewComments(final AudioSong song) {
        String comments = song.getComments() == null ? "" : song.getComments();
        if (hasCommentsKey(comments)) {
            comments = removeCommentsKey(comments);
        }
        if (song.getKey() != null) {
            comments = addCommentsKey(comments, song.getKey());
        }
        return comments;
    }

    /**
     * Indicates whether this comment contains a key.
     *
     * @param comments comment
     * @return true, if the comment contains a key
     */
    private boolean hasCommentsKey(final String comments) {
        return getKey(comments) != null;
    }

    /**
     * Extracts a key out of a comment string.
     *
     * @param comments comment
     * @return key or <code>null</code>, if not found
     */
    private String getKey(final String comments) {
        final String keyString;
        if (comments == null || comments.length() < KEY_START_MARKER.length() + KEY_END_MARKER.length()) {
            keyString = null;
        } else {
            final int start = comments.indexOf(KEY_START_MARKER);
            if (start == -1) {
                keyString = null;
            } else {
                final int end = comments.indexOf(KEY_END_MARKER, start);
                if (end == -1) {
                    keyString = null;
                } else {
                    keyString = comments.substring(start + KEY_START_MARKER.length(), end);
                    //keyString = KeyFactory.parseTKEY(key);
                }
            }
        }
        return keyString;
    }

    /**
     * Removes a key from a comment string.
     *
     * @param comments comment
     * @return comment without the key
     */
    private String removeCommentsKey(final String comments) {
        final int start = comments.indexOf(KEY_START_MARKER);
        final int end = comments.indexOf(KEY_END_MARKER, start);
        if (comments.length() > end) return comments.substring(0, start) + comments.substring(end+1);
        return comments.substring(0, start);
    }

    /**
     * Adds a key to a comment.
     *
     * @param comments comment
     * @param key key
     * @return new comment with key
     */
    private String addCommentsKey(final String comments, final Key key) {
        return comments + KEY_START_MARKER + getRenderer().toKeyString(key) + KEY_END_MARKER;
    }

    /**
     * Ruby and Python object's classnames are not the same after the JVM exists.
     * Therefore we have to get their type's name, which is persistent.
     *
     * @param renderer renderer
     * @return classname
     */
    public static String getClassName(final KeyTextRenderer renderer) {
        final String classname;
        if (renderer instanceof RubyObject) {
            classname = "__jruby." + ((RubyObject)renderer).getMetaClass().getName();
        } else if (renderer instanceof PyProxy) {
            classname = "__jython." + ((PyProxy)renderer)._getPyInstance().getType().getName();
        } else {
            classname = renderer.getClass().getName();
        }
        return classname;
    }

}
