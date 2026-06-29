package com.dat3m.dartagnan.litmus.compilation;

import com.dat3m.dartagnan.configuration.Arch;
import com.dat3m.dartagnan.utils.rules.Provider;
import com.dat3m.dartagnan.utils.rules.Providers;
import com.dat3m.dartagnan.wmm.Wmm;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;

@RunWith(Parameterized.class)
public class PtxToOpenCLTest extends AbstractCompilationWithDrCheckTest {

    @Parameterized.Parameters(name = "{index}: {0}")
    public static Iterable<Object[]> data() throws IOException {
        return buildLitmusTests("litmus/PTX/Portability/");
    }

    public PtxToOpenCLTest(String path) {
        super(path);
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
        return () -> filePathProvider.get().replace("litmus/PTX/Portability", "litmus/OPENCL/Portability");
    }
}