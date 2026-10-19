package com.dat3m.dartagnan.litmus.compilation;

import com.dat3m.dartagnan.configuration.Arch;
import com.dat3m.dartagnan.utils.rules.Provider;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.dat3m.dartagnan.utils.ResourceHelper.getRootPath;
import static java.util.Collections.emptyList;

@RunWith(Parameterized.class)
public class OpenCLToVulkanTest extends AbstractCompilationWithDrCheckTest {

    private static final String OPENCL_DIR = "litmus/OPENCL/Portability/";
    private static final String VULKAN_DIR = "litmus/VULKAN/Portability/";

    // { OpenCL filename, Vulkan filename, expectDataRace, expectedToMatch }
    // expectedToMatch is omitted when expectDataRace is true, since it is ignored in that case.
    private static final Object[][] LITMUS_MAP = {
            // Av/Vis mismatches
            // Fix1: Sync-AvVis
            // Fix2: Access-Full-AvVis
            // Fix3: Adjacent-Access-AvVis
            {"CoWR-barrier.litmus", "CoWR-barrier-naive.litmus", true},
            {"CoWR-barrier.litmus", "CoWR-barrier-fix1.litmus", false, true},
            {"CoWR-barrier.litmus", "CoWR-barrier-fix2.litmus", false, true},
            {"CoWR-barrier.litmus", "CoWR-barrier-fix3.litmus", false, true},
            {"CoWR-barrier.litmus", "CoWR-barrier-fix4.litmus", true}, // without .nonpriv for the non-adjacent stores and loads
            {"MP-rel-acq.litmus", "MP-rel-acq-naive.litmus", true},
            {"MP-rel-acq.litmus", "MP-rel-acq-fix1.litmus", false, true},
            {"MP-rel-acq.litmus", "MP-rel-acq-fix2+3.litmus", false, true},
            {"MP-fence-rel-acq.litmus", "MP-fence-rel-acq-naive.litmus", true},
            {"MP-fence-rel-acq.litmus", "MP-fence-rel-acq-fix1.litmus", false, true},
            {"MP-fence-rel-acq.litmus", "MP-fence-rel-acq-fix2+3.litmus", false, true},

            // Local memory (sc1 / semsc1)
            {"CoWR-barrier-local.litmus", "CoWR-barrier-local-naive.litmus", true},
            {"CoWR-barrier-local.litmus", "CoWR-barrier-local-fix1.litmus", false, true},
            {"CoWR-barrier-local.litmus", "CoWR-barrier-local-fix2.litmus", false, true},
            {"CoWR-barrier-local.litmus", "CoWR-barrier-local-fix3.litmus", false, true},
            {"MP-rel-acq-local.litmus", "MP-rel-acq-local-naive.litmus", true},
            {"MP-rel-acq-local.litmus", "MP-rel-acq-local-fix1.litmus", false, true},
            {"MP-rel-acq-local.litmus", "MP-rel-acq-local-fix2+3.litmus", false, true},
            {"MP-fence-rel-acq-local.litmus", "MP-fence-rel-acq-local-naive.litmus", true},
            {"MP-fence-rel-acq-local.litmus", "MP-fence-rel-acq-local-fix1.litmus", false, true},
            {"MP-fence-rel-acq-local.litmus", "MP-fence-rel-acq-local-fix2+3.litmus", false, true},

            // Cross work-group, device scope
            {"MP-rel-acq-cross-wg.litmus", "MP-rel-acq-cross-wg-naive.litmus", true},
            {"MP-rel-acq-cross-wg.litmus", "MP-rel-acq-cross-wg-fix1.litmus", false, true},
            {"MP-rel-acq-cross-wg.litmus", "MP-rel-acq-cross-wg-fix2+3.litmus", false, true},
            {"MP-fence-rel-acq-cross-wg.litmus", "MP-fence-rel-acq-cross-wg-naive.litmus", true},
            {"MP-fence-rel-acq-cross-wg.litmus", "MP-fence-rel-acq-cross-wg-fix1.litmus", false, true},
            {"MP-fence-rel-acq-cross-wg.litmus", "MP-fence-rel-acq-cross-wg-fix2+3.litmus", false, true},

            // Cross space: local data (sc1), global flag (sc0), fences with semsc0.semsc1
            {"MP-fence-rel-acq-cross-space.litmus", "MP-fence-rel-acq-cross-space-naive.litmus", true},
            {"MP-fence-rel-acq-cross-space.litmus", "MP-fence-rel-acq-cross-space-fix1.litmus", false, true},
            {"MP-fence-rel-acq-cross-space.litmus", "MP-fence-rel-acq-cross-space-fix2+3.litmus", false, true},

            // SC downgrade to AcqRel in Vulkan
            {"SB-fence-sc.litmus", "SB-fence-sc-naive.litmus", false, false}, // SC downgrade to AcqRel in Vulkan
            {"SB-fence-sc.litmus", "SB-fence-sc-fix1.litmus", false, false}, // Make Store/Load to Rel/Acq, but still fail
    };
    private final String targetPath;
    private final boolean expectDataRace;
    private final boolean expectedToMatch;
    public OpenCLToVulkanTest(String sourcePath, String targetPath, boolean expectDataRace, boolean expectedToMatch) {
        super(sourcePath);
        this.targetPath = targetPath;
        this.expectDataRace = expectDataRace;
        this.expectedToMatch = expectedToMatch;
    }

    @Parameterized.Parameters(name = "{index}: {0} -> {1}")
    public static Iterable<Object[]> data() {
        return Arrays.stream(LITMUS_MAP)
                .map(e -> new Object[]{
                        getRootPath(OPENCL_DIR + e[0]),
                        getRootPath(VULKAN_DIR + e[1]),
                        e[2],
                        e.length > 3 ? e[3] : false
                })
                .collect(Collectors.toList());
    }

    @Override
    protected Provider<Arch> getSourceProvider() {
        return () -> Arch.OPENCL;
    }

    @Override
    protected Provider<Arch> getTargetProvider() {
        return () -> Arch.VULKAN;
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