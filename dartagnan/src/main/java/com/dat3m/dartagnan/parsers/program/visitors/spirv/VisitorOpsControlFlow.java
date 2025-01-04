package com.dat3m.dartagnan.parsers.program.visitors.spirv;

import com.dat3m.dartagnan.exception.ParsingException;
import com.dat3m.dartagnan.expression.Expression;
import com.dat3m.dartagnan.expression.ExpressionFactory;
import com.dat3m.dartagnan.expression.Type;
import com.dat3m.dartagnan.expression.integers.IntLiteral;
import com.dat3m.dartagnan.expression.type.IntegerType;
import com.dat3m.dartagnan.expression.type.TypeFactory;
import com.dat3m.dartagnan.parsers.SpirvBaseVisitor;
import com.dat3m.dartagnan.parsers.SpirvParser;
import com.dat3m.dartagnan.parsers.program.visitors.spirv.builders.ControlFlowBuilder;
import com.dat3m.dartagnan.parsers.program.visitors.spirv.builders.ProgramBuilder;
import com.dat3m.dartagnan.program.Register;
import com.dat3m.dartagnan.program.event.Event;
import com.dat3m.dartagnan.program.event.EventFactory;
import com.dat3m.dartagnan.program.event.Tag;
import com.dat3m.dartagnan.program.event.core.Alloc;
import com.dat3m.dartagnan.program.event.core.CondJump;
import com.dat3m.dartagnan.program.event.core.Label;
import com.dat3m.dartagnan.program.event.functions.Return;
import com.dat3m.dartagnan.program.memory.ScopedPointerVariable;
import com.dat3m.dartagnan.program.memory.VirtualMemoryObject;

import java.math.BigInteger;
import java.util.*;

import static com.dat3m.dartagnan.program.event.EventFactory.newFunctionReturn;

public class VisitorOpsControlFlow extends SpirvBaseVisitor<Event> {

    private static final TypeFactory types = TypeFactory.getInstance();
    private static final ExpressionFactory expressions = ExpressionFactory.getInstance();
    private final ProgramBuilder builder;
    private final ControlFlowBuilder cfBuilder;
    private String continueLabelId;
    private String mergeLabelId;
    private String nextLabelId;

    public VisitorOpsControlFlow(ProgramBuilder builder) {
        this.builder = builder;
        this.cfBuilder = builder.getControlFlowBuilder();
    }

    @Override
    public Event visitOpPhi(SpirvParser.OpPhiContext ctx) {
        String id = ctx.idResult().getText();
        String typeId = ctx.idResultType().getText();
        Register register = builder.addRegister(id, typeId);
        for (SpirvParser.VariableContext vCtx : ctx.variable()) {
            SpirvParser.PairIdRefIdRefContext pCtx = vCtx.pairIdRefIdRef();
            String labelId = pCtx.idRef(1).getText();
            String expressionId = pCtx.idRef(0).getText();
            cfBuilder.addPhiDefinition(labelId, register, expressionId);
            cfBuilder.setPhiId(labelId, register, id);
        }
        builder.addExpression(id, register);
        cfBuilder.setPhiLocation(id);
        return null;
    }

    @Override
    public Event visitOpLabel(SpirvParser.OpLabelContext ctx) {
        if (nextLabelId != null) {
            if (!nextLabelId.equals(ctx.idResult().getText())) {
                throw new ParsingException("Illegal label, expected '%s' but received '%s'",
                        nextLabelId, ctx.idResult().getText());
            }
            nextLabelId = null;
        }
        String labelId = ctx.idResult().getText();
        Label event = cfBuilder.getOrCreateLabel(labelId);
        cfBuilder.startBlock(labelId);
        return builder.addEvent(event);
    }

    @Override
    public Event visitOpSelectionMerge(SpirvParser.OpSelectionMergeContext ctx) {
        if (mergeLabelId == null) {
            mergeLabelId = ctx.mergeBlock().getText();
            builder.setNextOps(Set.of("OpBranchConditional", "OpSwitch"));
            return null;
        }
        throw new ParsingException("End label must be null");
    }

    @Override
    public Event visitOpSwitch(SpirvParser.OpSwitchContext ctx) {
        Expression selector = builder.getExpression(ctx.selector().getText());
        if (!(selector.getType() instanceof IntegerType integerType)) {
            throw new ParsingException("Switch selector must be an integer type");
        }
        String defaultLabelId = ctx.defaultLabel().getText();
        Label defaultLabel = cfBuilder.getOrCreateLabel(defaultLabelId);
        Map<Expression, Label> cases = new HashMap<>();
        for (SpirvParser.TargetPairLiteralIntegerIdRefContext tCtx : ctx.targetPairLiteralIntegerIdRef()) {
            SpirvParser.PairLiteralIntegerIdRefContext pCtx = tCtx.pairLiteralIntegerIdRef();
            Expression value =  expressions.makeValue(Integer.parseInt(pCtx.literalInteger().getText()), integerType);
            String labelId = pCtx.idRef().getText();
            Label label = cfBuilder.getOrCreateLabel(labelId);
            cases.put(value, label);
        }
        List<CondJump> jumps = EventFactory.newSwitch(selector, defaultLabel, cases);
        for (CondJump jump : jumps) {
            builder.addEvent(jump);
        }
        return cfBuilder.endBlock(jumps.get(jumps.size() - 1));
    }

    @Override
    public Event visitOpLoopMerge(SpirvParser.OpLoopMergeContext ctx) {
        if (continueLabelId == null && mergeLabelId == null) {
            continueLabelId = ctx.continueTarget().getText();
            mergeLabelId = ctx.mergeBlock().getText();
            builder.setNextOps(Set.of("OpBranchConditional", "OpBranch"));
            return null;
        }
        throw new ParsingException("End and continue labels must be null");
    }

    @Override
    public Event visitOpBranch(SpirvParser.OpBranchContext ctx) {
        String labelId = ctx.targetLabel().getText();
        if (continueLabelId == null && mergeLabelId == null) {
            return visitGoto(labelId);
        }
        if (continueLabelId != null && mergeLabelId != null) {
            return visitLoopBranch(labelId);
        }
        throw new ParsingException("OpBranch '%s' must be either " +
                "a part of a loop definition or an arbitrary goto", labelId);
    }

    @Override
    public Event visitOpBranchConditional(SpirvParser.OpBranchConditionalContext ctx) {
        Expression guard = builder.getExpression(ctx.condition().getText());
        String trueLabelId = ctx.trueLabel().getText();
        String falseLabelId = ctx.falseLabel().getText();
        if (trueLabelId.equals(falseLabelId)) {
            throw new ParsingException("Labels of conditional branch cannot be the same");
        }
        if (mergeLabelId != null) {
            if (continueLabelId != null) {
                return visitLoopBranchConditional(guard, trueLabelId, falseLabelId);
            }
            return visitIfBranch(guard, trueLabelId, falseLabelId);
        }
        return visitConditionalJump(guard, trueLabelId, falseLabelId);
    }

    @Override
    public Event visitOpReturn(SpirvParser.OpReturnContext ctx) {
        Type returnType = builder.getCurrentFunctionType().getReturnType();
        if (types.getVoidType().equals(returnType)) {
            Return event = newFunctionReturn(null);
            builder.addEvent(event);
            return cfBuilder.endBlock(event);
        }
        throw new ParsingException("Illegal non-value return for a non-void function '%s'",
                builder.getCurrentFunctionName());
    }

    @Override
    public Event visitOpReturnValue(SpirvParser.OpReturnValueContext ctx) {
        Type returnType = builder.getCurrentFunctionType().getReturnType();
        if (!types.getVoidType().equals(returnType)) {
            String valueId = ctx.valueIdRef().getText();
            Expression expression = builder.getExpression(valueId);
            if (expression instanceof ScopedPointerVariable) {
                Register register = builder.addRegister(valueId + "_res", returnType);
                builder.addEvent(EventFactory.newLocal(register, expression));
                expression = register;
            }
            Event event = newFunctionReturn(expression);
            builder.addEvent(event);
            return cfBuilder.endBlock(event);
        }
        throw new ParsingException("Illegal value return for a void function '%s'",
                builder.getCurrentFunctionName());
    }

    @Override
    public Event visitOpLifetimeStart(SpirvParser.OpLifetimeStartContext ctx) {
        // Declare that an object was not defined before this instruction.
        String pointerId = ctx.pointer().getText();
        int size = Integer.parseInt(ctx.sizeLiteralInteger().getText());
        Expression pointerExp = builder.getExpression(pointerId);
        if (!(pointerExp instanceof ScopedPointerVariable pointerVariable)
                || !pointerVariable.getScopeId().equals(Tag.Spirv.SC_FUNCTION) ) {
            throw new ParsingException("Lifetime start can only be applied to a pointer with Function storage class: '%s'", pointerId);
        }
        Register register = builder.addRegister(pointerId + "_alloc", pointerVariable.getType());
        IntegerType pointerIntegerType = TypeFactory.getInstance().getArchType();
        Expression sizeExpression = new IntLiteral(pointerIntegerType, new BigInteger(Long.toString(size)));
        Alloc alloc = EventFactory.newAlloc(register, pointerExp.getType(), sizeExpression, false, false);
        builder.addEvent(alloc);
        return null;
    }

    @Override
    public Event visitOpLifetimeStop(SpirvParser.OpLifetimeStopContext ctx) {
        // Declare that an object is not used after this instruction.
        String pointerId = ctx.pointer().getText();
        Integer size = Integer.parseInt(ctx.sizeLiteralInteger().getText());
        Expression pointerExp = builder.getExpression(pointerId);
        if (!(pointerExp instanceof ScopedPointerVariable pointerVariable)
                || !pointerVariable.getScopeId().equals(Tag.Spirv.SC_FUNCTION)) {
            throw new ParsingException("Lifetime stop can only be applied to a pointer with Function storage class: '%s'", pointerId);
        }
        // TODO: Remove the variable from the program?
        return null;
    }

    private Event visitGoto(String labelId) {
        Label label = cfBuilder.getOrCreateLabel(labelId);
        Event event = EventFactory.newGoto(label);
        builder.addEvent(event);
        return cfBuilder.endBlock(event);
    }

    private Event visitLoopBranch(String labelId) {
        continueLabelId = null;
        mergeLabelId = null;
        return visitGoto(labelId);
    }

    private Event visitConditionalJump(Expression guard, String trueLabelId, String falseLabelId) {
        if (cfBuilder.isBlockStarted(trueLabelId)) {
            String labelId = trueLabelId;
            trueLabelId = falseLabelId;
            falseLabelId = labelId;
            guard = ExpressionFactory.getInstance().makeNot(guard);
        }
        Label trueLabel = cfBuilder.getOrCreateLabel(trueLabelId);
        Label falseLabel = cfBuilder.getOrCreateLabel(falseLabelId);
        Event trueJump = builder.addEvent(EventFactory.newJump(guard, trueLabel));
        builder.addEvent(EventFactory.newGoto(falseLabel));
        return cfBuilder.endBlock(trueJump);
    }

    private Event visitIfBranch(Expression guard, String trueLabelId, String falseLabelId) {
        for (String labelId : List.of(trueLabelId, falseLabelId)) {
            if (cfBuilder.isBlockStarted(labelId)) {
                throw new ParsingException("Illegal backward jump to '%s' from a structured branch", labelId);
            }
        }
        mergeLabelId = null;
        builder.setNextOps(Set.of("OpLabel"));

        // TODO: Currently, we treat structured branches as unstructured control flow.
        //  We need a new event type to represent SPIR-V structured control flow.
        //  For now, we can treat a structured branch as unstructured jumps,
        //  because Vulkan memory model has no control dependency.

        return visitConditionalJump(guard, trueLabelId, falseLabelId);
    }

    private Event visitLoopBranchConditional(Expression guard, String trueLabelId, String falseLabelId) {
        mergeLabelId = null;
        continueLabelId = null;
        nextLabelId = trueLabelId;
        builder.setNextOps(Set.of("OpLabel"));

        // TODO: Currently, we treat structured loops as unstructured control flow.
        //  We need to add a new event type for this.
        //  For now, we can treat a structured branch as unstructured jumps,
        //  because Vulkan memory model has no control dependency.

        return visitConditionalJump(guard, trueLabelId, falseLabelId);
    }

    public Set<String> getSupportedOps() {
        return Set.of(
                "OpPhi",
                "OpBranch",
                "OpBranchConditional",
                "OpLabel",
                "OpLoopMerge",
                "OpSelectionMerge",
                "OpSwitch",
                "OpReturn",
                "OpReturnValue",
                "OpLifetimeStart",
                "OpLifetimeStop"
        );
    }
}
