package com.dat3m.dartagnan.modelMutant;

import com.dat3m.dartagnan.configuration.Arch;
import com.dat3m.dartagnan.configuration.Property;
import com.dat3m.dartagnan.program.Program;
import com.dat3m.dartagnan.utils.ResourceHelper;
import com.dat3m.dartagnan.utils.rules.Provider;
import com.dat3m.dartagnan.utils.rules.Providers;
import com.dat3m.dartagnan.utils.rules.RequestShutdownOnError;
import com.dat3m.dartagnan.verification.VerificationTask;
import com.dat3m.dartagnan.verification.solving.AssumeSolver;
import com.dat3m.dartagnan.wmm.Wmm;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.Timeout;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.java_smt.SolverContextFactory;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static com.dat3m.dartagnan.configuration.OptionNames.INITIALIZE_REGISTERS;
import static com.dat3m.dartagnan.configuration.OptionNames.USE_INTEGERS;
import static com.dat3m.dartagnan.utils.ResourceHelper.getRootPath;
import static com.google.common.io.Files.getNameWithoutExtension;
import static org.junit.Assert.assertEquals;

public abstract class MutationCompareTest {
    private String testPath;
    private String mutantModel;
    private static Map<String, Boolean> compareResults;

    MutationCompareTest(String testPath) {
        this.testPath = testPath;
    }

    static Iterable<Object[]> buildLitmusTests(String litmusPath) throws IOException {
        Set<String> skip = ResourceHelper.getSkipSet();

        try (Stream<Path> fileStream = Files.walk(Paths.get(getRootPath(litmusPath)))) {
            return fileStream
                    .filter(Files::isRegularFile)
                    .map(Path::toString)
                    .filter(f -> f.endsWith("litmus"))
                    .filter(f -> !skip.contains(f))
                    .collect(ArrayList::new,
                            (l, f) -> l.add(new Object[]{f}), ArrayList::addAll);
        }
    }

    // =================== Modifiable behavior ====================

    protected abstract Provider<Arch> getTargetProvider();

    protected Provider<Wmm> getOriginalWmmProvider() {
        return Providers.createWmmFromArch(getTargetProvider());
    }

    protected Provider<Wmm> getMutantWmmProvider() {
        return Providers.createWmmFromArch(getTargetProvider());
    }

    protected Provider<EnumSet<Property>> getPropertyProvider() {
        return Provider.fromSupplier(() -> EnumSet.of(Property.PROGRAM_SPEC));
    }

    protected Provider<Configuration> getConfigurationProvider() {
        return Provider.fromSupplier(() -> Configuration.builder()
                .setOption(INITIALIZE_REGISTERS, "true")
                .setOption(USE_INTEGERS, "true")
                .build());
    }

    protected Provider<Integer> getBoundProvider() {
        return Provider.fromSupplier(() -> 1);
    }

    protected long getTimeout() {
        return 10000;
    }

    // ============================================================

    protected final Provider<ShutdownManager> shutdownManagerProvider = Provider.fromSupplier(ShutdownManager::create);
    protected final Provider<Arch> targetProvider = getTargetProvider();
    protected final Provider<String> filePathProvider = () -> testPath;
    protected final Provider<String> nameProvider = Provider.fromSupplier(() -> getNameWithoutExtension(Path.of(testPath).getFileName().toString()));
    protected final Provider<Integer> boundProvider = getBoundProvider();
    protected final Provider<Program> programProvider = Providers.createProgramFromPath(filePathProvider);
    protected final Provider<Program> program2Provider = Providers.createProgramFromPath(filePathProvider);
    protected final Provider<Wmm> originalWmmProvider = getOriginalWmmProvider();
    protected final Provider<Wmm> mutantWmmProvider = getMutantWmmProvider();
    protected final Provider<EnumSet<Property>> propertyProvider = getPropertyProvider();
    protected final Provider<Configuration> configProvider = getConfigurationProvider();
    protected final Provider<VerificationTask> originalTaskProvider = Providers.createTask(programProvider, originalWmmProvider, propertyProvider, targetProvider, boundProvider, configProvider);
    protected final Provider<VerificationTask> mutantTaskProvider = Providers.createTask(program2Provider, mutantWmmProvider, propertyProvider, targetProvider, boundProvider, configProvider);
    protected final Provider<SolverContext> contextProvider = Providers.createSolverContextFromManager(shutdownManagerProvider, () -> SolverContextFactory.Solvers.Z3);
    protected final Provider<SolverContext> context2Provider = Providers.createSolverContextFromManager(shutdownManagerProvider, () -> SolverContextFactory.Solvers.Z3);
    protected final Provider<ProverEnvironment> proverProvider = Providers.createProverWithFixedOptions(contextProvider, SolverContext.ProverOptions.GENERATE_MODELS);
    protected final Provider<ProverEnvironment> prover2Provider = Providers.createProverWithFixedOptions(context2Provider, SolverContext.ProverOptions.GENERATE_MODELS);

    private final Timeout timeout = Timeout.millis(getTimeout());
    private final RequestShutdownOnError shutdownOnError = RequestShutdownOnError.create(shutdownManagerProvider);

    @Rule
    public RuleChain ruleChain = RuleChain.outerRule(shutdownManagerProvider)
            .around(shutdownOnError)
            .around(filePathProvider)
            .around(nameProvider)
            .around(boundProvider)
            .around(programProvider)
            .around(program2Provider)
            .around(originalWmmProvider)
            .around(mutantWmmProvider)
            .around(propertyProvider)
            .around(configProvider)
            .around(originalTaskProvider)
            .around(mutantTaskProvider)
            .around(timeout)
            // Context/Prover need to be created inside test-thread spawned by <timeout>
            .around(contextProvider)
            .around(context2Provider)
            .around(proverProvider)
            .around(prover2Provider);


    @Test
    public void testAssume() throws Exception {
        AssumeSolver originalSolver = AssumeSolver.run(contextProvider.get(), proverProvider.get(), originalTaskProvider.get());
        AssumeSolver mutantSolver = AssumeSolver.run(context2Provider.get(), prover2Provider.get(), mutantTaskProvider.get());
        assertEquals(originalSolver.getResult(), mutantSolver.getResult());
    }
}
