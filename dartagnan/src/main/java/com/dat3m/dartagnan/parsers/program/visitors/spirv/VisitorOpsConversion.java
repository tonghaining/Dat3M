package com.dat3m.dartagnan.parsers.program.visitors.spirv;

import com.dat3m.dartagnan.exception.ParsingException;
import com.dat3m.dartagnan.expression.Expression;
import com.dat3m.dartagnan.expression.ExpressionFactory;
import com.dat3m.dartagnan.expression.type.ScopedPointerType;
import com.dat3m.dartagnan.expression.type.TypeFactory;
import com.dat3m.dartagnan.parsers.SpirvBaseVisitor;
import com.dat3m.dartagnan.parsers.SpirvParser;
import com.dat3m.dartagnan.parsers.program.visitors.spirv.builders.ProgramBuilder;
import com.dat3m.dartagnan.program.memory.MemoryObject;
import com.dat3m.dartagnan.program.memory.ScopedPointerVariable;
import com.dat3m.dartagnan.program.memory.VirtualMemoryObject;

import java.util.Set;

public class VisitorOpsConversion extends SpirvBaseVisitor<Void> {

    private static final TypeFactory types = TypeFactory.getInstance();
    private static final ExpressionFactory expressions = ExpressionFactory.getInstance();
    private final ProgramBuilder builder;

    public VisitorOpsConversion(ProgramBuilder builder) {
        this.builder = builder;
    }

    @Override
    public Void visitOpBitcast(SpirvParser.OpBitcastContext ctx) {
        String id = ctx.idResult().getText();
        String typeId = ctx.idResultType().getText();
        if (builder.getType(typeId) instanceof ScopedPointerType pointerType) {
            String operand = ctx.operand().getText();
            Expression operandExpr = builder.getExpression(operand);
            if (!(operandExpr instanceof ScopedPointerVariable scopedPointerVariable)) {
                throw new ParsingException("Type '%s' is not a pointer type", operand);
            }
            if (!pointerType.getScopeId().equals(scopedPointerVariable.getScopeId())) {
                throw new ParsingException("Storage class mismatch in OpBitcast between '%s' and '%s'", typeId, operand);
            }
            MemoryObject memObj = scopedPointerVariable.getAddress();
            ScopedPointerVariable pointer = expressions.makeScopedPointerVariable(
                    id, pointerType.getScopeId(), pointerType.getPointedType(), memObj);
            builder.addExpression(id, pointer);
            return null;
        } else {
            // TODO: Add support for scalar or vector of numerical-type bitcasts
            throw new ParsingException("Type '%s' is not a pointer type", typeId);
        }
    }

    public Set<String> getSupportedOps() {
        return Set.of(
                "OpBitcast"
        );
    }
}
