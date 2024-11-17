package com.dat3m.dartagnan.parsers.program.visitors.spirv;

import com.dat3m.dartagnan.exception.ParsingException;
import com.dat3m.dartagnan.expression.Expression;
import com.dat3m.dartagnan.expression.ExpressionFactory;
import com.dat3m.dartagnan.expression.Type;
import com.dat3m.dartagnan.expression.integers.IntLiteral;
import com.dat3m.dartagnan.expression.type.AggregateType;
import com.dat3m.dartagnan.expression.type.ArrayType;
import com.dat3m.dartagnan.expression.type.IntegerType;
import com.dat3m.dartagnan.expression.type.TypeFactory;
import com.dat3m.dartagnan.parsers.SpirvBaseVisitor;
import com.dat3m.dartagnan.parsers.SpirvParser;
import com.dat3m.dartagnan.parsers.program.visitors.spirv.builders.ProgramBuilder;
import com.dat3m.dartagnan.parsers.program.visitors.spirv.helpers.HelperTypes;
import com.dat3m.dartagnan.program.event.Event;
import com.dat3m.dartagnan.program.memory.MemoryObject;
import com.dat3m.dartagnan.program.memory.ScopedPointer;
import com.dat3m.dartagnan.program.memory.ScopedPointerVariable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
        if (!(compositeExpression instanceof ScopedPointerVariable scopedPointerVariable)) {
            throw new ParsingException("Invalid composite '%s'", compositeId);
        }
        List<Integer> intIndexes = new ArrayList<>();
        List<Expression> exprIndexes = new ArrayList<>();
        idxContexts.forEach(c -> {
            IntLiteral expression = expressions.parseValue(c.getText(), (IntegerType) types.getPointerType());
            exprIndexes.add(expression);
            intIndexes.add(expression.getValueAsInt());
        });
        Type runtimeResultType = HelperTypes.getMemberType(compositeId, scopedPointerVariable.getInnerType(), intIndexes);
        if (builder.getType(typeId) != runtimeResultType) {
            throw new ParsingException("Invalid type '%s' for composite '%s'", typeId, compositeId);
        }
        Expression expression = HelperTypes.getMemberAddress(compositeId, scopedPointerVariable, scopedPointerVariable.getInnerType(), exprIndexes);
        ScopedPointer pointer = expressions.makeScopedPointer(id, scopedPointerVariable.getScopeId(), runtimeResultType, expression);
        builder.addExpression(id, pointer);
    }

    public Set<String> getSupportedOps() {
        return Set.of(
                "OpCompositeExtract"
        );
    }
}
