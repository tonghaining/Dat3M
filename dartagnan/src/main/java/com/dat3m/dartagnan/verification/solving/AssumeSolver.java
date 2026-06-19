package com.dat3m.dartagnan.verification.solving;

import com.dat3m.dartagnan.configuration.Property;
import com.dat3m.dartagnan.encoding.*;
import com.dat3m.dartagnan.smt.ProverWithTracker;
import com.dat3m.dartagnan.utils.Result;
import com.dat3m.dartagnan.verification.Context;
import com.dat3m.dartagnan.verification.VerificationTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.SolverContext;
import org.sosy_lab.java_smt.api.SolverException;

import java.io.IOException;
import java.nio.file.Path;

import static com.dat3m.dartagnan.configuration.OptionNames.EXPORT_ENCODING_PATH;
import static com.dat3m.dartagnan.utils.Result.FAIL;
import static com.dat3m.dartagnan.utils.Result.PASS;
import static java.util.Collections.singletonList;

@Options
public class AssumeSolver extends ModelChecker {

    private static final Logger logger = LoggerFactory.getLogger(AssumeSolver.class);

    @Option(name = EXPORT_ENCODING_PATH,
            description = "Path for the program-encoding interface JSON file (events + relation may/must sets). " +
                    "Defaults to <output_dir>/encoding/<program_name>.json. " +
                    "Intended as input for an external WMM encoder (e.g. Rust).",
            secure = true)
    private String exportEncodingPath = "";

    private AssumeSolver(VerificationTask task) throws InvalidConfigurationException {
        super(task);
        task.getConfig().inject(this);
    }

    public static AssumeSolver create(VerificationTask task) throws InvalidConfigurationException {
        return new AssumeSolver(task);
    }

    protected Context preprocessAndAnalyse(VerificationTask task) throws InvalidConfigurationException {
        final Configuration config = task.getConfig();
        preprocessProgram(task, config);
        preprocessMemoryModel(task, config);

        final Context analysisContext = Context.create();
        performStaticProgramAnalyses(task, analysisContext, config);
        performStaticWmmAnalyses(task, analysisContext, config);
        performIntervalAnalysis(task, analysisContext, config);
        return analysisContext;
    }

    @Override
    protected void runInternal() throws InterruptedException, SolverException, InvalidConfigurationException {
        final Context analysisContext = preprocessAndAnalyse(task);

        initSMTSolver(task.getConfig());
        final SolverContext solverContext = this.solverContext;
        final ProverWithTracker prover = this.prover;

        context = EncodingContext.of(task, analysisContext, solverContext.getFormulaManager());
        ProgramEncoder programEncoder = ProgramEncoder.withContext(context);
        WmmEncoder wmmEncoder = WmmEncoder.withContext(context);
        PropertyEncoder propertyEncoder = PropertyEncoder.withContext(context, wmmEncoder);
        SymmetryEncoder symmetryEncoder = SymmetryEncoder.withContext(context);

        logger.info("Starting encoding using {}", solverContext.getVersion());
        prover.writeComment("Program encoding");
        prover.addConstraint(programEncoder.encodeFullProgram());

        try {
            Path exportPath = exportEncodingPath.isEmpty()
                    ? ProgramEncodingExporter.defaultExportPath(context.getTask().getProgram())
                    : Path.of(exportEncodingPath);
            new ProgramEncodingExporter(context).export(exportPath);
            logger.info("Exported program encoding interface to {}", exportPath);
        } catch (IOException e) {
            logger.warn("Failed to export program encoding interface: {}", e.getMessage());
        }

        prover.writeComment("Memory model encoding");
        prover.addConstraint(wmmEncoder.encodeFullMemoryModel());
        prover.writeComment("Symmetry breaking encoding");
        prover.addConstraint(symmetryEncoder.encodeFullSymmetryBreaking());

        BooleanFormulaManager bmgr = context.getBooleanFormulaManager();
        // Adding bounds
        prover.writeComment("Bounds over variables");
        prover.addConstraint(programEncoder.encodeBounds());
        BooleanFormula assumptionLiteral = bmgr.makeVariable("DAT3M_spec_assumption");
        BooleanFormula propertyEncoding = propertyEncoder.encodeProperties(task.getProperty());
        BooleanFormula assumedSpec = bmgr.implication(assumptionLiteral, propertyEncoding);
        prover.writeComment("Property encoding");
        prover.addConstraint(assumedSpec);

        checkForInterrupts();

        logger.info("Starting first solver.check()");
        if (prover.isUnsatWithAssumptions(singletonList(assumptionLiteral))) {
            checkForInterrupts();
            prover.writeComment("Bound encoding");
            prover.addConstraint(propertyEncoder.encodeBoundEventExec());
            logger.info("Starting second solver.check()");
            res = prover.isUnsat() ? PASS : Result.UNKNOWN;
        } else {
            res = FAIL;
        }

        if (logger.isDebugEnabled()) {
            logProverStatistics(logger, prover);
        }

        // For Safety specs, we have SAT=FAIL, but for reachability specs, we have SAT=PASS
        res = Property.getCombinedType(task.getProperty(), task) == Property.Type.SAFETY ? res : res.invert();
        logger.info("Verification finished with result {}", res);
    }
}