/*
 * =================================================
 * Copyright 2008 tagtraum industries incorporated
 * All rights reserved.
 * =================================================
 */
package com.beatunes.keytogrouping;

import com.tagtraum.beatunes.BeaTunes;
import com.tagtraum.beatunes.KeyTextRenderer;
import com.tagtraum.beatunes.analysis.TaskEditor;

import javax.swing.*;
import java.awt.*;
import java.util.prefs.Preferences;

/**
 * Provides the visual editor for the Analysis Options dialog.
 * Since this tasks does not have any options that need to be configured,
 * the "editor" is an empty, not-opaque JPanel.
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public class KeyToGroupingEditor implements TaskEditor<KeyToGrouping> {

    private static final Preferences PREFERENCES = Preferences.userNodeForPackage(KeyToGroupingEditor.class);
    private static final String ANALYSISOPTIONS_KEY_GROUPING_RENDERER = "analysisoptions.key.groupingrenderer";
    private JPanel component = new JPanel();
    private BeaTunes application;
    private JComboBox<KeyTextRenderer> keyFormatComboBox = new JComboBox<>();

    public KeyToGroupingEditor() {
        this.component.addPropertyChangeListener("enabled", evt -> {
            final Boolean newValue = (Boolean) evt.getNewValue();
            keyFormatComboBox.setEnabled(newValue);
        });
        this.component.setLayout(new FlowLayout());
        this.component.add(new JLabel("Use format: "));
        this.component.add(keyFormatComboBox);
    }

    public void setApplication(final BeaTunes beaTunes) {
        this.application = beaTunes;
        this.component.setOpaque(false);
    }

    public BeaTunes getApplication() {
        return application;
    }

    public void init() {
        final java.util.List<KeyTextRenderer> renderers = getApplication().getPluginManager().getImplementations(KeyTextRenderer.class);
        this.keyFormatComboBox.setModel(new DefaultComboBoxModel<>(
                renderers.toArray(new KeyTextRenderer[renderers.size()])
        ));
        this.keyFormatComboBox.setSelectedItem(getApplication().getGeneralPreferences().getKeyTextRenderer());
        this.keyFormatComboBox.setOpaque(false);
        this.keyFormatComboBox.setRenderer(new DefaultListCellRenderer() {
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

        final String classname = PREFERENCES.get(ANALYSISOPTIONS_KEY_GROUPING_RENDERER, getApplication().getGeneralPreferences().getKeyTextRenderer().getClass().getName());
        for (final KeyTextRenderer renderer : renderers) {
            final String rendererClassname = KeyToGrouping.getClassName(renderer);
            if (rendererClassname.equals(classname)) {
                keyFormatComboBox.setSelectedItem(renderer);
                break;
            }
        }
    }

    /**
     * This component could contain switches for settings - in this
     * case it is just an empty panel, because we don't have any
     * particular settings to deal with.
     *
     * @return an empty panel
     */
    public JComponent getComponent() {
        return component;
    }

    /**
     * Initializes this editor with the values contained in the passed task.
     *
     * @param task task
     */
    public void setTask(final KeyToGrouping task) {
        final String rendererClass = task.getRendererClass();
        for (int i = 0; i < keyFormatComboBox.getItemCount(); i++) {
            final KeyTextRenderer renderer = keyFormatComboBox.getItemAt(i);
            if (KeyToGrouping.getClassName(renderer).equals(rendererClass)) {
                keyFormatComboBox.setSelectedIndex(i);
                break;
            }
        }
    }

    /**
     * Initializes the passed task with the values set in this editor.
     *
     * @param task task to adjust
     * @return task that reflects the values set in the editor
     */
    public KeyToGrouping getTask(final KeyToGrouping task) {
        final KeyTextRenderer renderer = keyFormatComboBox.getItemAt(keyFormatComboBox.getSelectedIndex());
        final String classname = KeyToGrouping.getClassName(renderer);
        task.setRendererClass(classname);

        PREFERENCES.put(ANALYSISOPTIONS_KEY_GROUPING_RENDERER, classname);
        return task;
    }

    /**
     * Creates a new task that reflects the values set in the editor.
     *
     * @return new task with with editor values
     */
    public KeyToGrouping getTask() {
        return getTask(new KeyToGrouping());
    }
}
