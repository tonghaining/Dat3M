package com.dat3m.testInfo;

import com.dat3m.dartagnan.parsers.program.ProgramParser;
import com.dat3m.dartagnan.program.Program;

import java.io.File;

public class EventExtractor {
    private final String litmusInputPath;

    public EventExtractor(String litmusInputPath) {
        this.litmusInputPath = litmusInputPath;
    }

    // ============================================================
    public Program getProgram() throws Exception {
        File file = new File(litmusInputPath);
        return new ProgramParser().parse(file);
    }

}
