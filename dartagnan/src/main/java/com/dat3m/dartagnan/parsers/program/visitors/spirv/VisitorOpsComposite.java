package com.dat3m.dartagnan.parsers.program.visitors.spirv;

import com.dat3m.dartagnan.exception.ParsingException;
import com.dat3m.dartagnan.expression.Expression;
import com.dat3m.dartagnan.expression.ExpressionFactory;
import com.dat3m.dartagnan.expression.Type;
import com.dat3m.dartagnan.expression.aggregates.ConstructExpr;
import com.dat3m.dartagnan.parsers.SpirvBaseVisitor;
import com.dat3m.dartagnan.parsers.SpirvParser;
import com.dat3m.dartagnan.parsers.program.visitors.spirv.builders.ProgramBuilder;
import com.dat3m.dartagnan.program.event.Event;

import java.util.List;
import java.util.Set;

public class VisitorOpsComposite extends SpirvBaseVisitor<Event> {

    private final ProgramBuilder builder;

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
        Expression element = compositeExpression;
        for (Integer index : indexes) {
            element = ExpressionFactory.getInstance().makeExtract(index, element);
        }
        if (type != element.getType()) {
            throw new ParsingException("Type mismatch in composite extraction: %s", id);
        }
        builder.addExpression(id, element);
    }

    public Set<String> getSupportedOps() {
        return Set.of(
                "OpCompositeExtract"
        );
    }
}
