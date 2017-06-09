/*
 * =================================================
 * Copyright 2016 tagtraum industries incorporated
 * All rights reserved.
 * =================================================
 */
package com.beatunes.inspection;

import com.tagtraum.audiokern.AudioSong;
import com.tagtraum.beatunes.MessageDialog;
import com.tagtraum.beatunes.inspection.CallableSolution;
import com.tagtraum.beatunes.songinfo.WordListSource;
import com.tagtraum.core.swing.AutoCompletion;
import com.tagtraum.core.swing.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.text.Collator;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Different Grouping InputDialog Solution.
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public class DifferentGroupingInputDialogSolution extends DifferentGroupingSolution {

    private static final Logger LOG = LoggerFactory.getLogger(DifferentGroupingInputDialogSolution.class);

    public DifferentGroupingInputDialogSolution(final DifferentGroupingIssue issue) {
        super(issue);
    }

    @Override
    public CallableSolution createCallable(final Collection<AudioSong> songs, final boolean allowUserInteraction) {
        final List<String> sortedGroupings = getSortedGroupings();
        final JComboBox<String> groupingComboBox = new JComboBox<>(sortedGroupings.toArray(new String[sortedGroupings.size()]));
        groupingComboBox.setOpaque(false);
        groupingComboBox.setRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(final JList list, final Object value, final int index,
                                                          final boolean isSelected, final boolean cellHasFocus) {
                // truncate groupings after 50 chars to keep the dialog manageable
                final Object actualValue;
                if (value instanceof String && ((String) value).length() > 50) {
                    final String s = (String) value;
                    actualValue = s.substring(0, 50) + "...";
                } else {
                    actualValue = value;
                }
                return super.getListCellRendererComponent(list, actualValue, index, isSelected, cellHasFocus);
            }
        });
        groupingComboBox.setEditable(true);
        AutoCompletion.install((JTextComponent) groupingComboBox.getEditor().getEditorComponent())
                .setWords(WordListSource.getInstance("grouping", getApplication()));
        final MessageDialog messageDialog = new MessageDialog(getApplication().getMainWindow(),
                getApplication().localize("Please select or enter a grouping"), JOptionPane.QUESTION_MESSAGE,
                JOptionPane.OK_CANCEL_OPTION, groupingComboBox);
        if (messageDialog.showDialog() == JOptionPane.CANCEL_OPTION) return null;
        // compute list before changing things...
        setGrouping((String) groupingComboBox.getSelectedItem());
        return super.createCallable(songs, true);
    }

    private List<String> getSortedGroupings() {
        List<String> groupings;
        try {
            groupings = Job.getDefaultJob().submit(() -> getApplication().getMediaLibrary().<String>getSongPropertyValues("grouping")).get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error(e.toString(), e);
            // dumb fallback
            groupings = getApplication().getMediaLibrary().getSongPropertyValues("grouping");
        }
        final Collator collator = Collator.getInstance(getApplication().getLocale());
        collator.setStrength(Collator.PRIMARY);
        groupings.sort(collator);
        if (LOG.isDebugEnabled()) LOG.debug("Groupings: " + groupings);
        return groupings;
    }

    @Override
    public String getDescription() {
        return "Choose another grouping for the selected songs.";
    }

    @Override
    public String getClassDescription() {
        return null;
    }

    @Override
    public String getClassId() {
        return null;
    }

}
