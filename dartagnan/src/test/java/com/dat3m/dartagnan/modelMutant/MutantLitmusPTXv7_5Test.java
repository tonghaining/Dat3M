package com.dat3m.dartagnan.modelMutant;

import com.dat3m.dartagnan.configuration.Arch;
import com.dat3m.dartagnan.utils.rules.Provider;
import com.dat3m.dartagnan.utils.rules.Providers;
import com.dat3m.dartagnan.wmm.Wmm;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;

@RunWith(Parameterized.class)
public class MutantLitmusPTXv7_5Test extends MutationCompareTest {

    @Parameterized.Parameters(name = "{index}: {0}")
    public static Iterable<Object[]> data() throws IOException {
        return buildLitmusTests("litmus/PTX/Nvidia/");
    }

    @Override
    protected Provider<Arch> getTargetProvider() {
        return () -> Arch.PTX;
    }

    public MutantLitmusPTXv7_5Test(String path) {
        super(path);
    }

    @Override
    protected Provider<Wmm> getOriginalWmmProvider() {
        return Providers.createWmmFromName(() -> "ptx-v7.5");
    }

    @Override
    protected Provider<Wmm> getMutantWmmProvider() {
        return Providers.createWmmFromName(() -> "ptx-v6.0");
    }
}
