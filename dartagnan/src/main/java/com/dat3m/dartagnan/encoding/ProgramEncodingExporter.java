package com.dat3m.dartagnan.encoding;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;

import com.dat3m.dartagnan.GlobalSettings;
import com.dat3m.dartagnan.program.Program;
import com.dat3m.dartagnan.program.analysis.ExecutionAnalysis;
import com.dat3m.dartagnan.program.event.Event;
import static com.dat3m.dartagnan.program.event.Tag.WRITE;
import com.dat3m.dartagnan.program.event.core.MemoryCoreEvent;
import com.dat3m.dartagnan.wmm.Relation;
import com.dat3m.dartagnan.wmm.Wmm;
import com.dat3m.dartagnan.wmm.analysis.RelationAnalysis;
import com.dat3m.dartagnan.wmm.definition.Coherence;
import com.dat3m.dartagnan.wmm.definition.ReadFrom;
import com.dat3m.dartagnan.wmm.utils.graph.EventGraph;

/*
 * Serializes the program-encoding output to a JSON file.
 *
 * The file captures:
 *   - events: id, thread, tags, and the SMT execution-variable name
 *   - relations: name, may-set, and must-set as lists of [e1_id, e2_id] pairs
 *   - rf_facts: per-(w,r) pair in rf's may-set: same_address and assign_value as SMT-LIB2 strings
 *   - co_facts: per-(w1,w2) write pair where either direction is in co's may-set: same_address as SMT-LIB2 string
 *   - exec_implications: [from_id, to_id] pairs where executing "from" implies "to" also executes
 */
public class ProgramEncodingExporter {

    private final EncodingContext context;

    public ProgramEncodingExporter(EncodingContext context) {
        this.context = context;
    }

    public static Path defaultExportPath(Program program) throws IOException {
        String stem = Path.of(program.getName()).getFileName().toString();
        int dot = stem.lastIndexOf('.');
        if (dot > 0) { stem = stem.substring(0, dot); }
        Path dir = Path.of(GlobalSettings.getOutputDirectory(), "encoding");
        Files.createDirectories(dir);
        return dir.resolve(stem + ".json");
    }

    public void export(Path outputPath) throws IOException {
        StringBuilder json = new StringBuilder("{\n");

        appendEvents(json);
        json.append(",\n");
        appendRelations(json);
        json.append(",\n");
        appendRfFacts(json);
        json.append(",\n");
        appendCoFacts(json);
        json.append(",\n");
        appendExecImplications(json);

        json.append("\n}\n");
        Files.writeString(outputPath, json.toString());
    }

    // -------------------------------------------------------------------------

    private void appendEvents(StringBuilder json) {
        List<Event> events = context.getTask().getProgram().getThreadEvents();
        json.append("  \"events\": [\n");
        for (int i = 0; i < events.size(); i++) {
            Event e = events.get(i);
            json.append("    {");
            json.append("\"id\": ").append(e.getGlobalId()).append(", ");
            json.append("\"thread\": ").append(e.getThread().getId()).append(", ");
            json.append("\"tags\": ").append(tagsJson(e.getTags())).append(", ");
            json.append("\"exec_var\": \"").append(jsonEscape(context.executionVarName(e))).append("\"");
            json.append("}");
            if (i < events.size() - 1) json.append(",");
            json.append("\n");
        }
        json.append("  ]");
    }

    private void appendRelations(StringBuilder json) {
        RelationAnalysis ra = context.getAnalysisContext().requires(RelationAnalysis.class);
        Wmm wmm = context.getTask().getMemoryModel();
        List<Relation> relations = new ArrayList<>(wmm.getRelations());

        json.append("  \"relations\": [\n");
        for (int i = 0; i < relations.size(); i++) {
            Relation rel = relations.get(i);
            RelationAnalysis.Knowledge k = ra.getKnowledge(rel);
            json.append("    {");
            json.append("\"name\": \"").append(jsonEscape(rel.getNameOrTerm())).append("\", ");
            json.append("\"may_set\": ").append(edgeListJson(k.getMaySet())).append(", ");
            json.append("\"must_set\": ").append(edgeListJson(k.getMustSet()));
            json.append("}");
            if (i < relations.size() - 1) json.append(",");
            json.append("\n");
        }
        json.append("  ]");
    }

    private void appendRfFacts(StringBuilder json) {
        RelationAnalysis ra = context.getAnalysisContext().requires(RelationAnalysis.class);
        BooleanFormulaManager bmgr = context.getBooleanFormulaManager();

        json.append("  \"rf_facts\": [\n");
        boolean[] first = {true};
        for (Relation rel : context.getTask().getMemoryModel().getRelations()) {
            if (!(rel.getDefinition() instanceof ReadFrom)) continue;
            ra.getKnowledge(rel).getMaySet().apply((e1, e2) -> {
                MemoryCoreEvent w = (MemoryCoreEvent) e1;
                MemoryCoreEvent r = (MemoryCoreEvent) e2;
                String sameAddr = formulaToSmtlib(context.sameAddress(w, r), bmgr);
                String assignVal = formulaToSmtlib(context.assignValue(r, w), bmgr);
                if (!first[0]) json.append(",\n");
                json.append("    {");
                json.append("\"w\": ").append(w.getGlobalId()).append(", ");
                json.append("\"r\": ").append(r.getGlobalId()).append(", ");
                json.append("\"same_address\": \"").append(jsonEscape(sameAddr)).append("\", ");
                json.append("\"assign_value\": \"").append(jsonEscape(assignVal)).append("\"");
                json.append("}");
                first[0] = false;
            });
            break;
        }
        json.append("\n  ]");
    }

    private void appendCoFacts(StringBuilder json) {
        RelationAnalysis ra = context.getAnalysisContext().requires(RelationAnalysis.class);
        BooleanFormulaManager bmgr = context.getBooleanFormulaManager();

        json.append("  \"co_facts\": [\n");
        boolean firstEntry = true;
        for (Relation rel : context.getTask().getMemoryModel().getRelations()) {
            if (!(rel.getDefinition() instanceof Coherence)) continue;
            EventGraph maySet = ra.getKnowledge(rel).getMaySet();
            List<MemoryCoreEvent> allWrites = context.getTask().getProgram()
                    .getThreadEvents(MemoryCoreEvent.class).stream()
                    .filter(e -> e.hasTag(WRITE))
                    .sorted(Comparator.comparingInt(Event::getGlobalId))
                    .toList();
            for (int i = 0; i < allWrites.size() - 1; i++) {
                MemoryCoreEvent w1 = allWrites.get(i);
                for (MemoryCoreEvent w2 : allWrites.subList(i + 1, allWrites.size())) {
                    if (!maySet.contains(w1, w2) && !maySet.contains(w2, w1)) continue;
                    String sameAddr = formulaToSmtlib(context.sameAddress(w1, w2), bmgr);
                    if (!firstEntry) json.append(",\n");
                    json.append("    {");
                    json.append("\"w1\": ").append(w1.getGlobalId()).append(", ");
                    json.append("\"w2\": ").append(w2.getGlobalId()).append(", ");
                    json.append("\"same_address\": \"").append(jsonEscape(sameAddr)).append("\"");
                    json.append("}");
                    firstEntry = false;
                }
            }
            break;
        }
        json.append("\n  ]");
    }

    private void appendExecImplications(StringBuilder json) {
        ExecutionAnalysis exec = context.getAnalysisContext().requires(ExecutionAnalysis.class);
        List<Event> events = context.getTask().getProgram().getThreadEvents();

        json.append("  \"exec_implications\": [\n");
        boolean first = true;
        for (int i = 0; i < events.size(); i++) {
            for (int j = 0; j < events.size(); j++) {
                if (i == j) continue;
                Event from = events.get(i);
                Event to = events.get(j);
                if (!exec.isImplied(from, to)) continue;
                if (!first) json.append(",\n");
                json.append("    [").append(from.getGlobalId()).append(", ").append(to.getGlobalId()).append("]");
                first = false;
            }
        }
        json.append("\n  ]");
    }

    // -------------------------------------------------------------------------

    private String formulaToSmtlib(BooleanFormula formula, BooleanFormulaManager bmgr) {
        if (bmgr.isTrue(formula)) return "true";
        if (bmgr.isFalse(formula)) return "false";
        String dump = context.getFormulaManager().getUnderlyingFormulaManager().dumpFormula(formula).toString().strip();
        int assertIdx = dump.lastIndexOf("(assert ");
        if (assertIdx >= 0) {
            String inner = dump.substring(assertIdx + "(assert ".length()).stripLeading();
            if (inner.endsWith(")")) {
                return inner.substring(0, inner.length() - 1).stripTrailing();
            }
        }
        return dump;
    }

    private static String edgeListJson(EventGraph graph) {
        StringBuilder sb = new StringBuilder("[");
        boolean[] first = {true};
        graph.apply((e1, e2) -> {
            if (!first[0]) sb.append(", ");
            sb.append("[").append(e1.getGlobalId()).append(", ").append(e2.getGlobalId()).append("]");
            first[0] = false;
        });
        return sb.append("]").toString();
    }

    private static String tagsJson(Set<String> tags) {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (String tag : tags) {
            if (!first) sb.append(", ");
            sb.append("\"").append(jsonEscape(tag)).append("\"");
            first = false;
        }
        return sb.append("]").toString();
    }

    private static String jsonEscape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
