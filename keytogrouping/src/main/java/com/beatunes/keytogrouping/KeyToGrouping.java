/*
 * =================================================
 * Copyright 2008 tagtraum industries incorporated
 * All rights reserved.
 * =================================================
 */
package com.beatunes.keytogrouping;

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
 * Copies key info to the grouping field following id3 TKEY conventions.
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */

// ============================================================================== //
// It is *essential* for this class to be annotated as Entity.                    //
// Otherwise it will not be saved in the analysis queue and cannot be processed.  //
// ============================================================================== //
@Entity
public class KeyToGrouping extends SongAnalysisTask {

    private static final Logger LOG = LoggerFactory.getLogger(KeyToGrouping.class);
    private static final String KEY_START_MARKER = "KEY:";
    private static final String KEY_END_MARKER = ";";
    private static final String GROUPING_RENDERER = "grouping.renderer";


    public KeyToGrouping() {
        // this task does not take long - therefore we ignore it in per task progress bars
        setProgressRelevant(false);
    }

    /**
     * Returns a verbose description of the task in HTML format.
     *
     * @return verbose HTML description.
     */
    @Override
    public String getDescription() {
        return "<h1>Key To Grouping</h1><p>Copies the tonal key (if it exists) to the grouping field using the configured format.</p>";
    }

    /**
     * This will be the displayed name of the analysis task.
     *
     * @return HTML string
     */
    @Override
    public String getName() {
        return "<html>Copy key to<br>grouping field</html>";
    }

    /**
     * This is where the actual work occurs.
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
        // get the new grouping
        final String newGrouping = getNewGrouping(song);
        if (LOG.isDebugEnabled()) LOG.debug("Setting new grouping to: " + newGrouping);
        // store the change persistently
        song.setGrouping(newGrouping);
    }

    /**
     * Indicates, whether this task can be skipped.
     *
     * @return true or false
     */
    @Override
    public boolean skip() {
        return false;
    }

    private String getNewGrouping(final AudioSong song) {
        String grouping = song.getGrouping() == null ? "" : song.getGrouping();
        if (hasGroupingKey(grouping)) {
            grouping = removeGroupingKey(grouping);
        }
        if (song.getKey() != null) {
            grouping = addGroupingKey(grouping, song.getKey());
        }
        return grouping;
    }

    private boolean hasGroupingKey(final String grouping) {
        final boolean hasKey;
        if (grouping == null || grouping.length() < KEY_START_MARKER.length() + KEY_END_MARKER.length()) {
            hasKey = false;
        } else {
            final int start = grouping.indexOf(KEY_START_MARKER);
            if (start == -1) {
                hasKey = false;
            } else {
                final int end = grouping.indexOf(KEY_END_MARKER, start);
                hasKey = end != -1;
            }
        }
        return hasKey;
    }

    private String removeGroupingKey(final String grouping) {
        final int start = grouping.indexOf(KEY_START_MARKER);
        final int end = grouping.indexOf(KEY_END_MARKER, start);
        if (grouping.length() > end) return grouping.substring(0, start) + grouping.substring(end+1);
        return grouping.substring(0, start);
    }

    private String addGroupingKey(final String grouping, final Key key) {
        final String keyString = getRenderer().toKeyString(key);
        return grouping + KEY_START_MARKER + keyString + KEY_END_MARKER;
    }

    public void setRendererClass(final String klass) {
        setProperty(GROUPING_RENDERER, klass);
    }

    public String getRendererClass() {
        final String renderer = getProperty(GROUPING_RENDERER);
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
     * Ruby and Python object's classnames are not the same after the JVM exists.
     * Therefore we have to get their type's name, which is persistent.
     *
     * @param renderer renderer
     * @return classname
     */
    public static String getClassName(final KeyTextRenderer renderer) {
        // TODO: move this somewhere else?
        final String classname;
        if (renderer instanceof RubyObject) {
            classname = "__jruby." + ((RubyObject) renderer).getMetaClass().getName();
        } else if (renderer instanceof PyProxy) {
            classname = "__jython." + ((PyProxy) renderer)._getPyInstance().getType().getName();
        } else {
            classname = renderer.getClass().getName();
        }
        return classname;
    }
}
