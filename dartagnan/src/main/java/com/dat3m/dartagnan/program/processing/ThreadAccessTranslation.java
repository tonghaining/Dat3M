package com.dat3m.dartagnan.program.processing;

import com.dat3m.dartagnan.expression.processing.ExprTransformer;
import com.dat3m.dartagnan.program.Program;
import com.dat3m.dartagnan.program.Thread;
import com.dat3m.dartagnan.program.event.Event;
import com.dat3m.dartagnan.program.event.RegReader;

import java.util.Set;

public class ThreadAccessTranslation implements ProgramProcessor {

    private ThreadAccessTranslation() {}

    public static ThreadAccessTranslation newInstance() {
        return new ThreadAccessTranslation();
    }

    @Override
    public void run(Program program) {
        for (Thread thread : program.getThreads()) {
            resolveThreadLocalAccesses(thread);
        }
    }

    private void resolveThreadLocalAccesses(Thread thread) {
        Set<ExprTransformer> transformers = thread.getTransformers();
        for (ExprTransformer transformer : transformers) {
            for (Event event : thread.getEvents()) {
                if (event instanceof RegReader reader) {
                    reader.transformExpressions(transformer);
                }
            }
        }
    }
}
