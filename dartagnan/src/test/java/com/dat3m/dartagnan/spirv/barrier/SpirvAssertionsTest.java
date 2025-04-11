package com.dat3m.dartagnan.spirv.barrier;

import com.dat3m.dartagnan.configuration.Arch;
import com.dat3m.dartagnan.encoding.ProverWithTracker;
import com.dat3m.dartagnan.parsers.cat.ParserCat;
import com.dat3m.dartagnan.parsers.program.ProgramParser;
import com.dat3m.dartagnan.program.Program;
import com.dat3m.dartagnan.utils.Result;
import com.dat3m.dartagnan.verification.VerificationTask;
import com.dat3m.dartagnan.verification.solving.AssumeSolver;
import com.dat3m.dartagnan.verification.solving.RefinementSolver;
import com.dat3m.dartagnan.wmm.Wmm;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.BasicLogManager;
import org.sosy_lab.java_smt.SolverContextFactory;
import org.sosy_lab.java_smt.api.SolverContext;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.EnumSet;

import static com.dat3m.dartagnan.configuration.Arch.OPENCL;
import static com.dat3m.dartagnan.configuration.Arch.VULKAN;
import static com.dat3m.dartagnan.configuration.Property.PROGRAM_SPEC;
import static com.dat3m.dartagnan.utils.ResourceHelper.getRootPath;
import static com.dat3m.dartagnan.utils.ResourceHelper.getTestResourcePath;
import static com.dat3m.dartagnan.utils.Result.*;
import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class SpirvAssertionsTest {

    private final String vulkanModelPath = getRootPath("cat/spirv.cat");
    private final String openCLModelPath = getRootPath("cat/opencl.cat");
    private final Arch model;
    private final String programPath;
    private final int bound;
    private final Result expected;

    public SpirvAssertionsTest(String file, Arch model, int bound, Result expected) {
        this.programPath = getTestResourcePath("spirv/barrier/" + file);
        this.model = model;
        this.bound = bound;
        this.expected = expected;
    }

    @Parameterized.Parameters(name = "{index}: {0}, {1}, {2}, {3}")
    public static Iterable<Object[]> data() throws IOException {
        return Arrays.asList(new Object[][]{
                {"barrier-loop-1-forall.spv.dis", VULKAN, 3, PASS},
                {"barrier-loop-1-exists.spv.dis", VULKAN, 3, PASS},
                {"barrier-no-loop-1-forall.spv.dis", VULKAN, 1, PASS},
                {"barrier-no-loop-1-exists.spv.dis", VULKAN, 1, PASS},

                {"barrier-loop-2-forall.spv.dis", VULKAN, 2, PASS},
                {"barrier-loop-2-exists.spv.dis", VULKAN, 2, PASS},
                {"barrier-no-loop-2-forall.spv.dis", VULKAN, 1, PASS},
                {"barrier-no-loop-2-exists.spv.dis", VULKAN, 1, PASS},

                {"barrier-loop-3-forall.spv.dis", VULKAN, 2, PASS},
                {"barrier-loop-3-exists.spv.dis", VULKAN, 2, PASS},
                {"barrier-no-loop-3-forall.spv.dis", VULKAN, 1, PASS},
                {"barrier-no-loop-3-exists.spv.dis", VULKAN, 1, PASS},

                {"barrier-loop-4-forall.spv.dis", VULKAN, 2, PASS},
                {"barrier-loop-4-exists.spv.dis", VULKAN, 2, FAIL},
                {"barrier-no-loop-4-forall.spv.dis", VULKAN, 1, PASS},
                {"barrier-no-loop-4-exists.spv.dis", VULKAN, 1, FAIL},

                {"barrier-loop-5-forall.spv.dis", VULKAN, 2, FAIL},
                {"barrier-loop-5-exists.spv.dis", VULKAN, 2, PASS},
                {"barrier-no-loop-5-forall.spv.dis", VULKAN, 1, FAIL},
                {"barrier-no-loop-5-exists.spv.dis", VULKAN, 1, PASS},

                {"barrier-loop-6-forall.spv.dis", VULKAN, 2, FAIL},
                {"barrier-loop-6-exists.spv.dis", VULKAN, 2, PASS},
                {"barrier-no-loop-6-forall.spv.dis", VULKAN, 1, FAIL},
                {"barrier-no-loop-6-exists.spv.dis", VULKAN, 1, PASS},

                {"barrier-inlining-1-forall-correct.spv.dis", OPENCL, 1, PASS},
                {"barrier-inlining-1-exists-correct.spv.dis", OPENCL, 1, PASS},
                {"barrier-inlining-1-forall-wrong.spv.dis", OPENCL, 1, FAIL},
                {"barrier-inlining-1-exists-wrong.spv.dis", OPENCL, 1, FAIL},

                {"barrier-no-inlining-1-forall-correct.spv.dis", OPENCL, 1, PASS},
                {"barrier-no-inlining-1-exists-correct.spv.dis", OPENCL, 1, PASS},
                {"barrier-no-inlining-1-forall-wrong.spv.dis", OPENCL, 1, FAIL},
                {"barrier-no-inlining-1-exists-wrong.spv.dis", OPENCL, 1, FAIL},
        });
    }

    @Test
    public void testAllSolvers() throws Exception {
        try (SolverContext ctx = mkCtx(); ProverWithTracker prover = mkProver(ctx)) {
             assertEquals(expected, RefinementSolver.run(ctx, prover, mkTask()).getResult());
        }
        try (SolverContext ctx = mkCtx(); ProverWithTracker prover = mkProver(ctx)) {
            assertEquals(expected, AssumeSolver.run(ctx, prover, mkTask()).getResult());
        }
    }

    private SolverContext mkCtx() throws InvalidConfigurationException {
        Configuration cfg = Configuration.builder().build();
        return SolverContextFactory.createSolverContext(
                cfg,
                BasicLogManager.create(cfg),
                ShutdownManager.create().getNotifier(),
                SolverContextFactory.Solvers.Z3);
    }

    private ProverWithTracker mkProver(SolverContext ctx) {
        return new ProverWithTracker(ctx, "", SolverContext.ProverOptions.GENERATE_MODELS);
    }

    private VerificationTask mkTask() throws Exception {
        VerificationTask.VerificationTaskBuilder builder = VerificationTask.builder()
                .withConfig(Configuration.builder().build())
                .withBound(bound)
                .withTarget(model);
        Program program = new ProgramParser().parse(new File(programPath));
        Wmm mcm = model == VULKAN ?
                new ParserCat().parse(new File(vulkanModelPath)) :
                new ParserCat().parse(new File(openCLModelPath));
        return builder.build(program, mcm, EnumSet.of(PROGRAM_SPEC));
    }
}
