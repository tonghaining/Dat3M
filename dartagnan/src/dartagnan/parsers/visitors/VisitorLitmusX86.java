package dartagnan.parsers.visitors;

import dartagnan.LitmusX86BaseVisitor;
import dartagnan.LitmusX86Parser;
import dartagnan.LitmusX86Visitor;
import dartagnan.asserts.*;
import dartagnan.expression.AConst;
import dartagnan.parsers.utils.ParsingException;
import dartagnan.parsers.utils.ProgramBuilder;
import dartagnan.program.Location;
import dartagnan.program.Register;
import dartagnan.program.event.Fence;
import dartagnan.program.event.Load;
import dartagnan.program.event.Local;
import dartagnan.program.event.Store;
import dartagnan.program.event.tso.Xchg;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class VisitorLitmusX86
        extends LitmusX86BaseVisitor<Object>
        implements LitmusX86Visitor<Object> {

    private final static Set<String> fences = new HashSet<>(Arrays.asList("Mfence"));

    private ProgramBuilder programBuilder = new ProgramBuilder();
    private String mainThread;
    private Integer threadCount = 0;

    // ----------------------------------------------------------------------------------------------------------------
    // Entry point

    @Override
    public Object visitMain(LitmusX86Parser.MainContext ctx) {
        visitThreadDeclaratorList(ctx.program().threadDeclaratorList());
        visitVariableDeclaratorList(ctx.variableDeclaratorList());
        visitInstructionList(ctx.program().instructionList());
        visitAssertionList(ctx.assertionList());
        return programBuilder.build();
    }


    // ----------------------------------------------------------------------------------------------------------------
    // Variable declarator list, e.g., { 0:EAX=0; 1:EAX=1; x=2; }

    @Override
    public Object visitVariableDeclaratorLocation(LitmusX86Parser.VariableDeclaratorLocationContext ctx) {
        programBuilder.addDeclarationLocImm(ctx.location().getText(), Integer.parseInt(ctx.value().getText()));
        return null;
    }

    @Override
    public Object visitVariableDeclaratorRegister(LitmusX86Parser.VariableDeclaratorRegisterContext ctx) {
        programBuilder.addDeclarationRegImm(threadId(ctx.threadId().getText()), ctx.register().getText(), Integer.parseInt(ctx.value().getText()));
        return null;
    }

    @Override
    public Object visitVariableDeclaratorRegisterLocation(LitmusX86Parser.VariableDeclaratorRegisterLocationContext ctx) {
        programBuilder.addDeclarationRegLoc(threadId(ctx.threadId().getText()), ctx.register().getText(), ctx.location().getText());
        return null;
    }

    @Override
    public Object visitVariableDeclaratorLocationLocation(LitmusX86Parser.VariableDeclaratorLocationLocationContext ctx) {
        programBuilder.addDeclarationLocLoc(ctx.location(0).getText(), ctx.location(1).getText());
        return null;
    }


    // ----------------------------------------------------------------------------------------------------------------
    // Thread declarator list (on top of instructions), e.g. " P0  |   P1  |   P2  ;"

    @Override
    public Object visitThreadDeclaratorList(LitmusX86Parser.ThreadDeclaratorListContext ctx) {
        for(LitmusX86Parser.ThreadIdContext threadCtx : ctx.threadId()){
            String thread = threadId(threadCtx.ThreadIdentifier().getText());
            programBuilder.initThread(thread);
            threadCount++;
        }
        return null;
    }


    // ----------------------------------------------------------------------------------------------------------------
    // Instruction list (the program itself)

    @Override
    public Object visitInstructionRow(LitmusX86Parser.InstructionRowContext ctx) {
        for(Integer i = 0; i < threadCount; i++){
            mainThread = i.toString();
            visitInstruction(ctx.instruction(i));
        }
        return null;
    }

    @Override
    public Object visitNone(LitmusX86Parser.NoneContext ctx) {
        return null;
    }

    @Override
    public Object visitLoadValueToRegister(LitmusX86Parser.LoadValueToRegisterContext ctx) {
        Register register = programBuilder.getOrCreateRegister(mainThread, ctx.register().getText());
        AConst constant = new AConst(Integer.parseInt(ctx.value().getText()));
        return programBuilder.addChild(mainThread, new Local(register, constant));
    }

    @Override
    public Object visitLoadLocationToRegister(LitmusX86Parser.LoadLocationToRegisterContext ctx) {
        Register register = programBuilder.getOrCreateRegister(mainThread, ctx.register().getText());
        Location location = programBuilder.getOrCreateLocation(ctx.location().getText());
        return programBuilder.addChild(mainThread, new Load(register, location, "_rx"));
    }

    @Override
    public Object visitStoreValueToLocation(LitmusX86Parser.StoreValueToLocationContext ctx) {
        Location location = programBuilder.getOrCreateLocation(ctx.location().getText());
        AConst constant = new AConst(Integer.parseInt(ctx.value().getText()));
        return programBuilder.addChild(mainThread, new Store(location, constant, "_rx"));
    }

    @Override
    public Object visitStoreRegisterToLocation(LitmusX86Parser.StoreRegisterToLocationContext ctx) {
        Register register = programBuilder.getOrErrorRegister(mainThread, ctx.register().getText());
        Location location = programBuilder.getOrCreateLocation(ctx.location().getText());
        return programBuilder.addChild(mainThread, new Store(location, register, "_rx"));
    }

    @Override
    public Object visitExchangeRegisterLocation(LitmusX86Parser.ExchangeRegisterLocationContext ctx) {
        Register register = programBuilder.getOrErrorRegister(mainThread, ctx.register().getText());
        Location location = programBuilder.getOrCreateLocation(ctx.location().getText());
        return programBuilder.addChild(mainThread, new Xchg(location, register,"_rx"));
    }

    @Override
    public Object visitIncrementLocation(LitmusX86Parser.IncrementLocationContext ctx) {
        // TODO: Implementation
        throw new ParsingException("INC is not implemented");
    }

    @Override
    public Object visitCompareRegisterValue(LitmusX86Parser.CompareRegisterValueContext ctx) {
        // TODO: Implementation
        throw new ParsingException("CMP is not implemented");
    }

    @Override
    public Object visitCompareLocationValue(LitmusX86Parser.CompareLocationValueContext ctx) {
        // TODO: Implementation
        throw new ParsingException("CMP is not implemented");
    }

    @Override
    public Object visitAddRegisterRegister(LitmusX86Parser.AddRegisterRegisterContext ctx) {
        // TODO: Implementation
        throw new ParsingException("ADD is not implemented");
    }

    @Override
    public Object visitAddRegisterValue(LitmusX86Parser.AddRegisterValueContext ctx) {
        // TODO: Implementation
        throw new ParsingException("ADD is not implemented");
    }

    @Override
    public Object visitFence(LitmusX86Parser.FenceContext ctx) {
        String name = ctx.getText().toLowerCase();
        name = name.substring(0, 1).toUpperCase() + name.substring(1);
        if(fences.contains(name)){
            return programBuilder.addChild(mainThread, new Fence(name));
        }
        throw new ParsingException("Unrecognised fence " + name);
    }


    // ----------------------------------------------------------------------------------------------------------------
    // Assertions

    @Override
    public Object visitAssertionList(LitmusX86Parser.AssertionListContext ctx) {
        if(ctx != null){
            AbstractAssert ass = (AbstractAssert) visit(ctx.assertion());
            if(ctx.AssertionForall() != null){
                ass = new AssertNot(ass);
            }

            ass.setType(getAssertionType(ctx));
            programBuilder.setAssert(ass);
        }
        return null;
    }

    @Override
    public Object visitAssertionBasic(LitmusX86Parser.AssertionBasicContext ctx){
        Object arg1 = ctx.assertionValue(0).accept(this);
        Object arg2 = ctx.assertionValue(1).accept(this);
        return new AssertBasic(
                arg1 instanceof Location ? (Location)arg1 : arg1 instanceof Register ? (Register)arg1 : (AConst)arg1,
                assOp(ctx.assertionCompare().getText()),
                arg2 instanceof Location ? (Location)arg2 : arg2 instanceof Register ? (Register)arg2 : (AConst)arg2);
    }

    @Override
    public Object visitAssertionValue(LitmusX86Parser.AssertionValueContext ctx){
        if(ctx.location() != null){
            return programBuilder.getOrErrorLocation(ctx.location().getText());
        }
        if(ctx.register() != null){
            return programBuilder.getOrErrorRegister(threadId(ctx.threadId().getText()), ctx.register().getText());
        }
        return new AConst(Integer.parseInt(ctx.value().getText()));
    }

    @Override
    public Object visitAssertionAnd(LitmusX86Parser.AssertionAndContext ctx) {
        return new AssertCompositeAnd(
                (AbstractAssert) visit(ctx.assertion(0)),
                (AbstractAssert) visit(ctx.assertion(1))
        );
    }

    @Override
    public Object visitAssertionOr(LitmusX86Parser.AssertionOrContext ctx) {
        return new AssertCompositeOr(
                (AbstractAssert) visit(ctx.assertion(0)),
                (AbstractAssert) visit(ctx.assertion(1))
        );
    }

    @Override
    public Object visitAssertionNot(LitmusX86Parser.AssertionNotContext ctx) {
        return new AssertNot((AbstractAssert) visit(ctx.assertion()));
    }

    @Override
    public Object visitAssertionParenthesis(LitmusX86Parser.AssertionParenthesisContext ctx) {
        return visit(ctx.assertion());
    }

    private String getAssertionType(LitmusX86Parser.AssertionListContext ctx){
        if(ctx.AssertionExists() != null){
            return AbstractAssert.ASSERT_TYPE_EXISTS;
        }

        if(ctx.AssertionExistsNot() != null){
            return AbstractAssert.ASSERT_TYPE_NOT_EXISTS;
        }

        if(ctx.AssertionFinal() != null){
            return AbstractAssert.ASSERT_TYPE_FINAL;
        }

        if(ctx.AssertionForall() != null){
            return AbstractAssert.ASSERT_TYPE_FORALL;
        }

        throw new ParsingException("Unknown type of assertion clause");
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Private

    private String threadId(String threadId){
        return threadId.replace("P", "");
    }

    private String assOp(String op){
        return op.equals("=") ? "==" : op;
    }
}