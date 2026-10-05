package com.dat3m.dartagnan.litmus.compilation;

import com.dat3m.dartagnan.configuration.Arch;
import com.dat3m.dartagnan.utils.rules.Provider;
import com.dat3m.dartagnan.utils.rules.Providers;
import com.dat3m.dartagnan.wmm.Wmm;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.dat3m.dartagnan.utils.ResourceHelper.getRootPath;
import static java.util.Collections.emptyList;

@RunWith(Parameterized.class)
public class PtxToOpenCLTest extends AbstractCompilationWithDrCheckTest {

    private static final String PTX_DIR = "litmus/PTX/Portability/";
    private static final String OPENCL_DIR = "litmus/OPENCL/Portability/";

    // { PTX filename, OpenCL filename, expectDataRace, expectedToMatch }
    // expectedToMatch is omitted when expectDataRace is true, since it is ignored in that case.
    private static final Object[][] LITMUS_MAP = {
            // Sequential Consistency mismatches
            // Fix1: St-Sc
            // Fix2: All-Sc
            // Fix3: Ld-Sc
            // Fix4: St-Rel

            // IRIW-fence-sc
            {"IRIW-fence-sc.litmus", "IRIW-fence-sc-naive.litmus", false, false},
            {"IRIW-fence-sc.litmus", "IRIW-fence-sc-fix1.litmus", false, true},
            {"IRIW-fence-sc.litmus", "IRIW-fence-sc-fix2.litmus", false, true},
            {"IRIW-fence-sc.litmus", "IRIW-fence-sc-fix3.litmus", false, false},
            {"IRIW-fence-sc.litmus", "IRIW-fence-sc-fix4.litmus", false, false},

            // IRIW-fence-sc variant: sys scope (all_svm_devices)
            {"IRIW-fence-sc-sys.litmus", "IRIW-fence-sc-sys-naive.litmus", false, false},
            {"IRIW-fence-sc-sys.litmus", "IRIW-fence-sc-sys-fix1.litmus", false, true},
            {"IRIW-fence-sc-sys.litmus", "IRIW-fence-sc-sys-fix2.litmus", false, true},
            {"IRIW-fence-sc-sys.litmus", "IRIW-fence-sc-sys-fix3.litmus", false, false},
            {"IRIW-fence-sc-sys.litmus", "IRIW-fence-sc-sys-fix4.litmus", false, false},

            // IRIW-fence-sc variant: same CTA, cta scope (work_group)
            {"IRIW-fence-sc-cta.litmus", "IRIW-fence-sc-cta-naive.litmus", false, false},
            {"IRIW-fence-sc-cta.litmus", "IRIW-fence-sc-cta-fix1.litmus", false, true},
            {"IRIW-fence-sc-cta.litmus", "IRIW-fence-sc-cta-fix2.litmus", false, true},
            {"IRIW-fence-sc-cta.litmus", "IRIW-fence-sc-cta-fix3.litmus", false, false},
            {"IRIW-fence-sc-cta.litmus", "IRIW-fence-sc-cta-fix4.litmus", false, false},

            // IRIW-fence-sc variant: same CTA, cta scope, local memory
            {"IRIW-fence-sc-cta.litmus", "IRIW-fence-sc-cta-local-naive.litmus", false, false},
            {"IRIW-fence-sc-cta.litmus", "IRIW-fence-sc-cta-local-fix1.litmus", false, true},
            {"IRIW-fence-sc-cta.litmus", "IRIW-fence-sc-cta-local-fix2.litmus", false, true},
            {"IRIW-fence-sc-cta.litmus", "IRIW-fence-sc-cta-local-fix3.litmus", false, false},
            {"IRIW-fence-sc-cta.litmus", "IRIW-fence-sc-cta-local-fix4.litmus", false, false},

            // RWC-fence-sc
            {"RWC-fence-sc.litmus", "RWC-fence-sc-naive.litmus", false, false},
            {"RWC-fence-sc.litmus", "RWC-fence-sc-fix1.litmus", false, true},
            {"RWC-fence-sc.litmus", "RWC-fence-sc-fix2.litmus", false, true},
            {"RWC-fence-sc.litmus", "RWC-fence-sc-fix3.litmus", false, false},
            {"RWC-fence-sc.litmus", "RWC-fence-sc-fix4.litmus", false, false},

            // RWC-fence-sc variant: sys scope (all_svm_devices)
            {"RWC-fence-sc-sys.litmus", "RWC-fence-sc-sys-naive.litmus", false, false},
            {"RWC-fence-sc-sys.litmus", "RWC-fence-sc-sys-fix1.litmus", false, true},
            {"RWC-fence-sc-sys.litmus", "RWC-fence-sc-sys-fix2.litmus", false, true},
            {"RWC-fence-sc-sys.litmus", "RWC-fence-sc-sys-fix3.litmus", false, false},
            {"RWC-fence-sc-sys.litmus", "RWC-fence-sc-sys-fix4.litmus", false, false},

            // RWC-fence-sc variant: same CTA, cta scope (work_group)
            {"RWC-fence-sc-cta.litmus", "RWC-fence-sc-cta-naive.litmus", false, false},
            {"RWC-fence-sc-cta.litmus", "RWC-fence-sc-cta-fix1.litmus", false, true},
            {"RWC-fence-sc-cta.litmus", "RWC-fence-sc-cta-fix2.litmus", false, true},
            {"RWC-fence-sc-cta.litmus", "RWC-fence-sc-cta-fix3.litmus", false, false},
            {"RWC-fence-sc-cta.litmus", "RWC-fence-sc-cta-fix4.litmus", false, false},

            // RWC-fence-sc variant: same CTA, cta scope, local memory
            {"RWC-fence-sc-cta.litmus", "RWC-fence-sc-cta-local-naive.litmus", false, false},
            {"RWC-fence-sc-cta.litmus", "RWC-fence-sc-cta-local-fix1.litmus", false, true},
            {"RWC-fence-sc-cta.litmus", "RWC-fence-sc-cta-local-fix2.litmus", false, true},
            {"RWC-fence-sc-cta.litmus", "RWC-fence-sc-cta-local-fix3.litmus", false, false},
            {"RWC-fence-sc-cta.litmus", "RWC-fence-sc-cta-local-fix4.litmus", false, false},

            // W+RWC-fence-sc
            {"W+RWC-fence-sc.litmus", "W+RWC-fence-sc-naive.litmus", false, false},
            {"W+RWC-fence-sc.litmus", "W+RWC-fence-sc-fix1.litmus", false, true},
            {"W+RWC-fence-sc.litmus", "W+RWC-fence-sc-fix2.litmus", false, true},
            {"W+RWC-fence-sc.litmus", "W+RWC-fence-sc-fix3.litmus", false, false},
            {"W+RWC-fence-sc.litmus", "W+RWC-fence-sc-fix4.litmus", false, false},
            {"W+RWC-fence-sc.litmus", "W+RWC-fence-sc-fix5.litmus", false, true}, // Pick ones involved in fence-sc interleaving
            {"W+RWC-fence-sc.litmus", "W+RWC-fence-sc-fix6.litmus", false, false}, // Pick ones not involved in fence-sc interleaving

            // W+RWC-fence-sc variant: sys scope (all_svm_devices)
            {"W+RWC-fence-sc-sys.litmus", "W+RWC-fence-sc-sys-naive.litmus", false, false},
            {"W+RWC-fence-sc-sys.litmus", "W+RWC-fence-sc-sys-fix1.litmus", false, true},
            {"W+RWC-fence-sc-sys.litmus", "W+RWC-fence-sc-sys-fix2.litmus", false, true},
            {"W+RWC-fence-sc-sys.litmus", "W+RWC-fence-sc-sys-fix3.litmus", false, false},
            {"W+RWC-fence-sc-sys.litmus", "W+RWC-fence-sc-sys-fix4.litmus", false, false},
            {"W+RWC-fence-sc-sys.litmus", "W+RWC-fence-sc-sys-fix5.litmus", false, true},
            {"W+RWC-fence-sc-sys.litmus", "W+RWC-fence-sc-sys-fix6.litmus", false, false},

            // W+RWC-fence-sc variant: same CTA, cta scope (work_group)
            {"W+RWC-fence-sc-cta.litmus", "W+RWC-fence-sc-cta-naive.litmus", false, false},
            {"W+RWC-fence-sc-cta.litmus", "W+RWC-fence-sc-cta-fix1.litmus", false, true},
            {"W+RWC-fence-sc-cta.litmus", "W+RWC-fence-sc-cta-fix2.litmus", false, true},
            {"W+RWC-fence-sc-cta.litmus", "W+RWC-fence-sc-cta-fix3.litmus", false, false},
            {"W+RWC-fence-sc-cta.litmus", "W+RWC-fence-sc-cta-fix4.litmus", false, false},
            {"W+RWC-fence-sc-cta.litmus", "W+RWC-fence-sc-cta-fix5.litmus", false, true},
            {"W+RWC-fence-sc-cta.litmus", "W+RWC-fence-sc-cta-fix6.litmus", false, false},

            // W+RWC-fence-sc variant: same CTA, cta scope, local memory
            {"W+RWC-fence-sc-cta.litmus", "W+RWC-fence-sc-cta-local-naive.litmus", false, false},
            {"W+RWC-fence-sc-cta.litmus", "W+RWC-fence-sc-cta-local-fix1.litmus", false, true},
            {"W+RWC-fence-sc-cta.litmus", "W+RWC-fence-sc-cta-local-fix2.litmus", false, true},
            {"W+RWC-fence-sc-cta.litmus", "W+RWC-fence-sc-cta-local-fix3.litmus", false, false},
            {"W+RWC-fence-sc-cta.litmus", "W+RWC-fence-sc-cta-local-fix4.litmus", false, false},
            {"W+RWC-fence-sc-cta.litmus", "W+RWC-fence-sc-cta-local-fix5.litmus", false, true},
            {"W+RWC-fence-sc-cta.litmus", "W+RWC-fence-sc-cta-local-fix6.litmus", false, false},

            // WRC-fence-acq-rel
            {"WRC-fence-acq-rel.litmus", "WRC-fence-acq-rel-naive.litmus", false, true},

            // Memory Region Mismatches
            // Fix1: Ra-Region-Widen
            // Fix2: Ra-Target-Only
            // Fix3: Ra-Inner-Fence

            // MP-rel-acq-cross-space
            {"MP-rel-acq-cross-space.litmus", "MP-rel-acq-same-space.litmus", false, true},
            {"MP-rel-acq-cross-space.litmus", "MP-rel-acq-cross-space-naive.litmus", true},
            {"MP-rel-acq-cross-space.litmus", "MP-rel-acq-cross-space-fix1.litmus", false, true},
            {"MP-rel-acq-cross-space.litmus", "MP-rel-acq-cross-space-fix2.litmus", true},
            {"MP-rel-acq-cross-space.litmus", "MP-rel-acq-cross-space-fix3.litmus", true},
            {"MP-rel-acq-cross-space.litmus", "MP-rel-acq-cross-space-fix4.litmus", false, true}, // Use barriers with both flags
            {"MP-rel-acq-cross-space.litmus", "MP-rel-acq-cross-space-fix5.litmus", false, true}, // Use barriers with source flag only
            {"MP-rel-acq-cross-space.litmus", "MP-rel-acq-cross-space-fix6.litmus", true}, // Use barriers with target flag only
            {"MP-rel-acq-cross-space.litmus", "MP-rel-acq-cross-space-fix7.litmus", true}, // Use Source Memory Space Flag
            {"MP-rel-acq-cross-space.litmus", "MP-rel-acq-cross-space-fix8.litmus", true}, // Use SC

            // MP-rel-acq-cross-space variant: same CTA, cta scope (work_group)
            {"MP-rel-acq-cross-space-cta.litmus", "MP-rel-acq-same-space.litmus", false, true},
            {"MP-rel-acq-cross-space-cta.litmus", "MP-rel-acq-cross-space-naive.litmus", true},
            {"MP-rel-acq-cross-space-cta.litmus", "MP-rel-acq-cross-space-fix1.litmus", false, true},
            {"MP-rel-acq-cross-space-cta.litmus", "MP-rel-acq-cross-space-fix2.litmus", true},
            {"MP-rel-acq-cross-space-cta.litmus", "MP-rel-acq-cross-space-fix3.litmus", true},
            {"MP-rel-acq-cross-space-cta.litmus", "MP-rel-acq-cross-space-fix4.litmus", false, true},
            {"MP-rel-acq-cross-space-cta.litmus", "MP-rel-acq-cross-space-fix5.litmus", false, true},
            {"MP-rel-acq-cross-space-cta.litmus", "MP-rel-acq-cross-space-fix6.litmus", true},
            {"MP-rel-acq-cross-space-cta.litmus", "MP-rel-acq-cross-space-fix7.litmus", true},
            {"MP-rel-acq-cross-space-cta.litmus", "MP-rel-acq-cross-space-fix8.litmus", true},

            // MP-rel-acq-cross-space variant: memory spaces swapped
            {"MP-rel-acq-cross-space.litmus", "MP-rel-acq-cross-space-swap-same-space.litmus", false, true},
            {"MP-rel-acq-cross-space.litmus", "MP-rel-acq-cross-space-swap-naive.litmus", true},
            {"MP-rel-acq-cross-space.litmus", "MP-rel-acq-cross-space-swap-fix1.litmus", false, true},
            {"MP-rel-acq-cross-space.litmus", "MP-rel-acq-cross-space-swap-fix2.litmus", true},
            {"MP-rel-acq-cross-space.litmus", "MP-rel-acq-cross-space-swap-fix3.litmus", true},
            {"MP-rel-acq-cross-space.litmus", "MP-rel-acq-cross-space-swap-fix4.litmus", false, true},
            {"MP-rel-acq-cross-space.litmus", "MP-rel-acq-cross-space-swap-fix5.litmus", false, true},
            {"MP-rel-acq-cross-space.litmus", "MP-rel-acq-cross-space-swap-fix6.litmus", true},
            {"MP-rel-acq-cross-space.litmus", "MP-rel-acq-cross-space-swap-fix7.litmus", true},
            {"MP-rel-acq-cross-space.litmus", "MP-rel-acq-cross-space-swap-fix8.litmus", true},

            // MP-rel-acq-cross-space variant: device scope in OpenCL
            {"MP-rel-acq-cross-space.litmus", "MP-rel-acq-cross-space-dev-same-space.litmus", false, true},
            {"MP-rel-acq-cross-space.litmus", "MP-rel-acq-cross-space-dev-naive.litmus", true},
            {"MP-rel-acq-cross-space.litmus", "MP-rel-acq-cross-space-dev-fix1.litmus", false, true},
            {"MP-rel-acq-cross-space.litmus", "MP-rel-acq-cross-space-dev-fix2.litmus", true},
            {"MP-rel-acq-cross-space.litmus", "MP-rel-acq-cross-space-dev-fix3.litmus", true},
            {"MP-rel-acq-cross-space.litmus", "MP-rel-acq-cross-space-dev-fix4.litmus", false, true},
            {"MP-rel-acq-cross-space.litmus", "MP-rel-acq-cross-space-dev-fix5.litmus", false, true},
            {"MP-rel-acq-cross-space.litmus", "MP-rel-acq-cross-space-dev-fix6.litmus", true},
            {"MP-rel-acq-cross-space.litmus", "MP-rel-acq-cross-space-dev-fix7.litmus", true},
            {"MP-rel-acq-cross-space.litmus", "MP-rel-acq-cross-space-dev-fix8.litmus", true},

            // MP-cas-cross-space
            {"MP-cas-cross-space.litmus", "MP-cas-cross-space-naive.litmus", true},
            {"MP-cas-cross-space.litmus", "MP-cas-cross-space-fix1.litmus", false, true},
            {"MP-cas-cross-space.litmus", "MP-cas-cross-space-fix2.litmus", true},
            {"MP-cas-cross-space.litmus", "MP-cas-cross-space-fix3.litmus", true},

            // MP-cas-cross-space variant: memory spaces swapped
            {"MP-cas-cross-space.litmus", "MP-cas-cross-space-swap-naive.litmus", true},
            {"MP-cas-cross-space.litmus", "MP-cas-cross-space-swap-fix1.litmus", false, true},
            {"MP-cas-cross-space.litmus", "MP-cas-cross-space-swap-fix2.litmus", true},
            {"MP-cas-cross-space.litmus", "MP-cas-cross-space-swap-fix3.litmus", true},

            // MP-cas-cross-space variant: same CTA, cta scope (work_group)
            {"MP-cas-cross-space-cta.litmus", "MP-cas-cross-space-cta-naive.litmus", true},
            {"MP-cas-cross-space-cta.litmus", "MP-cas-cross-space-cta-fix1.litmus", false, true},
            {"MP-cas-cross-space-cta.litmus", "MP-cas-cross-space-cta-fix2.litmus", true},
            {"MP-cas-cross-space-cta.litmus", "MP-cas-cross-space-cta-fix3.litmus", true},

            // SB-barrier-cross-space
            {"SB-barrier-cross-space.litmus", "SB-barrier-cross-space-naive.litmus", true},
            {"SB-barrier-cross-space.litmus", "SB-barrier-cross-space-fix1.litmus", false, true},

            // SB-barrier-cross-space variant: local memory
            {"SB-barrier-cross-space.litmus", "SB-barrier-cross-space-local-naive.litmus", true},
            {"SB-barrier-cross-space.litmus", "SB-barrier-cross-space-local-fix1.litmus", false, true},

            // SB-barrier-cross-space variant: x global, y local
            {"SB-barrier-cross-space.litmus", "SB-barrier-cross-space-mixed-naive.litmus", true},
            {"SB-barrier-cross-space.litmus", "SB-barrier-cross-space-mixed-fix1.litmus", false, true},

            {"SB-fence-sc-relaxed.litmus", "SB-fence-sc.litmus", false, true},
            {"SB-fence-sc-relaxed.litmus", "SB-fence-sc-relaxed-cross-space.litmus", false, true},
            {"SB-fence-sc-relaxed.litmus", "SB-fence-sc-relaxed-cross-space1.litmus", false, true},
            {"SUM.litmus", "SUM.litmus", false, true},
            {"MP-fence-sc-cross-space.litmus", "MP-fence-sc-cross-space.litmus", false, false},
            {"MP-fence-sc-cross-space-add.litmus", "MP-fence-sc-cross-space-add.litmus", false, true},
            {"MP-fence-sc-cross-space-add-3.litmus", "MP-fence-sc-cross-space-add-3.litmus", false, true},
            {"MP-fence-acqrel-cross-space-add.litmus", "MP-fence-acqrel-cross-space-add.litmus", false, false},
            {"MP-fence-acqrel-cross-space-add-3.litmus", "MP-fence-acqrel-cross-space-add-3.litmus", false, false},
            {"MP-fence-acqrel-cross-space-add.litmus", "MP-fence-acqrel-cross-space-add-local.litmus", false, false},
            {"MP-fence-acqrel-cross-space-add.litmus", "MP-fence-acqrel-cross-space-add-asym.litmus", false, false},
            {"MP-fence-acqrel-cross-space-add.litmus", "MP-fence-acqrel-cross-space-add-fix.litmus", false, true},
            {"MP-fence-acq-rel.litmus", "MP-fence-acqrel-cross-wg.litmus", false, true},

            // Scope Inclusion Mismatches
            // Fix1: Scope-Upper-Alignment
            // Fix2: Scope-Lower-Alignment

            // RMW-add
            {"RMW-add.litmus", "RMW-add-naive.litmus", true},
            {"RMW-add.litmus", "RMW-add-fix1.litmus", false, true},
            {"RMW-add.litmus", "RMW-add-fix2.litmus", false, true},

            // RMW-add variant: same CTA, P0 cta / P1 gpu
            {"RMW-add-cta-gpu.litmus", "RMW-add-cta-gpu-naive.litmus", true},
            {"RMW-add-cta-gpu.litmus", "RMW-add-cta-gpu-fix1.litmus", false, true},
            {"RMW-add-cta-gpu.litmus", "RMW-add-cta-gpu-fix2.litmus", false, true},

            // RMW-add variant: same CTA, P0 cta / P1 sys
            {"RMW-add-cta-sys.litmus", "RMW-add-cta-sys-naive.litmus", true},
            {"RMW-add-cta-sys.litmus", "RMW-add-cta-sys-fix1.litmus", false, true},
            {"RMW-add-cta-sys.litmus", "RMW-add-cta-sys-fix2.litmus", false, true},

            // RMW-add variant: same CTA, P0 cta / P1 gpu, local memory
            {"RMW-add-cta-gpu.litmus", "RMW-add-cta-gpu-local-naive.litmus", true},
            {"RMW-add-cta-gpu.litmus", "RMW-add-cta-gpu-local-fix1.litmus", false, true},
            {"RMW-add-cta-gpu.litmus", "RMW-add-cta-gpu-local-fix2.litmus", false, true},

            // Data Race Mismatches
            // Fix1: Atomic-Affected

            // MP-fence-sc-weak
            {"MP-fence-sc-weak.litmus", "MP-fence-sc-weak-naive.litmus", true},
            {"MP-fence-sc-weak.litmus", "MP-fence-sc-weak-fix1.litmus", false, true},

            // MP-fence-sc-weak variant: sys scope (all_svm_devices)
            {"MP-fence-sc-weak-sys.litmus", "MP-fence-sc-weak-sys-naive.litmus", true},
            {"MP-fence-sc-weak-sys.litmus", "MP-fence-sc-weak-sys-fix1.litmus", false, true},

            // MP-fence-sc-weak variant: same CTA, cta scope (work_group)
            {"MP-fence-sc-weak-cta.litmus", "MP-fence-sc-weak-cta-naive.litmus", true},
            {"MP-fence-sc-weak-cta.litmus", "MP-fence-sc-weak-cta-fix1.litmus", false, true},

            // MP-fence-sc-weak variant: same CTA, cta scope, local memory
            {"MP-fence-sc-weak-cta.litmus", "MP-fence-sc-weak-cta-local-naive.litmus", true},
            {"MP-fence-sc-weak-cta.litmus", "MP-fence-sc-weak-cta-local-fix1.litmus", false, true},

            // SB-fence-sc-weak
            {"SB-fence-sc-weak.litmus", "SB-fence-sc-weak.litmus", true},
            {"SB-fence-sc-weak.litmus", "SB-fence-sc.litmus", false, true}, // Fix1

            // SB-fence-sc-weak variant: sys scope (all_svm_devices)
            {"SB-fence-sc-weak-sys.litmus", "SB-fence-sc-weak-sys-naive.litmus", true},
            {"SB-fence-sc-weak-sys.litmus", "SB-fence-sc-weak-sys-fix1.litmus", false, true},

            // SB-fence-sc-weak variant: same CTA, cta scope (work_group)
            {"SB-fence-sc-weak-cta.litmus", "SB-fence-sc-weak-cta-naive.litmus", true},
            {"SB-fence-sc-weak-cta.litmus", "SB-fence-sc-weak-cta-fix1.litmus", false, true},

            // SB-fence-sc-weak variant: same CTA, cta scope, local memory
            {"SB-fence-sc-weak-cta.litmus", "SB-fence-sc-weak-cta-local-naive.litmus", true},
            {"SB-fence-sc-weak-cta.litmus", "SB-fence-sc-weak-cta-local-fix1.litmus", false, true},

            // Other litmus tests
            {"LB.litmus", "LB.litmus", false, true},
            {"RMW-release-sequence.litmus", "RMW-release-sequence.litmus", false, true},
            {"RMW-cas.litmus", "RMW-cas.litmus", false, true},
    };
    private final String targetPath;
    private final boolean expectDataRace;
    private final boolean expectedToMatch;
    public PtxToOpenCLTest(String sourcePath, String targetPath, boolean expectDataRace, boolean expectedToMatch) {
        super(sourcePath);
        this.targetPath = targetPath;
        this.expectDataRace = expectDataRace;
        this.expectedToMatch = expectedToMatch;
    }

    @Parameterized.Parameters(name = "{index}: {0} -> {1}")
    public static Iterable<Object[]> data() {
        return Arrays.stream(LITMUS_MAP)
                .map(e -> new Object[]{
                        getRootPath(PTX_DIR + e[0]),
                        getRootPath(OPENCL_DIR + e[1]),
                        e[2],
                        e.length > 3 ? e[3] : false
                })
                .collect(Collectors.toList());
    }

    @Override
    protected Provider<Arch> getSourceProvider() {
        return () -> Arch.PTX;
    }

    @Override
    protected Provider<Wmm> getSourceWmmProvider() {
        return Providers.createWmmFromName(() -> "ptx-v7.5");
    }

    @Override
    protected Provider<Arch> getTargetProvider() {
        return () -> Arch.OPENCL;
    }

    @Override
    protected Provider<String> getTargetFilePathProvider() {
        return () -> targetPath;
    }

    @Override
    protected boolean isExpectedDataRace() {
        return expectDataRace;
    }

    @Override
    protected List<String> getCompilationBreakers() {
        return expectedToMatch ? emptyList() : List.of(filePathProvider.get());
    }
}
