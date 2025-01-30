package com.dat3m.dartagnan.parsers.program.visitors.spirv.builders;

import com.dat3m.dartagnan.configuration.Arch;
import com.dat3m.dartagnan.exception.ParsingException;
import com.dat3m.dartagnan.expression.Expression;
import com.dat3m.dartagnan.expression.ExpressionFactory;
import com.dat3m.dartagnan.expression.Type;
import com.dat3m.dartagnan.expression.type.*;
import com.dat3m.dartagnan.parsers.program.visitors.spirv.decorations.BuiltIn;
import com.dat3m.dartagnan.parsers.program.visitors.spirv.helpers.HelperTags;
import com.dat3m.dartagnan.parsers.program.visitors.spirv.utils.ThreadCreator;
import com.dat3m.dartagnan.parsers.program.visitors.spirv.utils.ThreadGrid;
import com.dat3m.dartagnan.program.event.functions.FunctionCall;
import com.dat3m.dartagnan.program.memory.*;
import com.dat3m.dartagnan.program.*;
import com.dat3m.dartagnan.program.event.*;

import java.util.*;
import java.util.stream.Collectors;

import static com.dat3m.dartagnan.parsers.program.visitors.spirv.decorations.DecorationType.BUILT_IN;

public class ProgramBuilder {

    protected final Map<String, Type> types = new HashMap<>();
    protected final Map<String, Expression> expressions = new HashMap<>();
    protected final Map<String, Expression> expressionsDead = new HashMap<>();
    protected final Map<String, ScopedPointerVariable> registerPointers = new HashMap<>();
    protected final Map<String, Expression> inputs = new HashMap<>();
    protected final Map<String, String> debugInfos = new HashMap<>();
    protected final ThreadGrid grid;
    protected final Program program;
    protected ControlFlowBuilder controlFlowBuilder;
    protected DecorationsBuilder decorationsBuilder;
    protected Function currentFunction;
    protected String entryPointId;
    protected Arch arch;
    protected Set<String> nextOps;

    public ProgramBuilder(ThreadGrid grid) {
        this.grid = grid;
        this.program = new Program(new Memory(), Program.SourceLanguage.SPV);
        this.controlFlowBuilder = new ControlFlowBuilder(expressions);
        this.decorationsBuilder = new DecorationsBuilder(grid);
    }

    public Program build() {
        validateBeforeBuild();
        controlFlowBuilder.build();
        BuiltIn builtIn = (BuiltIn) decorationsBuilder.getDecoration(BUILT_IN);
        Function entryFunction = getEntryPointFunction();
        List<Function> subFunctions = program.getFunctions().stream()
                .filter(f -> !f.equals(entryFunction))
                .toList();
        new ThreadCreator(grid, entryFunction, subFunctions, getVariables(), builtIn).create();
        return program;
    }

    public ThreadGrid getThreadGrid() {
        return grid;
    }

    public ControlFlowBuilder getControlFlowBuilder() {
        return controlFlowBuilder;
    }

    public DecorationsBuilder getDecorationsBuilder() {
        return decorationsBuilder;
    }

    public Set<String> getNextOps() {
        return nextOps;
    }

    public void setNextOps(Set<String> nextOps) {
        if (this.nextOps != null) {
            throw new ParsingException("Illegal attempt to override next ops");
        }
        this.nextOps = nextOps;
    }

    public void clearNextOps() {
        this.nextOps = null;
    }

    public void setEntryPointId(String id) {
        if (entryPointId != null) {
            throw new ParsingException("Multiple entry points are not supported");
        }
        entryPointId = id;
    }

    public String getEntryPointId() {
        return entryPointId;
    }

    public void setArch(Arch arch) {
        if (this.arch != null) {
            throw new ParsingException("Illegal attempt to override memory model");
        }
        this.arch = arch;
    }

    public Arch getArch() {
        return arch;
    }

    public void setSpecification(Program.SpecificationType type, Expression condition) {
        if (program.getSpecification() != null) {
            throw new ParsingException("Attempt to override program specification");
        }
        program.setSpecification(type, condition);
    }

    public boolean hasInput(String id) {
        return inputs.containsKey(id);
    }

    public boolean hasDefinition(String id) {
        return types.containsKey(id) || expressions.containsKey(id);
    }

    public Expression getInput(String id) {
        if (inputs.containsKey(id)) {
            return inputs.get(id);
        }
        throw new ParsingException("Reference to undefined input variable '%s'", id);
    }

    public void addInput(String id, Expression value) {
        if (inputs.containsKey(id)) {
            throw new ParsingException("Duplicated input definition '%s'", id);
        }
        inputs.put(id, value);
    }

    public Type getType(String id) {
        Type type = types.get(id);
        if (type == null) {
            throw new ParsingException("Reference to undefined type '%s'", id);
        }
        return type;
    }

    public Type addType(String id, Type type) {
        if (types.containsKey(id) || expressions.containsKey(id)) {
            throw new ParsingException("Duplicated definition '%s'", id);
        }
        types.put(id, type);
        return type;
    }

    public Expression getExpression(String id) {
        Expression expression = expressions.get(id);
        Expression expressionDead = expressionsDead.get(id);
        if (expression == null || expressionDead != null) {
            throw new ParsingException("Reference to undefined expression '%s'", id);
        }
        return expression;
    }

    public void removeExpression(String id) {
        expressionsDead.put(id, expressions.get(id));
    }

    public Expression addExpression(String id, Expression value) {
        if (types.containsKey(id) || expressions.containsKey(id)) {
            throw new ParsingException("Duplicated definition '%s'", id);
        }
        expressions.put(id, value);
        return value;
    }

    public Set<ScopedPointerVariable> getVariables() {
        Set<ScopedPointerVariable> res = expressions.values().stream()
                .filter(ScopedPointerVariable.class::isInstance)
                .map(v -> (ScopedPointerVariable) v)
                .collect(Collectors.toSet());
        for (ScopedPointer pointer : registerPointers.values()) {
            if (pointer instanceof ScopedPointerVariable variable) {
                res.add(variable);
            }
        }
        return res;
    }

    public MemoryObject allocateVariable(String id, int bytes) {
        MemoryObject memObj = program.getMemory().allocateVirtual(bytes, true, null);
        memObj.setName(id);
        return memObj;
    }

    public ScopedPointerVariable allocateScopedPointerVariable(String id, Expression initValue, String storageClass, Type type) {
        MemoryObject memObj = allocateVariable(id, TypeFactory.getInstance().getMemorySizeInBytes(type));
        memObj.setIsThreadLocal(false);
        memObj.setInitialValue(0, initValue);
        HelperTags.addFeatureTags(memObj, storageClass, arch);
        return ExpressionFactory.getInstance().makeScopedPointerVariable(
                id, storageClass, type, memObj);
    }

    public ElementPointerVariable allocateElementPointerVariable(String id, ScopedPointerVariable pointer, int offset) {
        Type type;
        if (pointer.getInnerType() instanceof AggregateType aggregateType) {
            type = aggregateType.getTypeOffsets().get(0).type();
        } else if (pointer.getInnerType() instanceof ArrayType arrayType) {
            type = arrayType.getElementType();
        } else {
            throw new ParsingException("Unsupported pointer type '%s'", pointer.getInnerType());
        }
        MemoryObject memObj = allocateVariable(id, TypeFactory.getInstance().getMemorySizeInBytes(type));
        memObj.setIsThreadLocal(false);
        Expression value = ExpressionFactory.getInstance().makeGetElementPointer(type, pointer,
                List.of(ExpressionFactory.getInstance().makeValue(offset, TypeFactory.getInstance().getArchType())));
        memObj.setInitialValue(0, value);
        HelperTags.addFeatureTags(memObj, pointer.getScopeId(), arch);
        return ExpressionFactory.getInstance().makeElementPointerVariable(
                id, memObj, pointer);
    }

    public String getPointerStorageClass(String id) {
        Expression expression = getExpression(id);
        // Pointers to variables and references from OpAccessChain
        if (expression instanceof ScopedPointer pointer) {
            return pointer.getScopeId();
        }
        // Pointers passed via function argument registers
        if (expression.getType() instanceof ScopedPointerType pointerType) {
            return pointerType.getScopeId();
        }
        throw new ParsingException("Reference to undefined pointer '%s'", id);
    }

    public void addRegisterPointer(String id, ScopedPointerVariable pointer) {
        if (registerPointers.containsKey(id)) {
            throw new ParsingException("Duplicated register pointer definition '%s'", id);
        }
        registerPointers.put(id, pointer);
    }

    public Register addRegister(String id, String typeId) {
        Type type = getType(typeId);
        return addRegister(id, type);
    }

    public Register addRegister(String id, Type type) {
        return getCurrentFunctionOrThrowError().newRegister(id, type);
    }

    public Expression makeUndefinedValue(Type type) {
        return program.newConstant(type);
    }

    public Event addEvent(Event event) {
        if (currentFunction == null) {
            throw new ParsingException("Attempt to add an event outside a function definition");
        }
        if (!controlFlowBuilder.isInsideBlock()) {
            throw new ParsingException("Attempt to add an event outside a control flow block");
        }
        if (controlFlowBuilder.hasCurrentLocation()) {
            event.setMetadata(controlFlowBuilder.getCurrentLocation());
        }
        if (event instanceof RegWriter regWriter) {
            Register register = regWriter.getResultRegister();
            addExpression(register.getName(), register);
        }
        currentFunction.append(event);
        return event;
    }

    public FunctionType getCurrentFunctionType() {
        return getCurrentFunctionOrThrowError().getFunctionType();
    }

    public String getCurrentFunctionName() {
        return getCurrentFunctionOrThrowError().getName();
    }

    public void startCurrentFunction(Function function) {
        if (currentFunction != null) {
            throw new ParsingException("Attempt to define function '%s' " +
                    "inside a definition of another function '%s'",
                    function.getName(), currentFunction.getName());
        }
        addExpression(function.getName(), function);
        for (Register register : function.getParameterRegisters()) {
            if (register instanceof PointerRegister pointerRegister) {
                ScopedPointerVariable pointer = registerPointers.get(register.getName());
                pointerRegister.setPointer(pointer);
            }
            addExpression(register.getName(), register);
        }
        program.addFunction(function);
        currentFunction = function;
    }

    public void endCurrentFunction() {
        if (currentFunction == null) {
            throw new ParsingException("Illegal attempt to exit a function definition");
        }
        currentFunction = null;
    }

    public void addDebugInfo(String id, String info) {
        if (debugInfos.containsKey(id)) {
            throw new ParsingException("Attempt to add debug information with duplicate id");
        }
        debugInfos.put(id, info);
    }

    public String getDebugInfo(String id) {
        if (!debugInfos.containsKey(id)) {
            throw new ParsingException("No debug information with id '%s'", id);
        }
        return debugInfos.get(id);
    }

    private void validateBeforeBuild() {
        if (nextOps != null) {
            throw new ParsingException("Missing expected op: %s",
                    String.join(",", nextOps));

        }
        if (currentFunction != null) {
            throw new ParsingException("Unclosed definition for function '%s'",
                    currentFunction.getName());
        }
        expressions.values().forEach(expression -> {
            if (expression instanceof Function function) {
                function.getEvents().forEach(event -> {
                    if (event instanceof FunctionCall call) {
                        String calledId = call.getCalledFunction().getName();
                        if (!expressions.containsKey(calledId)
                                || !(expressions.get(calledId) instanceof Function)) {
                            throw new ParsingException("Call to undefined function '%s'", calledId);
                        }
                    }
                });
            }
        });
    }

    private Function getEntryPointFunction() {
        if (entryPointId == null) {
            throw new ParsingException("Cannot build the program, entryPointId is missing");
        }
        Expression expression = expressions.get(entryPointId);
        if (expression == null) {
            throw new ParsingException("Cannot build the program, missing function definition '%s'", entryPointId);
        }
        if (expression instanceof Function function) {
            if (function.hasReturnValue()) {
                throw new ParsingException("Entry point function %s is not a void function", entryPointId);
            }
            return function;
        }
        throw new ParsingException("Entry point expression '%s' must be a function", entryPointId);
    }

    private Function getCurrentFunctionOrThrowError() {
        if (currentFunction != null) {
            return currentFunction;
        }
        throw new ParsingException("Attempt to reference current function " +
                "outside of a function definition");
    }
}
