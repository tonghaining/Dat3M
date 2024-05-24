package com.dat3m.testInfo;

import com.dat3m.dartagnan.program.Program;
import com.dat3m.dartagnan.program.Thread;
import com.dat3m.dartagnan.program.event.Event;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class TagManipulator {
    protected EventExtractor extractor;

    protected TagManipulator(EventExtractor extractor) {
        this.extractor = extractor;
    }

    public List<Program> getTaggedPrograms() throws Exception {
        List<Program> taggedPrograms = new ArrayList<>();
        Program program = extractor.getProgram();
        for (Thread thread : program.getThreads()) {
            for (Event event : thread.getEvents()) {
                List<Event> candidateEvents = getCandidateEvents(event);
                    for (Event taggedEvent : candidateEvents) {
                        Program taggedProgram = extractor.getProgram();
                        taggedProgram.replaceEvent(event, taggedEvent);
                        taggedPrograms.add(taggedProgram);
                    }
            }
        }
        return taggedPrograms;
    }

    public abstract List<Event> getCandidateEvents(Event event);

    protected List<Event> getTagListMutants(Event event, List<String> tags) {
        List<Event> mutants = new ArrayList<>();
        for (String tag : tags) {
            if (event.getTags().contains(tag)) {
                for (String proxyTag : tags) {
                    if (!tag.equals(proxyTag)) {
                        Event mutant = event.getCopy();
                        mutant.removeTags(tag);
                        mutant.addTags(proxyTag);
                        mutants.add(mutant);
                    }
                }
            }
        }
        return mutants;
    }

}
