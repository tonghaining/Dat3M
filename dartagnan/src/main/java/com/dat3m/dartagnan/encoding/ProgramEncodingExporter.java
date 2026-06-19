package com.dat3m.dartagnan.encoding;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.dat3m.dartagnan.GlobalSettings;
import com.dat3m.dartagnan.program.Program;
import com.dat3m.dartagnan.program.event.Event;
import com.dat3m.dartagnan.wmm.Relation;
import com.dat3m.dartagnan.wmm.Wmm;
import com.dat3m.dartagnan.wmm.analysis.RelationAnalysis;
import com.dat3m.dartagnan.wmm.utils.graph.EventGraph;

/*
 * Serializes the program-encoding output to a JSON file.
 *
 * The file captures:
 *   - events: id, thread, tags, and the SMT execution-variable name
 *   - relations: name, may-set, and must-set as lists of [e1_id, e2_id] pairs
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

    // -------------------------------------------------------------------------

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
