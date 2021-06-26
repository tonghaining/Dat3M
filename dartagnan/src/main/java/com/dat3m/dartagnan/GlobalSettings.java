package com.dat3m.dartagnan;

import com.microsoft.z3.Version;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GlobalSettings {
	
	private static final Logger logger = LogManager.getLogger(GlobalSettings.class);

    // === Parsing ===
    public static final boolean ATOMIC_AS_LOCK = false;

    // === WMM Assumptions ===
    public static final boolean ASSUME_LOCAL_CONSISTENCY = true;
    public static final boolean PERFORM_ATOMIC_BLOCK_OPTIMIZATION = true;

    // === Encoding ===
    public static final boolean FIXED_MEMORY_ENCODING = false;
    // NOTE: This ALLOW_PARTIAL_MODELS does NOT work on Litmus tests due to their different assertion condition
    public static final boolean ALLOW_PARTIAL_MODELS = false;
    public static final boolean MERGE_CF_VARS = true; // ONLY works if ALLOW_PARTIAL_MODELS is 'false'
    public static final boolean ANTISYMM_CO = false;
    public static final boolean ENABLE_SYMMETRY_BREAKING = true;

    // === BranchEquivalence ===
    public static final boolean MERGE_BRANCHES = true;
    public static final boolean ALWAYS_SPLIT_ON_JUMP = false;

    // === Static analysis ===
    public static final boolean PERFORM_DEAD_CODE_ELIMINATION = true;
    public static final boolean PERFORM_REORDERING = true;
    public static final boolean DETERMINISTIC_REORDERING = true;
    public static final boolean ENABLE_SYMMETRY_REDUCTION = true;

    // ==== Refinement ====
    public static final boolean REF_PRINT_STATISTICS = true;
    public static final boolean REF_USE_OUTER_WMM = true;
    public static final boolean REF_ADD_ACYCLIC_DEP_RF = false; // Only takes effect if REF_USE_OUTER_WMM is set to TRUE

    public enum SymmetryLearning { NONE, LINEAR, QUADRATIC, FULL }
    public static final SymmetryLearning REF_SYMMETRY_LEARNING = SymmetryLearning.FULL;

    // --------------------

    // === Recursion depth ===
    public static final int MAX_RECURSION_DEPTH = 200;

    // === Debug ===
    public static final boolean ENABLE_DEBUG_OUTPUT = true;

    // === Testing ===
    public static final boolean USE_BUGGY_ALIAS_ANALYSIS = false;

    public static void LogGlobalSettings() {
    	logger.info("Z3 version: " + Version.getFullVersion());
    	logger.info("ATOMIC_AS_LOCK: " + ATOMIC_AS_LOCK);
    	logger.info("ASSUME_LOCAL_CONSISTENCY: " + ASSUME_LOCAL_CONSISTENCY);
    	logger.info("PERFORM_ATOMIC_BLOCK_OPTIMIZATION: " + PERFORM_ATOMIC_BLOCK_OPTIMIZATION);
    	logger.info("MERGE_CF_VARS: " + MERGE_CF_VARS);
    	logger.info("ANTISYMM_CO: " + ANTISYMM_CO);
    	logger.info("MERGE_BRANCHES: " + MERGE_BRANCHES);
    	logger.info("ALWAYS_SPLIT_ON_JUMP: " + ALWAYS_SPLIT_ON_JUMP);
    	logger.info("PERFORM_DEAD_CODE_ELIMINATION: " + PERFORM_DEAD_CODE_ELIMINATION);
    	logger.info("PERFORM_REORDERING: " + PERFORM_REORDERING);
    	logger.info("ENABLE_SYMMETRY_BREAKING: " + ENABLE_SYMMETRY_BREAKING);
        logger.info("ENABLE_SYMMETRY_REDUCTION: " + ENABLE_SYMMETRY_REDUCTION);
    	logger.info("MAX_RECURSION_DEPTH: " + MAX_RECURSION_DEPTH);
    	logger.info("ENABLE_DEBUG_OUTPUT: " + ENABLE_DEBUG_OUTPUT);
    	logger.info("USE_BUGGY_ALIAS_ANALYSIS: " + USE_BUGGY_ALIAS_ANALYSIS);
    }
}
