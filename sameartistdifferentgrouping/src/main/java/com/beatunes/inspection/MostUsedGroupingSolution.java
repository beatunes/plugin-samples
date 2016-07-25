/*
 * =================================================
 * Copyright 2016 tagtraum industries incorporated
 * All rights reserved.
 * =================================================
 */
package com.beatunes.inspection;

import com.tagtraum.beatunes.inspection.Issue;

/**
 * Most Used Grouping Solution.
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public class MostUsedGroupingSolution extends DifferentGroupingSolution {

    public MostUsedGroupingSolution(final Issue issue, final String grouping) {
        super(issue, grouping);
    }

    @Override
    public boolean isPreferred() {
        return true;
    }

    @Override
    public String getDescription() {
        return "Change the grouping of the selected songs to the grouping most songs use ('" + getGrouping() + "').";
    }

    @Override
    public String getClassDescription() {
        return "Change the grouping of the songs to the grouping most songs use.";
    }

    @Override
    public String getId() {
        return super.getId();
    }

    @Override
    public String getClassId() {
        return getClass().getName();
    }
}
