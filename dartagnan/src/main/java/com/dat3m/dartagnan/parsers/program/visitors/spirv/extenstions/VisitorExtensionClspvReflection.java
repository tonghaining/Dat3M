package com.dat3m.dartagnan.parsers.program.visitors.spirv.extenstions;

import com.dat3m.dartagnan.exception.ParsingException;
import com.dat3m.dartagnan.expression.Expression;
import com.dat3m.dartagnan.expression.ExpressionFactory;
import com.dat3m.dartagnan.expression.Type;
import com.dat3m.dartagnan.expression.integers.IntLiteral;
import com.dat3m.dartagnan.expression.type.*;
import com.dat3m.dartagnan.parsers.SpirvParser;
import com.dat3m.dartagnan.parsers.program.visitors.spirv.builders.ProgramBuilder;
import com.dat3m.dartagnan.parsers.program.visitors.spirv.utils.ThreadGrid;
import com.dat3m.dartagnan.program.event.Tag;
import com.dat3m.dartagnan.program.memory.ScopedPointerVariable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class VisitorExtensionClspvReflection extends VisitorExtension<Expression> {

    private static final TypeFactory types = TypeFactory.getInstance();
    private static final ExpressionFactory expressions = ExpressionFactory.getInstance();

    private final ProgramBuilder builder;
    private ScopedPointerVariable pushConstant;
    private AggregateType pushConstantType;

    public VisitorExtensionClspvReflection(ProgramBuilder builder) {
        this.builder = builder;
    }

    @Override
    public Expression visitKernel(SpirvParser.KernelContext ctx) {
        // Do nothing, kernel name and the number of arguments
        return null;
    }

    @Override
    public Expression visitArgumentInfo(SpirvParser.ArgumentInfoContext ctx) {
        // Do nothing, variable name in OpenCL
        return null;
    }

    @Override
    public Expression visitArgumentStorageBuffer(SpirvParser.ArgumentStorageBufferContext ctx) {
        // Do nothing, variable index in OpenCL and Spir-V and descriptor set
        return null;
    }

    @Override
    public Expression visitArgumentWorkgroup(SpirvParser.ArgumentWorkgroupContext ctx) {
        // Do nothing, default size of workgroup buffer defined in spec constant
        return null;
    }

    @Override
    public Expression visitSpecConstantWorkgroupSize(SpirvParser.SpecConstantWorkgroupSizeContext ctx) {
        // Do nothing, will be overwritten by BuiltIn WorkgroupSize
        return null;
    }

    @Override
    public Expression visitPushConstantGlobalOffset(SpirvParser.PushConstantGlobalOffsetContext ctx) {
        setPushConstantValue("PushConstantGlobalOffset", ctx.offsetIdRef().getText(), ctx.sizeIdRef().getText());
        return null;
    }

    @Override
    public Expression visitPushConstantGlobalSize(SpirvParser.PushConstantGlobalSizeContext ctx) {
        setPushConstantValue("PushConstantGlobalSize", ctx.offsetIdRef().getText(), ctx.sizeIdRef().getText());
        return null;
    }

    @Override
    public Expression visitPushConstantEnqueuedLocalSize(SpirvParser.PushConstantEnqueuedLocalSizeContext ctx) {
        setPushConstantValue("PushConstantEnqueuedLocalSize", ctx.offsetIdRef().getText(), ctx.sizeIdRef().getText());
        return null;
    }

    @Override
    public Expression visitPushConstantNumWorkgroups(SpirvParser.PushConstantNumWorkgroupsContext ctx) {
        setPushConstantValue("PushConstantNumWorkgroups", ctx.offsetIdRef().getText(), ctx.sizeIdRef().getText());
        return null;
    }

    @Override
    public Expression visitPushConstantRegionOffset(SpirvParser.PushConstantRegionOffsetContext ctx) {
        setPushConstantValue("PushConstantRegionOffset", ctx.offsetIdRef().getText(), ctx.sizeIdRef().getText());
        return null;
    }

    @Override
    public Expression visitPushConstantRegionGroupOffset(SpirvParser.PushConstantRegionGroupOffsetContext ctx) {
        setPushConstantValue("PushConstantRegionGroupOffset", ctx.offsetIdRef().getText(), ctx.sizeIdRef().getText());
        return null;
    }

    @Override
    public Expression visitArgumentPodPushConstant(SpirvParser.ArgumentPodPushConstantContext ctx) {
        initPushConstant();
        int argOffset = getExpressionAsConstInteger(ctx.offsetIdRef().getText());
        int argSize = getExpressionAsConstInteger(ctx.sizeIdRef().getText());
        getTypeOffset("ArgumentPodPushConstant", pushConstantType, argOffset, argSize);
        return null;
    }

    @Override
    public Expression visitSAddSat(SpirvParser.SAddSatContext ctx) {
        Expression x = getExpression(ctx.x().getText());
        Expression y = getExpression(ctx.y().getText());
        if (x.getType() instanceof IntegerType && y.getType() instanceof IntegerType) {
            return expressions.makeAdd(x, y);
        } else if (x.getType() instanceof ArrayType xType && y.getType() instanceof ArrayType yType) {
            if (xType.getNumElements() != yType.getNumElements() || !xType.getElementType().equals(yType.getElementType())) {
                throw new ParsingException("Array types must have the same number of elements");
            }
            List<Expression> sums = new ArrayList<>();
            for (int i = 0; i < xType.getNumElements(); i++) {
                sums.add(expressions.makeAdd(
                        expressions.makeExtract(i, x),
                        expressions.makeExtract(i, y)
                ));
            }
            return expressions.makeArray(xType.getElementType(), sums, true);
        }
        throw new ParsingException("Unsupported types for s_add_sat: %s and %s", x.getType(), y.getType());
    }

    @Override
    public Expression visitSSubSat(SpirvParser.SSubSatContext ctx) {
        Expression x = getExpression(ctx.x().getText());
        Expression y = getExpression(ctx.y().getText());
        if (x.getType() instanceof IntegerType && y.getType() instanceof IntegerType) {
            return expressions.makeSub(x, y);
        } else if (x.getType() instanceof ArrayType xType && y.getType() instanceof ArrayType yType) {
            if (xType.getNumElements() != yType.getNumElements() || !xType.getElementType().equals(yType.getElementType())) {
                throw new ParsingException("Array types must have the same number of elements");
            }
            List<Expression> subs = new ArrayList<>();
            for (int i = 0; i < xType.getNumElements(); i++) {
                subs.add(expressions.makeSub(
                        expressions.makeExtract(i, x),
                        expressions.makeExtract(i, y)
                ));
            }
            return expressions.makeArray(xType.getElementType(), subs, true);
        }
        throw new ParsingException("Unsupported types for s_sub_sat: %s and %s", x.getType(), y.getType());
    }

    private Void setPushConstantValue(String argument, String offsetId, String sizeId) {
        initPushConstant();
        int argOffset = getExpressionAsConstInteger(offsetId);
        int argSize = getExpressionAsConstInteger(sizeId);
        TypeOffset typeOffset = getTypeOffset(argument, pushConstantType, argOffset, argSize);
        if (typeOffset.type() instanceof ArrayType aType && aType.getNumElements() == 3) {
            Type elType = aType.getElementType();
            if (elType instanceof IntegerType iType) {
                int offset = typeOffset.offset();
                for (int value : computePushConstantValue(argument)) {
                    Expression elExpr = expressions.makeValue(value, iType);
                    pushConstant.setInitialValue(offset, elExpr);
                    offset += types.getMemorySizeInBytes(elExpr.getType());
                }
                return null;
            }
        }
        throw new ParsingException("Argument %s doesn't match the PushConstant type", argument);
    }

    private List<Integer> computePushConstantValue(String command) {
        ThreadGrid grid = builder.getThreadGrid();
        return switch (command) {
            case "PushConstantGlobalSize" -> List.of(grid.dvSize(), 1, 1);
            case "PushConstantEnqueuedLocalSize" -> List.of(grid.wgSize(), 1, 1);
            case "PushConstantNumWorkgroups" -> List.of(grid.qfSize() / grid.wgSize(), 1, 1);
            case "PushConstantGlobalOffset",
                    "PushConstantRegionOffset",
                    "PushConstantRegionGroupOffset" -> List.of(0, 0, 0);
            default -> throw new ParsingException("Unsupported PushConstant command '%s'", command);
        };
    }

    // TODO: Better way to identify PushConstant using kernel and arg info methods
    private void initPushConstant() {
        if (pushConstant == null) {
            List<ScopedPointerVariable> variables = builder.getVariables().stream()
                    .filter(v -> Tag.Spirv.SC_PUSH_CONSTANT.equals(v.getScopeId()))
                    .toList();
            if (variables.size() == 1) {
                pushConstant = variables.get(0);
                Type type = pushConstant.getInnerType();
                if (type instanceof AggregateType agType) {
                    pushConstantType = agType;
                    return;
                }
                throw new ParsingException("Unexpected type '%s' for PushConstant '%s'",
                        type, pushConstant.getId());
            }
            throw new ParsingException("Cannot identify PushConstant referenced by CLSPV extension");
        }
    }

    private int getExpressionAsConstInteger(String id) {
        Expression expression = builder.getExpression(id);
        if (expression instanceof IntLiteral iExpr) {
            return iExpr.getValueAsInt();
        }
        throw new ParsingException("Expression '%s' is not an integer constant", id);
    }

    private Expression getExpression(String id) {
        Expression expression = builder.getExpression(id);
        if (expression == null) {
            throw new ParsingException("Expression '%s' is not defined", id);
        }
        return expression;
    }

    private TypeOffset getTypeOffset(String argument, AggregateType type, int argOffset, int argSize) {
        TypeOffset lastOffset = null;
        for (TypeOffset typeOffset : type.getTypeOffsets()) {
            if (argOffset <= typeOffset.offset()) {
                if (argOffset == typeOffset.offset()) {
                    lastOffset = typeOffset;
                }
                break;
            }
            lastOffset = typeOffset;
        }
        if (lastOffset != null) {
            if (argOffset == lastOffset.offset() && argSize == types.getMemorySizeInBytes(lastOffset.type())) {
                return new TypeOffset(lastOffset.type(), lastOffset.offset());
            }
            if (lastOffset.type() instanceof AggregateType aType) {
                TypeOffset subTypeOffset = getTypeOffset(argument, aType, argOffset - lastOffset.offset(), argSize);
                return new TypeOffset(subTypeOffset.type(), lastOffset.offset() + subTypeOffset.offset());
            }
        }
        throw new ParsingException("Argument %s doesn't match the PushConstant type", argument);
    }

    @Override
    public Set<String> getSupportedInstructions() {
        return Set.of(
                "Kernel",
                "ArgumentInfo",
                "ArgumentStorageBuffer",
                "ArgumentWorkgroup",
                "ArgumentPodPushConstant",
                "PushConstantGlobalOffset",
                "PushConstantGlobalSize",
                "PushConstantEnqueuedLocalSize",
                "PushConstantNumWorkgroups",
                "PushConstantRegionOffset",
                "PushConstantRegionGroupOffset",
                "SpecConstantWorkgroupSize",
                "s_add_sat",
                "s_sub_sat"
        );
    }
}
