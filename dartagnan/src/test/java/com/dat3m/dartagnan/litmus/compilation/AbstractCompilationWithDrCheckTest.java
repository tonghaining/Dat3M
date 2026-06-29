package com.dat3m.dartagnan.litmus.compilation;

import com.dat3m.dartagnan.configuration.ProgressModel;
import com.dat3m.dartagnan.program.Program;
import com.dat3m.dartagnan.program.event.Event;
import com.dat3m.dartagnan.program.event.Tag;
import com.dat3m.dartagnan.utils.rules.Provider;
import com.dat3m.dartagnan.utils.rules.Providers;
import com.dat3m.dartagnan.verification.TaskSolver;
import com.dat3m.dartagnan.verification.VerificationTask;
import com.dat3m.dartagnan.wmm.Wmm;
import org.junit.Test;

import java.util.EnumSet;
import java.util.stream.Stream;

import static com.dat3m.dartagnan.configuration.Property.CAT_SPEC;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public abstract class AbstractCompilationWithDrCheckTest extends AbstractCompilationTest {

    AbstractCompilationWithDrCheckTest(String path) {
        super(path);
    }

    protected abstract Provider<String> getTargetFilePathProvider();

    protected final Provider<Program> drCheckProgramProvider = Providers.createProgramFromPath(getTargetFilePathProvider());
    protected final Provider<Wmm> drCheckWmmProvider = getTargetWmmProvider();
    protected final Provider<VerificationTask> drCheckTaskProvider = Providers.createTask(
            drCheckProgramProvider, drCheckWmmProvider,
            () -> EnumSet.of(CAT_SPEC),
            targetProvider, ProgressModel::defaultHierarchy, () -> 1, configProvider);

    protected final Provider<Program> targetProgramProvider = Providers.createProgramFromPath(getTargetFilePathProvider());
    protected final Provider<Wmm> targetWmmProvider = getTargetWmmProvider();
    protected final Provider<VerificationTask> targetTask2Provider = Providers.createTask(
            targetProgramProvider, targetWmmProvider,
            propertyProvider, targetProvider, ProgressModel::defaultHierarchy, () -> 1, configProvider);

    {
        ruleChain = ruleChain
                .around(drCheckProgramProvider)
                .around(drCheckWmmProvider)
                .around(drCheckTaskProvider)
                .around(targetProgramProvider)
                .around(targetWmmProvider)
                .around(targetTask2Provider);
    }

    @Test
    @Override
    public void testAssume() throws Exception {
        if (!isCompilableToHardware(program1Provider.get())) {
            return;
        }

        try (TaskSolver s1 = TaskSolver.create(task1Provider.get()).withShutdownManager(shutdownManagerProvider.get());
             TaskSolver drChecker = TaskSolver.create(drCheckTaskProvider.get()).withShutdownManager(shutdownManagerProvider.get());
             TaskSolver s2 = TaskSolver.create(targetTask2Provider.get()).withShutdownManager(shutdownManagerProvider.get())) {

            s1.run();
            if (!s1.hasModel()) {
                drChecker.run();
                if (drChecker.hasModel()) {
                    fail("Target program has a data race under the target memory model");
                }
                boolean compilationIsBroken = getCompilationBreakers().contains(filePathProvider.get());
                s2.run();
                assertEquals(compilationIsBroken, s2.hasModel());
            }
        }
    }

    private static boolean isCompilableToHardware(Program program) {
        return program.getThreadEvents().stream().noneMatch(AbstractCompilationWithDrCheckTest::isRcuOrSrcu);
    }

    private static boolean isRcuOrSrcu(Event e) {
        return Stream.of(Tag.Linux.RCU_LOCK, Tag.Linux.RCU_UNLOCK, Tag.Linux.RCU_SYNC,
                Tag.Linux.SRCU_LOCK, Tag.Linux.SRCU_UNLOCK, Tag.Linux.SRCU_SYNC,
                Tag.Linux.AFTER_SRCU_READ_UNLOCK
        ).anyMatch(e::hasTag);
    }
}
