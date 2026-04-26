package com.dat3m.dartagnan.spirv.opencl.paa;

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
        this.programPath = getTestResourcePath("spirv/opencl/" + file);
        this.bound = bound;
        this.expected = expected;
    }

    @Parameterized.Parameters(name = "{index}: {0}, {1}, {2}")
    public static Iterable<Object[]> data() throws IOException {
        return Arrays.asList(new Object[][]{
                {"heterogeneousAtomicity/histogram-1.1.4.spvasm", 2, PASS},
                {"heterogeneousAtomicity/histogram-2.1.2.spvasm", 2, PASS},
                {"heterogeneousAtomicity/histogram-4.1.1.spvasm", 2, PASS},
                {"heterogeneousAtomicity/histogram-dv2wg.spvasm", 2, FAIL},
                {"heterogeneousAtomicity/histogram-lc2gb-1.spvasm", 2, FAIL},
                {"heterogeneousAtomicity/histogram-lc2gb-2.spvasm", 2, FAIL},
                {"heterogeneousAtomicity/compact-features-2.1.2.spvasm", 2, PASS},
                {"heterogeneousAtomicity/compact-features-lc2gb.spvasm", 2, FAIL},
                {"heterogeneousAtomicity/HeCBench-string-search-2.1.2.spvasm", 2, PASS},
                {"heterogeneousAtomicity/HeCBench-string-search-lc2gb.spvasm", 2, FAIL},
                {"heterogeneousAtomicity/amrex-GpuAtomic-1.1.4.spvasm", 4, PASS},
                {"heterogeneousAtomicity/amrex-GpuAtomic-4.1.1.spvasm", 4, PASS},
                {"heterogeneousAtomicity/amrex-GpuAtomic-2.1.2.spvasm", 4, PASS},
                {"heterogeneousAtomicity/amrex-GpuAtomic-dv2wg-2.1.2.spvasm", 3, FAIL},
                {"heterogeneousAtomicity/amrex-GpuAtomic-dv2wg-4.1.2.spvasm", 3, FAIL},
                {"heterogeneousAtomicity/amrex-GpuAtomic-dv2wg-4.1.1.spvasm", 4, PASS},
                {"heterogeneousAtomicity/torch-xpu-ops-AtomicIntegerImpl-1.1.4.spvasm", 2, FAIL},
                {"heterogeneousAtomicity/torch-xpu-ops-AtomicIntegerImpl-4.1.1.spvasm", 2, FAIL},
                {"heterogeneousAtomicity/torch-xpu-ops-AtomicIntegerImpl-2.1.2.spvasm", 4, FAIL},
                {"heterogeneousAtomicity/torch-xpu-ops-AtomicIntegerImpl-fix-2.1.2.spvasm", 4, PASS},
                {"heterogeneousAtomicity/ops-lib-core-combine-max-4.1.2.spvasm", 2, PASS},
                {"heterogeneousAtomicity/ops-lib-core-combine-max-4.1.1.spvasm", 2, PASS},
                {"heterogeneousAtomicity/ops-lib-core-combine-max-2.1.2.spvasm", 4, PASS},
                {"heterogeneousAtomicity/ops-lib-core-combine-max-dv2wg-2.1.2.spvasm", 4, FAIL},
                {"heterogeneousAtomicity/ops-lib-core-combine-max-dv2wg-4.1.2.spvasm", 4, FAIL},
                {"heterogeneousAtomicity/ops-lib-core-combine-max-dv2wg-4.1.1.spvasm", 4, PASS},
                {"heterogeneousAtomicity/ops-lib-core-combine-max-switch.spvasm", 4, PASS},
                {"gpuverify/atomics/atomic_read_race.spvasm", 1, FAIL},
                {"gpuverify/atomics/equality_fail.spvasm", 1, PASS},
                {"gpuverify/atomics/forloop.spvasm", 1, FAIL},
                {"gpuverify/atomics/histo.spvasm", 1, PASS},
                {"gpuverify/barrier_intervals/test1.spvasm", 1, PASS},
                {"gpuverify/basicbarrier.spvasm", 1, PASS},
                {"gpuverify/basicglobalarray.spvasm", 1, PASS},
                {"gpuverify/benign_race_tests/fail/writeafterread_addition.spvasm", 1, FAIL},
                {"gpuverify/benign_race_tests/fail/writeafterread_otherval.spvasm", 1, FAIL},
                {"gpuverify/benign_race_tests/fail/writezero_nobenign.spvasm", 1, FAIL},
                {"gpuverify/benign_race_tests/pass/writezero.spvasm", 1, FAIL},
                {"gpuverify/divergence/race_and_divergence.spvasm", 1, FAIL},
                {"gpuverify/divergence/race_no_divergence.spvasm", 1, FAIL},
                {"gpuverify/inter_group_and_barrier_flag_tests/fail/local_id.spvasm", 1, FAIL},
                {"gpuverify/inter_group_and_barrier_flag_tests/fail/no_barrier_flags.spvasm", 1, FAIL},
                {"gpuverify/inter_group_and_barrier_flag_tests/fail/sync.spvasm", 1, FAIL},
                {"gpuverify/inter_group_and_barrier_flag_tests/pass/local_id_benign_write_write.spvasm", 1, FAIL},
                {"gpuverify/inter_group_and_barrier_flag_tests/pass/pass_due_to_intra_group_flag.spvasm", 1, FAIL},
                {"gpuverify/mem_fence.spvasm", 1, PASS},
                {"gpuverify/misc/fail/miscfail1.spvasm", 1, FAIL},
                {"gpuverify/misc/fail/miscfail3.spvasm", 2, FAIL},
                {"gpuverify/misc/fail/struct_member_race.spvasm", 1, FAIL},
                {"gpuverify/misc/pass/misc2.spvasm", 2, PASS},
                {"gpuverify/multidimarrays/test5.spvasm", 1, FAIL},
                {"gpuverify/no_log/pass.spvasm", 1, FAIL},
                {"gpuverify/null_pointers/null_pointer_assignment_equal.spvasm", 1, FAIL},
                {"gpuverify/null_pointers/null_pointer_assignment_unequal.spvasm", 1, FAIL},
                {"gpuverify/null_pointers/null_pointer_greater.spvasm", 1, FAIL},
                {"gpuverify/pointertests/test_return_pointer.spvasm", 1, PASS},
                {"gpuverify/report_global_id/test1.spvasm", 1, PASS},
                {"gpuverify/report_global_id/test2.spvasm", 1, FAIL},
                {"gpuverify/sourcelocation_tests/barrier_divergence/pass.spvasm", 1, PASS},
                {"gpuverify/sourcelocation_tests/needs_source_location_ensures.spvasm", 9, PASS},
                {"gpuverify/sourcelocation_tests/needs_source_location_requires.spvasm", 9, PASS},
                {"gpuverify/sourcelocation_tests/races/fail/read_write.spvasm", 1, FAIL},
                {"gpuverify/sourcelocation_tests/races/fail/write_read.spvasm", 1, FAIL},
                {"gpuverify/sourcelocation_tests/races/fail/write_write/loop.spvasm", 1, FAIL},
                {"gpuverify/sourcelocation_tests/races/fail/write_write/normal.spvasm", 1, FAIL},
                {"gpuverify/sourcelocation_tests/races/pass/no_race.spvasm", 1, PASS},
                {"gpuverify/sourcelocation_tests/races/pass/read_read.spvasm", 1, PASS},
                {"gpuverify/test_for_benign_read_write_bug.spvasm", 1, FAIL},
                {"gpuverify/test_part_load_store/store_int_and_short.spvasm", 1, PASS},
                {"gpuverify/test_for_ssa_bug.spvasm", 9, PASS},
                {"gpuverify/test_structs/use_array_element.spvasm", 1, PASS},
                {"gpuverify/test_structs/use_element.spvasm", 1, PASS},
                {"gpuverify/test_structs/use_struct_element.spvasm", 1, PASS},
                {"gpuverify/vectortests/addressofvector.spvasm", 1, PASS},
                {"gpuverify/saturate/sadd.spvasm", 1, PASS},
                {"gpuverify/saturate/ssub.spvasm", 1, PASS},
                {"gpuverify/atomics/refined_atomic_abstraction/bad_local_counters.spvasm", 1, FAIL},
                {"gpuverify/atomics/refined_atomic_abstraction/intra_local_counters.spvasm", 1, PASS},
                {"gpuverify/atomics/counter.spvasm", 1, PASS},
                {"gpuverify/barrier_intervals/test2.spvasm", 1, PASS},
                {"gpuverify/sourcelocation_tests/barrier_divergence/fail.spvasm", 1, PASS},
                {"gpuverify/global_size/local_size_fail_divide_global_size.spvasm", 1, PASS},
                {"gpuverify/global_size/mismatch_dims.spvasm", 1, PASS},
                {"gpuverify/global_size/num_groups_and_global_size.spvasm", 1, PASS},
                {"gpuverify/inter_group_and_barrier_flag_tests/fail/missing_local_barrier_flag.spvasm", 1, FAIL},
                {"gpuverify/inter_group_and_barrier_flag_tests/pass/local_barrier_flag.spvasm", 1, PASS},
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