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

    // { PTX filename, OpenCL filename, expectedToMatch }
    private static final Object[][] LITMUS_MAP = {
            {"IRIW-fence-sc.litmus", "IRIW-fence-sc.litmus", false},
            {"IRIW-fence-sc.litmus", "IRIW-fence-sc-fix1.litmus", true},
            {"IRIW-fence-sc.litmus", "IRIW-fence-sc-fix2.litmus", false},
            {"IRIW-fence-sc.litmus", "IRIW-fence-sc-fix3.litmus", true},
            {"RWC-fence-sc.litmus", "RWC-fence-sc.litmus", false},
            {"RWC-fence-sc.litmus", "RWC-fence-sc-fix1.litmus", true},
            {"SB-fence-sc-relaxed.litmus", "SB-fence-sc-relaxed.litmus", true},
            {"SB-fence-sc-relaxed.litmus", "SB-fence-sc-relaxed-cross-space.litmus", true},
            {"SB-fence-sc-relaxed.litmus", "SB-fence-sc-relaxed-cross-space1.litmus", true},
            // {"SB-fence-sc-weak.litmus", "SB-fence-sc-weak.litmus", false}, // Data Race
            {"WRC-fence-acq-rel.litmus", "WRC-fence-acq-rel.litmus", true},
            {"W+RWC-fence-sc.litmus", "W+RWC-fence-sc.litmus", false},
            {"W+RWC-fence-sc.litmus", "W+RWC-fence-sc-fix1.litmus", true},
            {"W+RWC-fence-sc.litmus", "W+RWC-fence-sc-fix2.litmus", false},
            {"LB.litmus", "LB.litmus", true},
            {"SUM.litmus", "SUM.litmus", true},
            {"MP.litmus", "MP-same-space.litmus", true},
            // {"MP.litmus", "MP-cross-space.litmus", false}, // Data Race
            // {"MP.litmus", "MP-cross-space-fix1.litmus", true}, // Data Race
            // {"MP.litmus", "MP-cross-space-fix2.litmus", true}, // Data Race
            {"MP.litmus", "MP-cross-space-fix3.litmus", true},
            {"MP.litmus", "MP-cross-space-fix4.litmus", true},
            // {"MP.litmus", "MP-cross-space-fix5.litmus", true}, // Data Race
            {"MP.litmus", "MP-cross-space-fix6.litmus", true},
            // {"MP.litmus", "MP-cross-space-fix7.litmus", true},  // Data Race
            // {"MP.litmus", "MP-cross-space-fix8.litmus", true}, // Data Race
            // {"MP-fence-sc-weak.litmus", "MP-fence-sc-weak.litmus", true}, // Data Race
            {"MP-fence-sc-cross-space.litmus", "MP-fence-sc-cross-space.litmus", false},
            {"MP-fence-sc-cross-space-add.litmus", "MP-fence-sc-cross-space-add.litmus", true},
            {"MP-fence-sc-cross-space-add-3.litmus", "MP-fence-sc-cross-space-add-3.litmus", true},
            {"MP-fence-acqrel-cross-space-add.litmus", "MP-fence-acqrel-cross-space-add.litmus", false},
            {"MP-fence-acqrel-cross-space-add-3.litmus", "MP-fence-acqrel-cross-space-add-3.litmus", false},
            {"MP-fence-acqrel-cross-space-add.litmus", "MP-fence-acqrel-cross-space-add-local.litmus", false},
            {"MP-fence-acqrel-cross-space-add.litmus", "MP-fence-acqrel-cross-space-add-asym.litmus", false},
            {"MP-fence-acqrel-cross-space-add.litmus", "MP-fence-acqrel-cross-space-add-fix.litmus", true},
            {"MP-fence-acq-rel.litmus", "MP-fence-acq-rel.litmus", true},
            {"RMW-release-sequence.litmus", "RMW-release-sequence.litmus", true},
            {"RMW-cas.litmus", "RMW-cas.litmus", true},
    };
    private final String targetPath;
    private final boolean expectedToMatch;
    public PtxToOpenCLTest(String sourcePath, String targetPath, boolean expectedToMatch) {
        super(sourcePath);
        this.targetPath = targetPath;
        this.expectedToMatch = expectedToMatch;
    }

    @Parameterized.Parameters(name = "{index}: {0} -> {1}")
    public static Iterable<Object[]> data() {
        return Arrays.stream(LITMUS_MAP)
                .map(e -> new Object[]{
                        getRootPath(PTX_DIR + e[0]),
                        getRootPath(OPENCL_DIR + e[1]),
                        e[2]
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
    protected List<String> getCompilationBreakers() {
        return expectedToMatch ? emptyList() : List.of(filePathProvider.get());
    }
}