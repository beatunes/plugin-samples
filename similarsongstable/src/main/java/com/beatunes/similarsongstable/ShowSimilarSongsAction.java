/*
 * =================================================
 * Copyright 2017 tagtraum industries incorporated
 * All rights reserved.
 * =================================================
 */
package com.beatunes.similarsongstable;

import com.tagtraum.audiokern.AudioSong;
import com.tagtraum.audiokern.ListBackedPlayListIterator;
import com.tagtraum.audiokern.PlayListIterator;
import com.tagtraum.beatunes.BeaTunes;
import com.tagtraum.beatunes.MessageDialog;
import com.tagtraum.beatunes.SimpleSongTable;
import com.tagtraum.beatunes.action.BaseAction;
import com.tagtraum.beatunes.action.BeaTunesUIRegion;
import com.tagtraum.core.app.ActionLocation;
import com.tagtraum.core.app.RelativeActionLocation;
import com.tagtraum.core.image.ImageFX;
import com.tagtraum.core.metric.Metric;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;

/**
 * ShowSimilarSongsAction.
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public class ShowSimilarSongsAction extends BaseAction {

    private static final Metric METRIC = Metric.getMetric(ShowSimilarSongsAction.class);

    public ShowSimilarSongsAction() {
    }

    public ShowSimilarSongsAction(final BeaTunes beaTunes) {
        super(beaTunes);
    }

    @Override
    public String getId() {
        return "radio.showlist";
    }

    @Override
    public void init() {
        super.init();
        getApplication().getPlayer().addPropertyChangeListener("song", e -> {
            final PlayListIterator<AudioSong> iterator = getApplication().getPlayer().getIterator();
            // similar songs iterators are always ListBackedPlayListIterators
            setEnabled(iterator instanceof ListBackedPlayListIterator);
        });
        setEnabled(false);
    }

    @Override
    protected void loadResources() {
        super.loadResources();
        putValue(Action.NAME, "Show similar Songs List");
    }

    @Override
    public ActionLocation[] getActionLocations() {
        return new ActionLocation[] {
            // The "Control" menu used to be named "iTunes"
            new RelativeActionLocation(BeaTunesUIRegion.ITUNES_MENU, RelativeActionLocation.RelativePosition.AFTER, "radio.startselected")
        };
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        // usage counter
        METRIC.incCount();
        final ListBackedPlayListIterator<AudioSong> iterator = (ListBackedPlayListIterator<AudioSong>) getApplication().getPlayer().getIterator();
        final List<AudioSong> list = iterator.getList();
        final AudioSong seed = list.get(0);
        final SimpleSongTable songTable = new SimpleSongTable(getApplication(), list);
        songTable.init();
        // ImageFX.getScaleFactor() makes up for HiDPI issues on Windows.
        songTable.getComponent().setPreferredSize(new Dimension(600 * ImageFX.getScaleFactor(), 400 * ImageFX.getScaleFactor()));
        final MessageDialog dialog = new MessageDialog(getApplication().getMainWindow(),
            "Based on " + seed.getName() + " by " + seed.getArtist(), JOptionPane.INFORMATION_MESSAGE,
            JOptionPane.DEFAULT_OPTION, songTable.getComponent());
        dialog.setModal(false);
        dialog.setTitle("Similar Songs Playlist");
        dialog.showDialog();
    }
}
