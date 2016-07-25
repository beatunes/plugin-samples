/*
 * =================================================
 * Copyright 2014 tagtraum industries incorporated
 * All rights reserved.
 * =================================================
 */
package com.beatunes.keytocomment;

import com.tagtraum.beatunes.BeaTunes;
import com.tagtraum.beatunes.KeyTextRenderer;
import com.tagtraum.beatunes.analysis.TaskEditor;

import javax.swing.*;
import java.awt.*;
import java.util.prefs.Preferences;

/**
 * Configuration editor for {@link com.beatunes.keytocomment.KeyToComment} task.
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public class KeyToCommentEditor implements TaskEditor<KeyToComment> {

    private static final Preferences PREFERENCES = java.util.prefs.Preferences.userNodeForPackage(KeyToCommentEditor.class);
    private static final String ANALYSISOPTIONS_KEY_RENDERER = "analysisoptions.key.renderer";

    private BeaTunes application;
    private final JPanel component = new JPanel();
    private final JComboBox<KeyTextRenderer> keyTextRendererComboBox = new JComboBox<>();
    private final JLabel keyFormatLabel = new JLabel("Key Format:");

    public KeyToCommentEditor() {
        this.component.setLayout(new BorderLayout());
        this.component.setOpaque(false);
        this.component.add(keyFormatLabel, BorderLayout.WEST);
        this.component.add(keyTextRendererComboBox, BorderLayout.CENTER);
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
        // this localization key happens to be defined in beaTunes 4.0.4 and later
        this.keyFormatLabel.setText(application.localize("Key_Format"));
        final java.util.List<KeyTextRenderer> renderers = application.getPluginManager().getImplementations(KeyTextRenderer.class);
        this.keyTextRendererComboBox.setModel(new DefaultComboBoxModel<>(
                renderers.toArray(new KeyTextRenderer[renderers.size()])
        ));
        this.keyTextRendererComboBox.setSelectedItem(application.getGeneralPreferences().getKeyTextRenderer());
        this.keyTextRendererComboBox.setOpaque(false);
        this.keyTextRendererComboBox.setRenderer(new DefaultListCellRenderer(){
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index, final boolean isSelected, final boolean cellHasFocus) {
                final String s;
                if (value instanceof KeyTextRenderer) {
                    s = ((KeyTextRenderer) value).getName();
                } else {
                    s = "";
                }
                return super.getListCellRendererComponent(list, s, index, isSelected, cellHasFocus);
            }
        });

        final String classname = PREFERENCES.get(ANALYSISOPTIONS_KEY_RENDERER, application.getGeneralPreferences().getKeyTextRenderer().getClass().getName());
        for (final KeyTextRenderer renderer : renderers) {
            final String rendererClassname = KeyToComment.getClassName(renderer);
            if (rendererClassname.equals(classname)) {
                keyTextRendererComboBox.setSelectedItem(renderer);
                break;
            }
        }
        this.component.addPropertyChangeListener("enabled", evt -> {
            final Boolean enabled = (Boolean) evt.getNewValue();
            keyTextRendererComboBox.setEnabled(enabled);
            keyFormatLabel.setEnabled(enabled);
        });
    }

    @Override
    public JComponent getComponent() {
        return component;
    }

    @Override
    public void setTask(final KeyToComment keyToComment) {
        final String rendererClass = keyToComment.getRendererClass();
        for (int i=0; i<keyTextRendererComboBox.getItemCount(); i++) {
            final KeyTextRenderer renderer = keyTextRendererComboBox.getItemAt(i);
            if (KeyToComment.getClassName(renderer).equals(rendererClass)) {
                keyTextRendererComboBox.setSelectedIndex(i);
                break;
            }
        }
    }

    @Override
    public KeyToComment getTask(final KeyToComment keyToComment) {
        final KeyTextRenderer renderer = keyTextRendererComboBox.getItemAt(keyTextRendererComboBox.getSelectedIndex());
        final String classname = KeyToComment.getClassName(renderer);
        keyToComment.setRendererClass(classname);
        PREFERENCES.put(ANALYSISOPTIONS_KEY_RENDERER, classname);
        return keyToComment;
    }

    @Override
    public KeyToComment getTask() {
        final KeyToComment keyToComment = new KeyToComment();
        return getTask(keyToComment);
    }
}
