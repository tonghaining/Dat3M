package com.dat3m.testInfo;

import com.dat3m.dartagnan.program.event.Event;
import com.dat3m.dartagnan.program.event.Tag;
import com.dat3m.dartagnan.program.event.core.Store;

import java.util.ArrayList;
import java.util.List;

public class PTXTagManipulator extends TagManipulator{

    private static final List<String> PROXY_TAGS = List.of(Tag.PTX.GEN, Tag.PTX.TEX, Tag.PTX.SUR, Tag.PTX.CON);


    public PTXTagManipulator(EventExtractor extractor) {
        super(extractor);
    }

    @Override
    public List<Event> getCandidateEvents(Event event) {
        List<Event> mutants = new ArrayList<>();
        mutants.addAll(getTagListMutants(event, PROXY_TAGS));
        mutants.addAll(getTagListMutants(event, Tag.PTX.getScopeTags()));
        return mutants;
    }

    // TODO: Weak instructions

}
