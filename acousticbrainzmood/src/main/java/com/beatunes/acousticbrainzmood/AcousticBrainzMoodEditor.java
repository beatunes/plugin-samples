/*
 * =================================================
 * Copyright 2016 tagtraum industries incorporated
 * All rights reserved.
 * =================================================
 */
package com.beatunes.acousticbrainzmood;

import com.tagtraum.beatunes.BeaTunes;
import com.tagtraum.beatunes.analysis.TaskEditor;

import javax.swing.*;
import java.awt.*;
import java.util.prefs.Preferences;

/**
 * Editor for the {@link AcousticBrainzMood} analysis {@link com.tagtraum.beatunes.analysis.Task}.
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public class AcousticBrainzMoodEditor implements TaskEditor<AcousticBrainzMood> {

    private static final Preferences PREFERENCES = Preferences.userNodeForPackage(AcousticBrainzMoodEditor.class);
    private static final String ACOUSTICBRAINZ_MOOD_REPLACE_EXISTING = "analysisoptions.acousticbrainzmood.replace.existing";
    private static final String ACOUSTICBRAINZ_MOOD_EMBED_MOOD_TAGS = "analysisoptions.acousticbrainzmood.embed.moodtags";

    private final JPanel component;
    private final JCheckBox replaceExistingValuesCheckBox;
    private final JCheckBox embedMoodTagsCheckBox;

    private BeaTunes application;

    public AcousticBrainzMoodEditor() {
        this.replaceExistingValuesCheckBox = new JCheckBox();
        this.replaceExistingValuesCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        this.replaceExistingValuesCheckBox.setOpaque(false);

        this.embedMoodTagsCheckBox = new JCheckBox();
        this.embedMoodTagsCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        this.embedMoodTagsCheckBox.setOpaque(false);

        this.replaceExistingValuesCheckBox.setSelected(PREFERENCES.getBoolean(ACOUSTICBRAINZ_MOOD_REPLACE_EXISTING, false));
        this.embedMoodTagsCheckBox.setSelected(PREFERENCES.getBoolean(ACOUSTICBRAINZ_MOOD_EMBED_MOOD_TAGS, true));

        this.component = new JPanel();
        this.component.setLayout(new BoxLayout(this.component, BoxLayout.Y_AXIS));
        this.component.add(this.replaceExistingValuesCheckBox);
        this.component.add(this.embedMoodTagsCheckBox);
        this.component.setOpaque(false);

        this.component.addPropertyChangeListener("enabled", evt -> {
            this.replaceExistingValuesCheckBox.setEnabled((Boolean) evt.getNewValue());
            this.embedMoodTagsCheckBox.setEnabled((Boolean) evt.getNewValue());
        });
    }

    @Override
    public void setApplication(final BeaTunes beaTunes) {
        this.application = beaTunes;
    }

    @Override
    public BeaTunes getApplication() {
        return application;
    }

    @Override
    public void init() {
        // the localizations happen to be already defined
        this.replaceExistingValuesCheckBox.setText(application.localize("Replace_existing_value"));
        this.embedMoodTagsCheckBox.setText(application.localize("Embed_mood_related_tags"));
    }

    @Override
    public JComponent getComponent() {
        return component;
    }

    @Override
    public void setTask(final AcousticBrainzMood task) {
        this.replaceExistingValuesCheckBox.setSelected(task.isReplaceExistingValue());
        this.embedMoodTagsCheckBox.setSelected(task.isEmbedMoodTags());
    }

    @Override
    public AcousticBrainzMood getTask(final AcousticBrainzMood task) {
        task.setReplaceExistingValue(replaceExistingValuesCheckBox.isSelected());
        PREFERENCES.putBoolean(ACOUSTICBRAINZ_MOOD_REPLACE_EXISTING, task.isReplaceExistingValue());
        task.setEmbedMoodTags(embedMoodTagsCheckBox.isSelected());
        PREFERENCES.putBoolean(ACOUSTICBRAINZ_MOOD_EMBED_MOOD_TAGS, task.isEmbedMoodTags());
        return task;
    }

    @Override
    public AcousticBrainzMood getTask() {
        final AcousticBrainzMood task = new AcousticBrainzMood();
        return getTask(task);
    }
}
