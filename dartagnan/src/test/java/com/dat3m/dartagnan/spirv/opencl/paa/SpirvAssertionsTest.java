package com.dat3m.dartagnan.spirv.opencl.paa;

import com.dat3m.dartagnan.configuration.Arch;
import com.dat3m.dartagnan.configuration.Method;
import com.dat3m.dartagnan.parsers.cat.ParserCat;
import com.dat3m.dartagnan.parsers.program.ProgramParser;
import com.dat3m.dartagnan.program.Program;
import com.dat3m.dartagnan.verification.ResultStatus;
import com.dat3m.dartagnan.utils.TestHelper;
import com.dat3m.dartagnan.verification.Task;
import com.dat3m.dartagnan.wmm.Wmm;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.file.Path;
import java.io.IOException;
import java.util.Arrays;
import java.util.EnumSet;

import static com.dat3m.dartagnan.configuration.Property.PROGRAM_SPEC;
import static com.dat3m.dartagnan.utils.ResourceHelper.getRootPath;
import static com.dat3m.dartagnan.utils.ResourceHelper.getTestResourcePath;
import static com.dat3m.dartagnan.verification.ResultStatus.FAIL;
import static com.dat3m.dartagnan.verification.ResultStatus.PASS;
import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class SpirvAssertionsTest {

    private final Path modelPath = getRootPath("cat/opencl-paa.cat");
    private final Path programPath;
    private final int bound;
    private final ResultStatus expected;

    public SpirvAssertionsTest(String file, int bound, ResultStatus expected) {
        this.programPath = getTestResourcePath("spirv/opencl/" + file);
        this.bound = bound;
        this.expected = expected;
    }

    @Parameterized.Parameters(name = "{index}: {0}, {1}, {2}")
    public static Iterable<Object[]> data() throws IOException {
        return Arrays.asList(new Object[][]{
                {"heterogeneousAtomicity/histogram-1.1.4.spvasm", 2, FAIL},
                {"heterogeneousAtomicity/histogram-2.1.2.spvasm", 2, PASS},
                {"heterogeneousAtomicity/histogram-4.1.1.spvasm", 2, PASS},
                {"heterogeneousAtomicity/compact-features-2.1.2.spvasm", 2, PASS},
                {"heterogeneousAtomicity/HeCBench-string-search-2.1.2.spvasm", 2, PASS},
                {"heterogeneousAtomicity/amrex-GpuAtomic-1.1.4.spvasm", 4, PASS},
                {"heterogeneousAtomicity/amrex-GpuAtomic-4.1.1.spvasm", 4, PASS},
                {"heterogeneousAtomicity/amrex-GpuAtomic-2.1.2.spvasm", 4, PASS},
                {"heterogeneousAtomicity/amrex-GpuAtomic-dv2wg-4.1.1.spvasm", 4, PASS},
                {"heterogeneousAtomicity/ops-lib-core-combine-max-4.1.2.spvasm", 2, PASS},
                {"heterogeneousAtomicity/ops-lib-core-combine-max-4.1.1.spvasm", 2, PASS},
                {"heterogeneousAtomicity/ops-lib-core-combine-max-2.1.2.spvasm", 2, PASS},
                {"heterogeneousAtomicity/ops-lib-core-combine-max-switch.spvasm", 2, PASS},
                {"heterogeneousAtomicity/ops-lib-core-combine-max-dv2wg-4.1.1.spvasm", 4, PASS},
                {"heterogeneousAtomicity/torch-xpu-ops-AtomicIntegerImpl-fix-2.1.2.spvasm", 4, PASS},
                {"alignment/alignment1-array-global.spvasm", 9, PASS},
                {"alignment/alignment1-array-local.spvasm", 9, PASS},
                {"alignment/alignment1-array-pointer.spvasm", 9, PASS},
                {"alignment/alignment1-struct-global.spvasm", 9, PASS},
                {"alignment/alignment1-struct-local.spvasm", 9, PASS},
                {"alignment/alignment1-struct-pointer.spvasm", 9, PASS},
                {"alignment/alignment2-struct-global.spvasm", 17, PASS},
                {"alignment/alignment2-struct-local.spvasm", 17, PASS},
                {"alignment/alignment2-struct-pointer.spvasm", 17, PASS},
                {"alignment/alignment3-struct-global.spvasm", 9, PASS},
                {"alignment/alignment3-struct-local.spvasm", 9, PASS},
                {"alignment/alignment3-struct-pointer.spvasm", 9, PASS},
                {"alignment/alignment4-struct-global.spvasm", 25, PASS},
                {"alignment/alignment4-struct-local.spvasm", 25, PASS},
                {"alignment/alignment4-struct-pointer.spvasm", 25, PASS},
                {"alignment/alignment5-struct-global.spvasm", 17, PASS},
                {"alignment/alignment5-struct-local.spvasm", 17, PASS},
                {"alignment/alignment5-struct-pointer.spvasm", 17, PASS},
                {"barrier/inlining/barrier-inlining-1-forall-correct.spvasm", 1, PASS},
                {"barrier/inlining/barrier-inlining-1-exists-correct.spvasm", 1, PASS},
                {"barrier/inlining/barrier-inlining-1-forall-wrong.spvasm", 1, FAIL},
                {"barrier/inlining/barrier-inlining-1-exists-wrong.spvasm", 1, FAIL},
                {"barrier/inlining/barrier-no-inlining-1-forall-correct.spvasm", 1, PASS},
                {"barrier/inlining/barrier-no-inlining-1-exists-correct.spvasm", 1, PASS},
                {"barrier/inlining/barrier-no-inlining-1-forall-wrong.spvasm", 1, FAIL},
                {"barrier/inlining/barrier-no-inlining-1-exists-wrong.spvasm", 1, FAIL},
                {"barrier/inlining/barrier-inlining-2-forall-correct.spvasm", 1, PASS},
                {"barrier/inlining/barrier-inlining-2-exists-correct.spvasm", 1, PASS},
                {"barrier/inlining/barrier-inlining-2-forall-wrong.spvasm", 1, FAIL},
                {"barrier/inlining/barrier-inlining-2-exists-wrong.spvasm", 1, FAIL},
                {"barrier/inlining/barrier-inlining-3-forall-correct.spvasm", 1, PASS},
                {"barrier/inlining/barrier-inlining-3-exists-correct.spvasm", 1, PASS},
                {"barrier/inlining/barrier-inlining-3-forall-wrong.spvasm", 1, FAIL},
                {"barrier/inlining/barrier-inlining-3-exists-wrong.spvasm", 1, FAIL},
                {"barrier/inlining/barrier-inlining-4-forall-correct.spvasm", 3, PASS},
                {"barrier/inlining/barrier-inlining-4-exists-correct.spvasm", 3, PASS},
                {"barrier/inlining/barrier-inlining-4-forall-wrong.spvasm", 3, FAIL},
                {"barrier/inlining/barrier-inlining-4-exists-wrong.spvasm", 3, FAIL},
                {"barrier/inlining/barrier-inlining-5-forall-correct.spvasm", 3, PASS},
                {"barrier/inlining/barrier-inlining-5-exists-correct.spvasm", 3, PASS},
                {"barrier/inlining/barrier-inlining-5-forall-wrong.spvasm", 3, FAIL},
                {"barrier/inlining/barrier-inlining-5-exists-wrong.spvasm", 3, FAIL},
                {"barrier/scope/barrier-inscope-wg.spvasm", 1, PASS},
                {"barrier/scope/barrier-not-inscope-wg.spvasm", 1, FAIL},
                {"basic/idx-overflow.spvasm", 1, PASS},
                {"benchmarks/caslock-acq2rx.spvasm", 2, FAIL},
                {"benchmarks/caslock-rel2rx.spvasm", 2, FAIL},
                {"benchmarks/caslock-dv2wg-1.1.2.spvasm", 2, FAIL},
                {"benchmarks/ticketlock-1.1.2.spvasm", 1, PASS},
                {"benchmarks/ticketlock-2.1.1.spvasm", 1, PASS},
                {"benchmarks/ticketlock-acq2rx.spvasm", 1, FAIL},
                {"benchmarks/ticketlock-rel2rx.spvasm", 1, FAIL},
                {"benchmarks/ticketlock-dv2wg-2.1.1.spvasm", 2, PASS},
                {"benchmarks/ticketlock-dv2wg-1.1.2.spvasm", 1, FAIL},
                {"benchmarks/ttaslock-1.1.2.spvasm", 2, PASS},
                {"benchmarks/ttaslock-2.1.1.spvasm", 2, PASS},
                {"benchmarks/ttaslock-acq2rx.spvasm", 1, FAIL},
                {"benchmarks/ttaslock-rel2rx.spvasm", 1, FAIL},
                {"benchmarks/ttaslock-dv2wg-2.1.1.spvasm", 2, PASS},
                {"benchmarks/ttaslock-dv2wg-1.1.2.spvasm", 1, FAIL},
                {"benchmarks/xf-barrier-2.1.2.spvasm", 9, PASS},
                {"benchmarks/xf-barrier-2.1.1.spvasm", 9, PASS},
                {"benchmarks/xf-barrier-fail1.spvasm", 9, FAIL},
                {"benchmarks/xf-barrier-fail2.spvasm", 9, FAIL},
                {"benchmarks/xf-barrier-fail3.spvasm", 9, FAIL},
                {"benchmarks/xf-barrier-fail4.spvasm", 9, FAIL},
                {"benchmarks/xf-barrier-weakest.spvasm", 9, FAIL},
                {"patterns/corr.spvasm", 2, PASS},
                {"patterns/iriw.spvasm", 2, PASS},
                {"patterns/mp.spvasm", 2, PASS},
                {"patterns/mp-acq2rx.spvasm", 2, FAIL},
                {"patterns/mp-rel2rx.spvasm", 2, FAIL},
                {"patterns/sb.spvasm", 2, PASS},
        });
    }

    @Test
    public void test() throws Exception {
        assertEquals(expected, TestHelper.createAndRunSolver(mkTask(), Method.EAGER));
    }

    private Task mkTask() throws Exception {
        Task.TaskBuilder builder = Task.builder()
                .withConfig(TestHelper.getBasicConfig())
                .withBound(bound)
                .withTarget(Arch.OPENCLPAA);
        Program program = new ProgramParser().parse(programPath);
        Wmm mcm = new ParserCat().parse(modelPath);
        return builder.build(program, mcm, EnumSet.of(PROGRAM_SPEC));
    }
}
