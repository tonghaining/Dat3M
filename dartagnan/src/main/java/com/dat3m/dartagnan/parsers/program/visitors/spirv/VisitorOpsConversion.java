package com.dat3m.dartagnan.parsers.program.visitors.spirv;

import com.dat3m.dartagnan.exception.ParsingException;
import com.dat3m.dartagnan.expression.Expression;
import com.dat3m.dartagnan.expression.ExpressionFactory;
import com.dat3m.dartagnan.expression.type.IntegerType;
import com.dat3m.dartagnan.expression.type.ScopedPointerType;
import com.dat3m.dartagnan.parsers.SpirvBaseVisitor;
import com.dat3m.dartagnan.parsers.SpirvParser;
import com.dat3m.dartagnan.parsers.program.visitors.spirv.builders.ProgramBuilder;
import com.dat3m.dartagnan.program.Register;
import com.dat3m.dartagnan.program.event.Tag;
import com.dat3m.dartagnan.program.event.core.Local;
import com.dat3m.dartagnan.program.memory.ScopedPointer;
import com.dat3m.dartagnan.program.memory.ScopedPointerVariable;

import java.util.Set;

public class VisitorOpsConversion extends SpirvBaseVisitor<Void> {

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
            Expression convertedPointer = expressions.makeCast(operandExpr, pointerType.getPointedType());
            Register reg = builder.addRegister(id, convertedPointer.getType());
            Local local = new Local(reg, convertedPointer);
            builder.addEvent(local);
            return null;
        } else {
            // TODO: Add support for scalar or vector of numerical-type bitcasts
            throw new ParsingException("Type '%s' is not a pointer type", typeId);
        }
    }

    @Override
    public Void visitOpConvertPtrToU(SpirvParser.OpConvertPtrToUContext ctx) {
        String id = ctx.idResult().getText();
        String typeId = ctx.idResultType().getText();
        if (!(builder.getType(typeId) instanceof IntegerType)) {
            throw new ParsingException("Type '%s' is not an integer type", typeId);
        }
        Expression pointerExpr = builder.getExpression(ctx.pointer().getText());
        Expression convertedPointer = expressions.makeCast(pointerExpr, builder.getType(typeId), false);
        Register reg = builder.addRegister(id, convertedPointer.getType());
        Local local = new Local(reg, convertedPointer);
        builder.addEvent(local);
        return null;
    }

    @Override
    public Void visitOpPtrCastToGeneric(SpirvParser.OpPtrCastToGenericContext ctx) {
        String id = ctx.idResult().getText();
        String typeId = ctx.idResultType().getText();
        if (!(builder.getType(typeId) instanceof ScopedPointerType targetPointerType)) {
            throw new ParsingException("Type '%s' is not a pointer type", typeId);
        }
        String pointer = ctx.pointer().getText();
        Expression originPointerExpr = builder.getExpression(pointer);
        String originStorageClass;
        if (originPointerExpr.getType() instanceof ScopedPointerType originPointerType) {
            originStorageClass = originPointerType.getScopeId();
        } else if (originPointerExpr instanceof ScopedPointer scopedPointer) {
            originStorageClass = scopedPointer.getScopeId();
        } else {
            throw new ParsingException("Type '%s' is not a pointer type", pointer);
        }
        if (!originStorageClass.equals(Tag.Spirv.SC_CROSS_WORKGROUP) &&
                !originStorageClass.equals(Tag.Spirv.SC_WORKGROUP) &&
                !originStorageClass.equals(Tag.Spirv.SC_FUNCTION)) {
            throw new ParsingException("Invalid storage class '%s' for OpPtrCastToGeneric", originStorageClass);
        }
        Expression convertedExpr = expressions.makeCast(originPointerExpr, targetPointerType);
        ScopedPointerVariable convertedPointer = builder.allocateScopedPointerVariable(id,
                convertedExpr, targetPointerType.getScopeId(), targetPointerType.getPointedType());
        builder.addExpression(id, convertedPointer);
        return null;
    }

    public Set<String> getSupportedOps() {
        return Set.of(
                "OpBitcast",
                "OpConvertPtrToU",
                "OpPtrCastToGeneric"
        );
    }
}
