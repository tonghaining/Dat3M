package com.dat3m.dartagnan.spirv.opencl.heterogeneousAtomicity;

import com.dat3m.dartagnan.configuration.Arch;
import com.dat3m.dartagnan.encoding.ProverWithTracker;
import com.dat3m.dartagnan.parsers.cat.ParserCat;
import com.dat3m.dartagnan.parsers.program.ProgramParser;
import com.dat3m.dartagnan.program.Program;
import com.dat3m.dartagnan.utils.Result;
import com.dat3m.dartagnan.verification.VerificationTask;
import com.dat3m.dartagnan.verification.solving.AssumeSolver;
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

import static com.dat3m.dartagnan.configuration.Property.CAT_SPEC;
import static com.dat3m.dartagnan.utils.ResourceHelper.getRootPath;
import static com.dat3m.dartagnan.utils.ResourceHelper.getTestResourcePath;
import static com.dat3m.dartagnan.utils.Result.FAIL;
import static com.dat3m.dartagnan.utils.Result.PASS;
import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class SpirvRacesTest {

    private final String modelPath = getRootPath("cat/opencl-paa.cat");
    private final String programPath;
    private final int bound;
    private final Result expected;

    public SpirvRacesTest(String file, int bound, Result expected) {
        this.programPath = getTestResourcePath("spirv/opencl/heterogeneousAtomicity/" + file);
        this.bound = bound;
        this.expected = expected;
    }

    @Parameterized.Parameters(name = "{index}: {0}, {1}, {2}")
    public static Iterable<Object[]> data() throws IOException {
        return Arrays.asList(new Object[][]{
                // OpenCL Source
                {"histogram-1.1.4.spvasm", 2, PASS},
                {"histogram-2.1.2.spvasm", 2, PASS},
                {"histogram-4.1.1.spvasm", 2, PASS},
                {"histogram-dv2wg.spvasm", 2, FAIL},
                {"histogram-lc2gb-1.spvasm", 2, FAIL},
                {"histogram-lc2gb-2.spvasm", 2, FAIL},

                {"compact-features-2.1.2.spvasm", 2, PASS},
                {"compact-features-lc2gb.spvasm", 2, FAIL},

                {"HeCBench-string-search-2.1.2.spvasm", 2, PASS},
                {"HeCBench-string-search-lc2gb.spvasm", 2, FAIL},

                // SYCL Source
                // CAS
                {"amrex-GpuAtomic-1.1.4.spvasm", 4, PASS},
                {"amrex-GpuAtomic-4.1.1.spvasm", 4, PASS},
                {"amrex-GpuAtomic-2.1.2.spvasm", 4, PASS},
                {"amrex-GpuAtomic-dv2wg-2.1.2.spvasm", 3, FAIL},
                {"amrex-GpuAtomic-dv2wg-4.1.2.spvasm", 3, FAIL},
                {"amrex-GpuAtomic-dv2wg-4.1.1.spvasm", 4, PASS},
                // {"amrex-GpuAtomic-gb2lc.spvasm", 4, FAIL},

                {"torch-xpu-ops-AtomicIntegerImpl-1.1.4.spvasm", 2, FAIL},
                {"torch-xpu-ops-AtomicIntegerImpl-4.1.1.spvasm", 2, FAIL},
                {"torch-xpu-ops-AtomicIntegerImpl-2.1.2.spvasm", 4, FAIL},
                {"torch-xpu-ops-AtomicIntegerImpl-fix-2.1.2.spvasm", 4, PASS},

                {"ops-lib-core-combine-max-4.1.2.spvasm", 2, PASS},
                {"ops-lib-core-combine-max-4.1.1.spvasm", 2, PASS},
                {"ops-lib-core-combine-max-2.1.2.spvasm", 4, PASS},
                {"ops-lib-core-combine-max-dv2wg-2.1.2.spvasm", 4, FAIL},
                {"ops-lib-core-combine-max-dv2wg-4.1.2.spvasm", 4, FAIL},
                {"ops-lib-core-combine-max-dv2wg-4.1.1.spvasm", 4, PASS},
                // {"ops-lib-core-combine-max-gb2lc.spvasm", 4, FAIL},
                {"ops-lib-core-combine-max-switch.spvasm", 4, PASS},

                // other atomics
                // {"oneAPI-samples-atomics_global-2.1.2.spvasm", 2, PASS},
                // {"oneAPI-samples-atomics_local-2.1.2.spvasm", 2, PASS},
        });
    }

    @Test
    public void test() throws Exception {
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
                SolverContextFactory.Solvers.YICES2);
    }

    private ProverWithTracker mkProver(SolverContext ctx) {
        return new ProverWithTracker(ctx, "", SolverContext.ProverOptions.GENERATE_MODELS);
    }

    private VerificationTask mkTask() throws Exception {
        VerificationTask.VerificationTaskBuilder builder = VerificationTask.builder()
                .withConfig(Configuration.builder().build())
                .withBound(bound)
                .withTarget(Arch.OPENCLPAA);
        Program program = new ProgramParser().parse(new File(programPath));
        Wmm mcm = new ParserCat().parse(new File(modelPath));
        return builder.build(program, mcm, EnumSet.of(CAT_SPEC));
    }
}