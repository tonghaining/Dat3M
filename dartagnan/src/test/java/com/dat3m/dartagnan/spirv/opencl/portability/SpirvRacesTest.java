package com.dat3m.dartagnan.spirv.opencl.portability;

import com.dat3m.dartagnan.configuration.Arch;
import com.dat3m.dartagnan.configuration.Method;
import com.dat3m.dartagnan.parsers.cat.ParserCat;
import com.dat3m.dartagnan.parsers.program.ProgramParser;
import com.dat3m.dartagnan.program.Program;
import com.dat3m.dartagnan.utils.Result;
import com.dat3m.dartagnan.utils.TestHelper;
import com.dat3m.dartagnan.verification.VerificationTask;
import com.dat3m.dartagnan.wmm.Wmm;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.EnumSet;

import static com.dat3m.dartagnan.configuration.Property.CAT_SPEC;
import static com.dat3m.dartagnan.utils.ResourceHelper.getRootPath;
import static com.dat3m.dartagnan.utils.ResourceHelper.getTestResourcePath;
import static com.dat3m.dartagnan.utils.Result.PASS;
import static com.dat3m.dartagnan.utils.Result.FAIL;
import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class SpirvRacesTest {

    private final String modelPath = getRootPath("cat/opencl.cat");
    private final String programPath;
    private final int bound;
    private final Result expected;

    public SpirvRacesTest(String file, int bound, Result expected) {
        this.programPath = getTestResourcePath("spirv/opencl/portability/" + file);
        this.bound = bound;
        this.expected = expected;
    }

    @Parameterized.Parameters(name = "{index}: {0}, {1}, {2}")
    public static Iterable<Object[]> data() throws IOException {
        return Arrays.asList(new Object[][]{
                {"mp-cross-region.spvasm", 1, FAIL},
                {"mp-cross-region-fix.spvasm", 1, PASS},
                {"mp-cross-region-extern.spvasm", 1, FAIL},
                {"mp-cross-region-extern-fix.spvasm", 1, PASS},
                // Kernels in benchmarks/cuda; -fix is the R1 repair pass.
                // Memory region (--ra-region-widen)
                {"mp-cas-cross-space.spvasm", 1, FAIL},
                {"mp-cas-cross-space-fix.spvasm", 1, PASS},
                {"mp-rel-acq-cross-space-cta.spvasm", 2, FAIL},
                {"mp-rel-acq-cross-space-cta-fix.spvasm", 2, PASS},
                {"mp-rel-acq-cross-space-swap.spvasm", 2, FAIL},
                {"mp-rel-acq-cross-space-swap-fix.spvasm", 2, PASS},
                // Scope inclusion (--scope-upper-alignment)
                {"rmw-add.spvasm", 1, FAIL},
                {"rmw-add-fix.spvasm", 1, PASS},
                // Sequential consistency (--st-sc)
                {"rwc-fence-sc-threadfence.spvasm", 1, PASS},
                {"rwc-fence-sc-threadfence-fix.spvasm", 1, PASS},
                // Sequential consistency with region-less SC fences
                // (--st-sc --ra-region-widen)
                {"rwc-fence-sc.spvasm", 1, PASS},
                {"rwc-fence-sc-fix.spvasm", 1, PASS},
                {"iriw-fence-sc.spvasm", 1, PASS},
                {"iriw-fence-sc-fix.spvasm", 1, PASS},
                // Data race (--atomic-affected)
                {"mp-fence-sc-weak.spvasm", 1, FAIL},
                {"mp-fence-sc-weak-fix.spvasm", 1, PASS},
                {"sb-fence-sc-weak.spvasm", 1, FAIL},
                {"sb-fence-sc-weak-fix.spvasm", 1, PASS},
        });
    }

    @Test
    public void test() throws Exception {
        assertEquals(expected, TestHelper.createAndRunSolver(mkTask(), Method.EAGER));
    }

    private VerificationTask mkTask() throws Exception {
        VerificationTask.VerificationTaskBuilder builder = VerificationTask.builder()
                .withConfig(TestHelper.getBasicConfig())
                .withBound(bound)
                .withTarget(Arch.OPENCL);
        Program program = new ProgramParser().parse(new File(programPath));
        Wmm mcm = new ParserCat().parse(new File(modelPath));
        return builder.build(program, mcm, EnumSet.of(CAT_SPEC));
    }
}
