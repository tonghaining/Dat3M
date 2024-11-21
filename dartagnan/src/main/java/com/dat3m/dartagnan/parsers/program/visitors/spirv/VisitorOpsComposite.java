package com.dat3m.dartagnan.parsers.program.visitors.spirv;

import com.dat3m.dartagnan.expression.Expression;
import com.dat3m.dartagnan.expression.ExpressionFactory;
import com.dat3m.dartagnan.expression.Type;
import com.dat3m.dartagnan.expression.type.AggregateType;
import com.dat3m.dartagnan.expression.type.ArrayType;
import com.dat3m.dartagnan.expression.type.TypeFactory;
import com.dat3m.dartagnan.parsers.SpirvBaseVisitor;
import com.dat3m.dartagnan.parsers.SpirvParser;
import com.dat3m.dartagnan.parsers.program.visitors.spirv.builders.ProgramBuilder;
import com.dat3m.dartagnan.program.event.Event;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class VisitorOpsComposite extends SpirvBaseVisitor<Event> {

    private static final ExpressionFactory expressions = ExpressionFactory.getInstance();
    private static final TypeFactory types = TypeFactory.getInstance();

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
        int index = Integer.parseInt(
                idxContexts.stream()
                        .map(SpirvParser.IndexesLiteralIntegerContext::getText)
                        .collect(Collectors.joining())
        );
        if (compositeExpression.getType() instanceof ArrayType arrayType) {
            if (type != arrayType.getElementType()) {
                throw new UnsupportedOperationException("Composite tye mismatch between array and element type");
            }
        } else if (compositeExpression.getType() instanceof AggregateType aggregateType) {
            if (type != aggregateType.getTypeOffsets().get(index).type()) {
                throw new UnsupportedOperationException("Composite tye mismatch between aggregate and element type");
            }
        } else {
            throw new UnsupportedOperationException("Composite type not supported");
        }
        Expression extractExpression = expressions.makeExtract(index, compositeExpression);
        builder.addExpression(id, extractExpression);
    }

    public Set<String> getSupportedOps() {
        return Set.of(
                "OpCompositeExtract"
        );
    }
}
