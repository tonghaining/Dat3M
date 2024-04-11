package com.dat3m.dartagnan.parsers.program.visitors.spirv;

import com.dat3m.dartagnan.exception.ParsingException;
import com.dat3m.dartagnan.expression.Expression;
import com.dat3m.dartagnan.expression.ExpressionFactory;
import com.dat3m.dartagnan.expression.Type;
import com.dat3m.dartagnan.expression.integers.IntBinaryOp;
import com.dat3m.dartagnan.expression.type.ArrayType;
import com.dat3m.dartagnan.expression.type.IntegerType;
import com.dat3m.dartagnan.parsers.SpirvBaseVisitor;
import com.dat3m.dartagnan.parsers.SpirvParser;
import com.dat3m.dartagnan.program.Register;
import com.dat3m.dartagnan.program.event.Event;
import com.dat3m.dartagnan.program.event.EventFactory;
import com.dat3m.dartagnan.program.event.core.Local;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class VisitorOpsBits extends SpirvBaseVisitor<Event> {

    private static final ExpressionFactory EXPR_FACTORY = ExpressionFactory.getInstance();

    private final ProgramBuilderSpv builder;

    public VisitorOpsBits(ProgramBuilderSpv builder) {
        this.builder = builder;
    }

    @Override
    public Event visitOpShiftLeftLogical(SpirvParser.OpShiftLeftLogicalContext ctx) {
        return visitShiftBinExpression(ctx.idResult(), ctx.idResultType(), ctx.base(), ctx.shift(), IntBinaryOp.LSHIFT);
    }

    @Override
    public Event visitOpShiftRightLogical(SpirvParser.OpShiftRightLogicalContext ctx) {
        return visitShiftBinExpression(ctx.idResult(), ctx.idResultType(), ctx.base(), ctx.shift(), IntBinaryOp.RSHIFT);
    }

    @Override
    public Event visitOpShiftRightArithmetic(SpirvParser.OpShiftRightArithmeticContext ctx) {
        return visitShiftBinExpression(ctx.idResult(), ctx.idResultType(), ctx.base(), ctx.shift(), IntBinaryOp.ARSHIFT);
    }

    @Override
    public Event visitOpBitwiseAnd(SpirvParser.OpBitwiseAndContext ctx) {
        return visitBitwiseExpression(ctx.idResult(), ctx.idResultType(), ctx.operand1(), ctx.operand2(), IntBinaryOp.AND);
    }

    @Override
    public Event visitOpBitwiseOr(SpirvParser.OpBitwiseOrContext ctx) {
        return visitBitwiseExpression(ctx.idResult(), ctx.idResultType(), ctx.operand1(), ctx.operand2(), IntBinaryOp.OR);
    }

    @Override
    public Event visitOpBitwiseXor(SpirvParser.OpBitwiseXorContext ctx) {
        return visitBitwiseExpression(ctx.idResult(), ctx.idResultType(), ctx.operand1(), ctx.operand2(), IntBinaryOp.XOR);
    }

    private Event visitShiftBinExpression(
            SpirvParser.IdResultContext idCtx,
            SpirvParser.IdResultTypeContext typeCtx,
            SpirvParser.BaseContext op1Ctx,
            SpirvParser.ShiftContext op2Ctx,
            IntBinaryOp op) {
        String id = idCtx.getText();
        Type type = builder.getType(typeCtx.getText());
        Expression op1 = builder.getExpression(op1Ctx.getText());
        Expression op2 = builder.getExpression(op2Ctx.getText());
        if (type instanceof IntegerType) {
            return createEventForIntegerType(id, typeCtx.getText(), op1, op2, op);
        }
        throw new ParsingException("Illegal result type for '%s'", id);
    }

    private Event visitBitwiseExpression(
            SpirvParser.IdResultContext idCtx,
            SpirvParser.IdResultTypeContext typeCtx,
            SpirvParser.Operand1Context op1Ctx,
            SpirvParser.Operand2Context op2Ctx,
            IntBinaryOp op) {
        String id = idCtx.getText();
        Type type = builder.getType(typeCtx.getText());
        Expression op1 = builder.getExpression(op1Ctx.getText());
        Expression op2 = builder.getExpression(op2Ctx.getText());
        if (type instanceof IntegerType) {
            return createEventForIntegerType(id, typeCtx.getText(), op1, op2, op);
        } else if (type instanceof ArrayType) {
            return createEventsForArrayType(id, typeCtx.getText(), op1, op2, op);
        }
        throw new ParsingException("Illegal result type for '%s'", id);
    }

    private Event createEventsForArrayType(String id, String type, Expression op1, Expression op2, IntBinaryOp op) {
        ArrayType arrayType = (ArrayType) builder.getType(type);
        checkTypeAlignment(id, arrayType, op1);
        checkTypeAlignment(id, arrayType, op2);
        List<Expression> results = new ArrayList<>();
        for (int i = 0; i < arrayType.getNumElements(); i++) {
            Expression op1Element = op1.getOperands().get(i);
            Expression op2Element = op2.getOperands().get(i);
            Expression resultElement = EXPR_FACTORY.makeBinary(op1Element, op, op2Element);
            results.add(resultElement);
        }
        Register register = builder.addRegister(id, type);
        Local event = EventFactory.newLocal(register, EXPR_FACTORY.makeArray(arrayType.getElementType(), results, true));
        return builder.addEvent(event);
    }

    private Event createEventForIntegerType(String id, String typeId, Expression op1, Expression op2, IntBinaryOp op) {
        Type idType = builder.getType(typeId);
        checkTypeAlignment(id, idType, op1);
        checkTypeAlignment(id, idType, op2);
        Register register = builder.addRegister(id, typeId);
        Local event = EventFactory.newLocal(register, EXPR_FACTORY.makeBinary(op1, op, op2));
        return builder.addEvent(event);
    }

    private void checkTypeAlignment(String id, Type idType, Expression op) {
        Type opType = op.getType();
        if (idType != opType) {
            throw new ParsingException("Illegal definition for '%s', " +
                    "operand '%s' must be of type '%s'", id, op, idType);
        }
    }

    public Set<String> getSupportedOps() {
        return Set.of(
                "OpShiftLeftLogical",
                "OpShiftRightLogical",
                "opShiftRightArithmetic",
                "OpBitwiseAnd",
                "OpBitwiseOr",
                "OpBitwiseXor"
        );
    }
}
