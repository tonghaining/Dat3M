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

    // { OpenCL filename, Vulkan filename, expectedToMatch }
    private static final Object[][] LITMUS_MAP = {
            // {"BAR.litmus", "BAR.litmus", false}, // Data Race
            {"BAR.litmus", "BAR-fix1.litmus", true},
            {"BAR.litmus", "BAR-fix2.litmus", true},
            {"BAR.litmus", "BAR-fix3.litmus", true},
            // {"BAR.litmus", "BAR-fix4.litmus", true}, // Data Race
            {"SB-RMW-SC.litmus", "SB-RMW-SC.litmus", true},
            {"SB-fence-sc-relaxed.litmus", "SB-fence-SC.litmus", false}, // SC downgrade to AcqRel in Vulkan
            {"SB-fence-sc-relaxed.litmus", "SB-fence-SC-fix1.litmus", false},
    };
    private final String targetPath;
    private final boolean expectedToMatch;
    public OpenCLToVulkanTest(String sourcePath, String targetPath, boolean expectedToMatch) {
        super(sourcePath);
        this.targetPath = targetPath;
        this.expectedToMatch = expectedToMatch;
    }

    @Parameterized.Parameters(name = "{index}: {0} -> {1}")
    public static Iterable<Object[]> data() {
        return Arrays.stream(LITMUS_MAP)
                .map(e -> new Object[]{
                        getRootPath(OPENCL_DIR + e[0]),
                        getRootPath(VULKAN_DIR + e[1]),
                        e[2]
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
    protected List<String> getCompilationBreakers() {
        return expectedToMatch ? emptyList() : List.of(filePathProvider.get());
    }
}