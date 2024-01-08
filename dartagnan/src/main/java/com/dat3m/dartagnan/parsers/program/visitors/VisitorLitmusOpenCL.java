package com.dat3m.dartagnan.parsers.program.visitors;

import com.dat3m.dartagnan.configuration.Arch;
import com.dat3m.dartagnan.exception.ParsingException;
import com.dat3m.dartagnan.expression.Expression;
import com.dat3m.dartagnan.expression.ExpressionFactory;
import com.dat3m.dartagnan.expression.IConst;
import com.dat3m.dartagnan.expression.op.IOpBin;
import com.dat3m.dartagnan.expression.type.IntegerType;
import com.dat3m.dartagnan.expression.type.TypeFactory;
import com.dat3m.dartagnan.parsers.LitmusOpenCLBaseVisitor;
import com.dat3m.dartagnan.parsers.LitmusOpenCLParser;
import com.dat3m.dartagnan.parsers.program.utils.AssertionHelper;
import com.dat3m.dartagnan.parsers.program.utils.ProgramBuilder;
import com.dat3m.dartagnan.program.Program;
import com.dat3m.dartagnan.program.Register;
import com.dat3m.dartagnan.program.event.EventFactory;
import com.dat3m.dartagnan.program.event.Tag;
import com.dat3m.dartagnan.program.event.arch.opencl.OpenCLAtomCAX;
import com.dat3m.dartagnan.program.event.arch.opencl.OpenCLAtomOp;
import com.dat3m.dartagnan.program.event.core.Event;
import com.dat3m.dartagnan.program.event.core.Label;
import com.dat3m.dartagnan.program.event.core.Load;
import com.dat3m.dartagnan.program.event.core.Store;
import com.dat3m.dartagnan.program.memory.MemoryObject;
import org.antlr.v4.runtime.misc.Interval;

public class VisitorLitmusOpenCL extends LitmusOpenCLBaseVisitor<Object> {
    private final ProgramBuilder programBuilder = ProgramBuilder.forArch(Program.SourceLanguage.LITMUS, Arch.OPENCL);
    private final ExpressionFactory expressions = programBuilder.getExpressionFactory();
    private final TypeFactory types = programBuilder.getTypeFactory();
    private final IntegerType archType = types.getArchType();
    private int mainThread;
    private int threadCount = 0;

    public VisitorLitmusOpenCL() {
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Entry point

    @Override
    public Object visitMain(LitmusOpenCLParser.MainContext ctx) {
        visitThreadDeclaratorList(ctx.program().threadDeclaratorList());
        visitVariableDeclaratorList(ctx.variableDeclaratorList());
        visitInstructionList(ctx.program().instructionList());
        if (ctx.assertionList() != null) {
            int a = ctx.assertionList().getStart().getStartIndex();
            int b = ctx.assertionList().getStop().getStopIndex();
            String raw = ctx.assertionList().getStart().getInputStream().getText(new Interval(a, b));
            programBuilder.setAssert(AssertionHelper.parseAssertionList(programBuilder, raw));
        }
        if (ctx.assertionFilter() != null) {
            int a = ctx.assertionFilter().getStart().getStartIndex();
            int b = ctx.assertionFilter().getStop().getStopIndex();
            String raw = ctx.assertionFilter().getStart().getInputStream().getText(new Interval(a, b));
            programBuilder.setAssertFilter(AssertionHelper.parseAssertionFilter(programBuilder, raw));
        }
        return programBuilder.build();
    }


    // ----------------------------------------------------------------------------------------------------------------
    // Variable declarator list
    @Override
    public Object visitVariableDeclaratorLocation(LitmusOpenCLParser.VariableDeclaratorLocationContext ctx) {
        programBuilder.initVirLocEqCon(ctx.location().getText(),
                (IConst) ctx.constant().accept(this));
        return null;
    }

    @Override
    public Object visitVariableDeclaratorRegister(LitmusOpenCLParser.VariableDeclaratorRegisterContext ctx) {
        programBuilder.initRegEqConst(ctx.threadId().id, ctx.register().getText(),
                (IConst) ctx.constant().accept(this));
        return null;
    }

    @Override
    public Object visitVariableDeclaratorRegisterLocation(LitmusOpenCLParser.VariableDeclaratorRegisterLocationContext ctx) {
        programBuilder.initRegEqLocPtr(ctx.threadId().id, ctx.register().getText(), ctx.location().getText(),
                archType);
        return null;
    }

    @Override
    public Object visitVariableDeclaratorLocationLocation(LitmusOpenCLParser.VariableDeclaratorLocationLocationContext ctx) {
        programBuilder.initVirLocEqLoc(ctx.location(0).getText(), ctx.location(1).getText());
        return null;
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Thread declarator list (on top of instructions)
    @Override
    public Object visitThreadDeclaratorList(LitmusOpenCLParser.ThreadDeclaratorListContext ctx) {
        for (LitmusOpenCLParser.ThreadScopeContext threadScopeContext : ctx.threadScope()) {
            int wgID = threadScopeContext.scopeID().wgID().id;
            int dvID = threadScopeContext.scopeID().dvID().id;
            // NB: the order of scopeIDs is important
            programBuilder.newScopedThread(Arch.OPENCL, threadScopeContext.threadId().id, dvID, wgID);
            threadCount++;
        }
        return null;
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Instruction list (the program itself)
    @Override
    public Object visitConstant(LitmusOpenCLParser.ConstantContext ctx) {
        return ExpressionFactory.getInstance().parseValue(ctx.getText(), archType);
    }

    @Override
    public Object visitRegister(LitmusOpenCLParser.RegisterContext ctx) {
        return programBuilder.getOrNewRegister(mainThread, ctx.getText(), archType);
    }

    @Override
    public Object visitInstructionRow(LitmusOpenCLParser.InstructionRowContext ctx) {
        for (int i = 0; i < threadCount; i++) {
            mainThread = i;
            visitInstruction(ctx.instruction(i));
        }
        return null;
    }

    @Override
    public Object visitStoreInstruction(LitmusOpenCLParser.StoreInstructionContext ctx) {
        MemoryObject object = programBuilder.getOrNewMemoryObject(ctx.location().getText());
        Expression constant = (Expression) ctx.value().accept(this);
        String mo = ctx.mo().content;
        Store store = EventFactory.newStoreWithMo(object, constant, mo);
        switch (mo) {
            case Tag.OpenCL.WEAK -> {
                if (ctx.scope() != null) {
                    throw new ParsingException("Weak store instruction doesn't need scope: " + ctx.scope().content);
                }
            }
            case Tag.OpenCL.REL, Tag.OpenCL.RLX -> store.addTags(ctx.scope().content);
            default -> throw new ParsingException("Store instruction doesn't support mo: " + mo);
        }
        store.addTags(ctx.context().content);
        return programBuilder.addChild(mainThread, store);
    }

    @Override
    public Object visitLocalValue(LitmusOpenCLParser.LocalValueContext ctx) {
        Register register = (Register) ctx.register().accept(this);
        Expression value = (Expression) ctx.value().accept(this);
        return programBuilder.addChild(mainThread, EventFactory.newLocal(register, value));
    }

    @Override
    public Object visitLocalAdd(LitmusOpenCLParser.LocalAddContext ctx) {
        Register rd = (Register) ctx.register().accept(this);
        Expression lhs = (Expression) ctx.value(0).accept(this);
        Expression rhs = (Expression) ctx.value(1).accept(this);
        Expression exp = expressions.makeADD(lhs, rhs);
        return programBuilder.addChild(mainThread, EventFactory.newLocal(rd, exp));
    }

    @Override
    public Object visitLocalSub(LitmusOpenCLParser.LocalSubContext ctx) {
        Register rd = (Register) ctx.register().accept(this);
        Expression lhs = (Expression) ctx.value(0).accept(this);
        Expression rhs = (Expression) ctx.value(1).accept(this);
        Expression exp = expressions.makeSUB(lhs, rhs);
        return programBuilder.addChild(mainThread, EventFactory.newLocal(rd, exp));
    }

    @Override
    public Object visitLocalMul(LitmusOpenCLParser.LocalMulContext ctx) {
        Register rd = (Register) ctx.register().accept(this);
        Expression lhs = (Expression) ctx.value(0).accept(this);
        Expression rhs = (Expression) ctx.value(1).accept(this);
        Expression exp = expressions.makeMUL(lhs, rhs);
        return programBuilder.addChild(mainThread, EventFactory.newLocal(rd, exp));
    }

    @Override
    public Object visitLocalDiv(LitmusOpenCLParser.LocalDivContext ctx) {
        Register rd = (Register) ctx.register().accept(this);
        Expression lhs = (Expression) ctx.value(0).accept(this);
        Expression rhs = (Expression) ctx.value(1).accept(this);
        Expression exp = expressions.makeDIV(lhs, rhs, true);
        return programBuilder.addChild(mainThread, EventFactory.newLocal(rd, exp));
    }

    @Override
    public Object visitLoadLocation(LitmusOpenCLParser.LoadLocationContext ctx) {
        Register register = (Register) ctx.register().accept(this);
        MemoryObject location = programBuilder.getOrNewMemoryObject(ctx.location().getText());
        String mo = ctx.mo().content;
        Load load = EventFactory.newLoadWithMo(register, location, mo);
        switch (mo) {
            case Tag.OpenCL.WEAK -> {
                if (ctx.scope() != null) {
                    throw new ParsingException("Weak load instruction doesn't need scope: " + ctx.scope().content);
                }
            }
            case Tag.OpenCL.ACQ, Tag.OpenCL.RLX -> load.addTags(ctx.scope().content);
            default -> throw new ParsingException("Load instruction doesn't support mo: " + mo);
        }
        load.addTags(ctx.context().content);
        return programBuilder.addChild(mainThread, load);
    }

    @Override
    public Object visitAtomCompareExchange(LitmusOpenCLParser.AtomCompareExchangeContext ctx) {
        Register register = (Register) ctx.register().accept(this);
        MemoryObject object = programBuilder.getOrNewMemoryObject(ctx.location().get(0).getText());
        MemoryObject expected = programBuilder.getOrNewMemoryObject(ctx.location().get(1).getText());
        Expression desired = (Expression) ctx.value().accept(this);
        String successMo = ctx.mo().get(0).content;
        String failureMo = ctx.mo().get(1).content;
        String scope = ctx.scope().content;
        OpenCLAtomCAX atom = EventFactory.OpenCL.newAtom(register, object, expected, desired, successMo, failureMo, scope);
        return programBuilder.addChild(mainThread, atom);
    }

    @Override
    public Object visitAtomOp(LitmusOpenCLParser.AtomOpContext ctx) {
        Register register = (Register) ctx.register().accept(this);
        MemoryObject location = programBuilder.getOrNewMemoryObject(ctx.location().getText());
        Expression value = (Expression) ctx.value().accept(this);
        String mo = ctx.mo().content;
        String scope = ctx.scope().content;
        IOpBin op = ctx.operation().op;
        String context = ctx.context().content;
        OpenCLAtomOp rmw = EventFactory.OpenCL.newAtomOp(location, register, value, op, mo, scope, context);
        return programBuilder.addChild(mainThread, rmw);
    }

    @Override
    public Object visitBarrier(LitmusOpenCLParser.BarrierContext ctx) {
        String mo = ctx.mo().content;
        String scope = ctx.scope().content;
        String context = ctx.context().content;
        if (!mo.equals(Tag.OpenCL.ACQ) && !mo.equals(Tag.OpenCL.REL) && !mo.equals(Tag.OpenCL.ACQ_REL)) {
            throw new ParsingException("Fences must be acquire, release or acq_rel");
        }
        Event fence = EventFactory.newFence(ctx.getText().toLowerCase());
        fence.addTags(mo, scope, context);
        return programBuilder.addChild(mainThread, fence);
    }

    @Override
    public Object visitFence(LitmusOpenCLParser.FenceContext ctx) {
        String mo = ctx.mo().content;
        String scope = ctx.scope().content;
        String context = ctx.context().content;
        if (!(mo.equals(Tag.OpenCL.ACQ_REL) || mo.equals(Tag.OpenCL.SC))) {
            throw new ParsingException("Fence instruction doesn't support mo: " + mo);
        }
        Event fence = EventFactory.newFence(ctx.getText().toLowerCase());
        fence.addTags(mo, scope, context);
        return programBuilder.addChild(mainThread, fence);
    }

    @Override
    public Object visitLabel(LitmusOpenCLParser.LabelContext ctx) {
        return programBuilder.addChild(mainThread, programBuilder.getOrCreateLabel(mainThread, ctx.Label().getText()));
    }

    @Override
    public Object visitBranchCond(LitmusOpenCLParser.BranchCondContext ctx) {
        Label label = programBuilder.getOrCreateLabel(mainThread, ctx.Label().getText());
        Expression lhs = (Expression) ctx.value(0).accept(this);
        Expression rhs = (Expression) ctx.value(1).accept(this);
        Expression expr = expressions.makeBinary(lhs, ctx.cond().op, rhs);
        return programBuilder.addChild(mainThread, EventFactory.newJump(expr, label));
    }

    @Override
    public Object visitJump(LitmusOpenCLParser.JumpContext ctx) {
        Label label = programBuilder.getOrCreateLabel(mainThread, ctx.Label().getText());
        return programBuilder.addChild(mainThread, EventFactory.newGoto(label));
    }

}