package com.dat3m.dartagnan.parsers.program.visitors.spirv;

import com.dat3m.dartagnan.expression.Expression;
import com.dat3m.dartagnan.expression.ExpressionFactory;
import com.dat3m.dartagnan.expression.Type;
import com.dat3m.dartagnan.expression.aggregates.ConstructExpr;
import com.dat3m.dartagnan.parsers.SpirvBaseVisitor;
import com.dat3m.dartagnan.parsers.SpirvParser;
import com.dat3m.dartagnan.parsers.program.visitors.spirv.builders.ProgramBuilder;
import com.dat3m.dartagnan.program.Register;
import com.dat3m.dartagnan.program.event.Event;
import com.dat3m.dartagnan.program.event.core.Local;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class VisitorOpsComposite extends SpirvBaseVisitor<Event> {

    private final ProgramBuilder builder;

    private static final ExpressionFactory expressions = ExpressionFactory.getInstance();

    public VisitorOpsComposite(ProgramBuilder builder) {
        this.builder = builder;
    }

    @Override
    public Event visitOpCompositeExtract(SpirvParser.OpCompositeExtractContext ctx) {
        extractCompositeElement(ctx.idResult().getText(), ctx.idResultType().getText(),
                ctx.composite().getText(), ctx.indexesLiteralInteger());
        return null;
    }

    private void extractCompositeElement(String id, String typeId, String compositeId,
                                    List<SpirvParser.IndexesLiteralIntegerContext> idxContexts) {
        Expression compositeExpression = builder.getExpression(compositeId);
        Type type = builder.getType(typeId);
        List<Integer> indexes = idxContexts.stream()
                .map(SpirvParser.IndexesLiteralIntegerContext::getText)
                .map(Integer::parseInt)
                .toList();
        Expression extractedExpression = expressions.makeExtract(indexes.get(0), compositeExpression);
        for (int i = 1; i < indexes.size(); i++) {
            extractedExpression = expressions.makeExtract(indexes.get(i), extractedExpression);
        }
        if (extractedExpression.getType() != type) {
            throw new UnsupportedOperationException("Type mismatch in OpCompositeExtract");
        }
        Register register = builder.addRegister(id, extractedExpression.getType());
        Local local = new Local(register, extractedExpression);
        builder.addEvent(local);
    }

    public Set<String> getSupportedOps() {
        return Set.of(
                "OpCompositeExtract"
        );
    }
}
