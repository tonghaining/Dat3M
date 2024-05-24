package com.dat3m.testInfo;

import com.dat3m.dartagnan.configuration.Arch;
import com.dat3m.dartagnan.parsers.cat.ParserCat;
import com.dat3m.dartagnan.program.Program;
import com.dat3m.dartagnan.utils.Result;
import com.dat3m.dartagnan.verification.VerificationTask;
import com.dat3m.dartagnan.verification.solving.AssumeSolver;
import com.dat3m.dartagnan.verification.solving.ModelChecker;
import com.dat3m.dartagnan.wmm.Wmm;
import org.sosy_lab.common.configuration.Configuration;
import com.dat3m.dartagnan.configuration.Property;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.BasicLogManager;
import org.sosy_lab.java_smt.SolverContextFactory;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;
import org.sosy_lab.java_smt.api.SolverException;

import static com.dat3m.dartagnan.configuration.OptionNames.PHANTOM_REFERENCES;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ModelComparer {
    private final Arch arch;
    private final Wmm formalWmm;
    private final List<Wmm> wmmList;

    public ModelComparer(Arch arch, String formalWmmPath, String wmmMutationFolderPath) throws IOException {
        this.arch = arch;
        File formalWmmFile = new File(formalWmmPath);
        this.formalWmm = new ParserCat().parse(formalWmmFile);
        this.wmmList = new ArrayList<>();
        File folder = new File(wmmMutationFolderPath);
        File[] listOfFiles = folder.listFiles();
        if (listOfFiles != null) {
            for (File file : listOfFiles) {
                if (file.isFile()) {
                    wmmList.add(new ParserCat().parse(file));
                }
            }
        }
    }

    public Result getResult(Wmm wmm, Program program) throws InvalidConfigurationException, SolverException, InterruptedException {
        ModelChecker modelChecker;
        ShutdownManager sdm = ShutdownManager.create();
        VerificationTask task = VerificationTask.builder()
                .withBound(1)
                .withSolverTimeout(10) // 10s
                .withTarget(arch)
                .build(program, wmm, Property.getDefault());
        Configuration config = Configuration.builder()
                .setOption(PHANTOM_REFERENCES, "true")
                .build();
        try (SolverContext ctx = SolverContextFactory.createSolverContext(
                config,
                BasicLogManager.create(config),
                sdm.getNotifier(),
                SolverContextFactory.Solvers.Z3);
             ProverEnvironment prover = ctx.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {

            modelChecker = AssumeSolver.run(ctx, prover, task);
            return modelChecker.getResult();
        }
    }

    public List<Program> getInterestingProgram(List<Program> litmusPrograms) {
        List<Program> interestingPrograms = new ArrayList<>();
        for (Program program : litmusPrograms) {
            try {
                Result correctResult = getResult(formalWmm, program);
                for (Wmm wmm : wmmList) {
                    if (getResult(wmm, program) != correctResult) {
                        interestingPrograms.add(program);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return interestingPrograms;
    }
}
