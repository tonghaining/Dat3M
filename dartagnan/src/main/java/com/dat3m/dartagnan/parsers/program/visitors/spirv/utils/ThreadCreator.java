package com.dat3m.dartagnan.parsers.program.visitors.spirv.utils;

import com.dat3m.dartagnan.expression.type.FunctionType;
import com.dat3m.dartagnan.parsers.program.visitors.spirv.decorations.BuiltIn;
import com.dat3m.dartagnan.program.*;
import com.dat3m.dartagnan.program.Thread;
import com.dat3m.dartagnan.program.event.*;
import com.dat3m.dartagnan.program.event.core.Label;
import com.dat3m.dartagnan.program.event.core.threading.ThreadStart;
import com.dat3m.dartagnan.program.event.functions.FunctionCall;
import com.dat3m.dartagnan.program.event.functions.Return;
import com.dat3m.dartagnan.program.memory.Memory;
import com.dat3m.dartagnan.program.memory.ScopedPointerVariable;
import com.google.common.collect.Lists;

import java.util.*;

public class ThreadCreator {

    private final ThreadGrid grid;
    private final Function entryFunction;
    private final Program program;
    private final Set<Function> subFunctions;
    private final Set<ScopedPointerVariable> variables;
    private final Map<Function, Map<Thread, Function>> localSubFunctions = new HashMap<>();
    private final MemoryTransformer transformer;
    private int nextFunctionId;

    public ThreadCreator(ThreadGrid grid, Function function, Set<Function> subFunctions,
                         Set<ScopedPointerVariable> variables, BuiltIn builtIn, int nextFunctionId) {
        this.grid = grid;
        this.entryFunction = function;
        this.program = function.getProgram();
        this.subFunctions = subFunctions;
        this.variables = variables;
        this.transformer = new MemoryTransformer(grid, function, subFunctions, builtIn);
        for (Function subFunction : subFunctions) {
            Map<Thread, Function> threadLocalSubFunctions = new HashMap<>();
            localSubFunctions.put(subFunction, threadLocalSubFunctions);
        }
        this.nextFunctionId = nextFunctionId;
    }

    public void create() {
        for (int i = 0; i < grid.dvSize(); i++) {
            program.addThread(createThreadFromFunction(i));
        }
        deleteLocalFunctionVariables();
    }

    private Thread createThreadFromFunction(int tid) {
        String name = entryFunction.getName();
        FunctionType type = entryFunction.getFunctionType();
        List<String> args = Lists.transform(entryFunction.getParameterRegisters(), Register::getName);
        ThreadStart start = EventFactory.newThreadStart(null);
        ScopeHierarchy scope = grid.getScoreHierarchy(tid);
        Thread thread = new Thread(name, type, args, tid, start, scope, Set.of());
        thread.copyDummyCountFrom(entryFunction);
        transformer.setTransferFunction(thread);
        copyFunctionEvents(entryFunction, thread);
        transformReturnEvents(thread);
        // Create thread-local sub-functions
        for (Function subFunction : subFunctions) {
            String localSubFunctionName = subFunction.getName() + "@T" + tid;
            List<String> localSubFunctionArgs = Lists.transform(subFunction.getParameterRegisters(), Register::getName);
            int localFunctionId = nextFunctionId++;
            Function localSubFunction = new Function(localSubFunctionName, subFunction.getFunctionType(),
                    localSubFunctionArgs, localFunctionId, null);
            localSubFunction.copyDummyCountFrom(subFunction);
            transformer.setTransferFunction(localSubFunction);
            copyFunctionEvents(subFunction, localSubFunction);
            program.addFunction(localSubFunction);
            localSubFunctions.get(subFunction).put(thread, localSubFunction);
        }
        // Update function calls to use thread-local sub-functions
        for (Event event : thread.getEvents()) {
            if (event instanceof FunctionCall call && localSubFunctions.containsKey(call.getCalledFunction())) {
                Function calledFunction = call.getCalledFunction();
                Function localSubFunction = localSubFunctions.get(calledFunction).get(thread);
                call.setCallTarget(localSubFunction);
            }
        }
        return thread;
    }

    private void copyFunctionEvents(Function origin, Function target) {
        List<Event> body = new ArrayList<>();
        Map<Event, Event> eventCopyMap = new HashMap<>();
        origin.getEvents().forEach(e -> body.add(eventCopyMap.computeIfAbsent(e, Event::getCopy)));
        for (Event copy : body) {
            if (copy instanceof EventUser user) {
                user.updateReferences(eventCopyMap);
            }
            if (copy instanceof RegReader reader) {
                reader.transformExpressions(transformer);
            }
            if (copy instanceof RegWriter regWriter) {
                regWriter.setResultRegister(transformer.getRegisterMapping(regWriter.getResultRegister()));
            }
        }
        if (!target.hasBody()) {
            body.forEach(target::append);
        } else {
            target.getEntry().insertAfter(body);
        }
    }

    private void transformReturnEvents(Thread thread) {
        Label returnLabel = EventFactory.newLabel("RETURN_OF_T" + thread.getId());
        Label endLabel = EventFactory.newLabel("END_OF_T" + thread.getId());
        for (Return event : thread.getEvents(Return.class)) {
            event.replaceBy(EventFactory.newGoto(returnLabel));
        }
        thread.append(returnLabel);
        thread.append(endLabel);
    }

    private void deleteLocalFunctionVariables() {
        Memory memory = program.getMemory();
        variables.forEach(v -> {
            if (v.getAddress().isThreadLocal()) {
                memory.deleteMemoryObject(v.getAddress());
            }
        });
    }
}
