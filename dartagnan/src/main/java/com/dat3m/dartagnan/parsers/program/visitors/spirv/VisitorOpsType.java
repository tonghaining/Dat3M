package com.dat3m.dartagnan.parsers.program.visitors.spirv;

import com.dat3m.dartagnan.exception.ParsingException;
import com.dat3m.dartagnan.expression.Expression;
import com.dat3m.dartagnan.expression.Type;
import com.dat3m.dartagnan.expression.integers.IntLiteral;
import com.dat3m.dartagnan.expression.type.TypeFactory;
import com.dat3m.dartagnan.parsers.SpirvBaseVisitor;
import com.dat3m.dartagnan.parsers.SpirvParser;
import com.dat3m.dartagnan.parsers.program.visitors.spirv.decorations.DecorationType;
import com.dat3m.dartagnan.parsers.program.visitors.spirv.decorations.Offset;
import com.dat3m.dartagnan.parsers.program.visitors.spirv.helpers.HelperTags;
import com.dat3m.dartagnan.parsers.program.visitors.spirv.builders.ProgramBuilder;

import java.util.*;
import java.util.stream.IntStream;

public class VisitorOpsType extends SpirvBaseVisitor<Type> {

    private static final TypeFactory types = TypeFactory.getInstance();

    private final ProgramBuilder builder;
    private final Offset offset;

    public VisitorOpsType(ProgramBuilder builder) {
        this.builder = builder;
        this.offset = (Offset) builder.getDecorationsBuilder().getDecoration(DecorationType.OFFSET);
    }

    @Override
    public Type visitOp(SpirvParser.OpContext ctx) {
        Type type = ctx.getChild(0).accept(this);
        if (type != null) {
            return type;
        }
        throw new ParsingException("Cannot parse operation '%s'", ctx.getText());
    }

    @Override
    public Type visitOpTypeVoid(SpirvParser.OpTypeVoidContext ctx) {
        return builder.addType(ctx.idResult().getText(), types.getVoidType());
    }

    @Override
    public Type visitOpTypeBool(SpirvParser.OpTypeBoolContext ctx) {
        return builder.addType(ctx.idResult().getText(), types.getBooleanType());
    }

    @Override
    public Type visitOpTypeInt(SpirvParser.OpTypeIntContext ctx) {
        // TODO: Signedness
        String id = ctx.idResult().getText();
        int size = Integer.parseInt(ctx.widthLiteralInteger().getText());
        Type type = types.getIntegerType(size);
        return builder.addType(id, type);
    }

    @Override
    public Type visitOpTypeVector(SpirvParser.OpTypeVectorContext ctx) {
        String id = ctx.idResult().getText();
        String elementTypeName = ctx.componentTypeIdRef().getText();
        Type elementType = builder.getType(elementTypeName);
        int size = Integer.parseInt(ctx.componentCountLiteralInteger().getText());
        Type type = types.getArrayType(elementType, size);
        return builder.addType(id, type);
    }

    @Override
    public Type visitOpTypeArray(SpirvParser.OpTypeArrayContext ctx) {
        String id = ctx.idResult().getText();
        String elementTypeName = ctx.elementType().getText();
        Type elementType = builder.getType(elementTypeName);
        String lengthValueName = ctx.lengthIdRef().getText();
        Expression lengthExpr = builder.getExpression(lengthValueName);
        if (lengthExpr != null) {
            if (lengthExpr instanceof IntLiteral iValue) {
                Type type = types.getArrayType(elementType, iValue.getValue().intValue());
                return builder.addType(id, type);
            }
            throw new ParsingException("Attempt to use a non-integer value as array size '%s'", lengthValueName);
        }
        throw new ParsingException("Reference to undefined expression '%s'", lengthValueName);
    }

    @Override
    public Type visitOpTypeRuntimeArray(SpirvParser.OpTypeRuntimeArrayContext ctx) {
        String id = ctx.idResult().getText();
        String elementTypeName = ctx.elementType().getText();
        Type elementType = builder.getType(elementTypeName);
        Type type = types.getArrayType(elementType);
        return builder.addType(id, type);
    }

    @Override
    public Type visitOpTypeStruct(SpirvParser.OpTypeStructContext ctx) {
        String id = ctx.idResult().getText();
        List<Type> memberTypes = ctx.memberType().stream()
                .map(memberCtx -> builder.getType(memberCtx.getText())).toList();
        Map<Integer, Integer> offsets = offset.getValue(id);
        if (offsets != null) {
            if (memberTypes.size() == offsets.size()) {
                List<Integer> memberOffsets = IntStream.range(0, offsets.size()).boxed().map(i -> {
                    if (!offsets.containsKey(i)) {
                        throw new ParsingException("Missing member offset decoration for struct '%s' index '%s'", id, i);
                    }
                    return offsets.get(i);
                }).toList();
                Type type = types.getAggregateType(memberTypes, memberOffsets);
                return builder.addType(id, type);
            }
            throw new ParsingException("Illegal member offset decorations for struct '%s'", id);
        }
        Type type = types.getAggregateType(memberTypes);
        return builder.addType(id, type);
    }

    @Override
    public Type visitOpTypePointer(SpirvParser.OpTypePointerContext ctx) {
        String id = ctx.idResult().getText();
        String inner = ctx.type().getText();
        String storageClass = HelperTags.parseStorageClass(ctx.storageClass().getText());
        Type type = types.getScopedPointerType(storageClass, builder.getType(inner));
        return builder.addType(id, type);
    }

    @Override
    public Type visitOpTypeFunction(SpirvParser.OpTypeFunctionContext ctx) {
        String id = ctx.idResult().getText();
        String returnTypeName = ctx.returnType().getText();
        Type returnType = builder.getType(returnTypeName);
        List<Type> argTypes = ctx.parameterType().stream()
                .map(argCtx -> builder.getType(argCtx.getText())).toList();
        Type type = types.getFunctionType(returnType, argTypes);
        return builder.addType(id, type);
    }

    public Set<String> getSupportedOps() {
        return Set.of(
                "OpTypeVoid",
                "OpTypeBool",
                "OpTypeInt",
                "OpTypeVector",
                "OpTypeArray",
                "OpTypeRuntimeArray",
                "OpTypeStruct",
                "OpTypePointer",
                "OpTypeFunction"
        );
    }
}
