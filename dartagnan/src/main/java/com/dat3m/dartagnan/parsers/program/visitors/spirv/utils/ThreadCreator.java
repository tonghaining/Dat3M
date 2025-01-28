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
    private final List<Function> subFunctions;
    private final Set<ScopedPointerVariable> variables;
    private final MemoryTransformer transformer;
    private final Map<Function, Map<Thread, Function>> threadLocalFunctions;

    public ThreadCreator(ThreadGrid grid, Function entryFunction, List<Function> subFunctions,
                         Set<ScopedPointerVariable> variables, BuiltIn builtIn) {
        this.grid = grid;
        this.entryFunction = entryFunction;
        this.subFunctions = subFunctions;
        this.variables = variables;
        this.transformer = new MemoryTransformer(grid, entryFunction, subFunctions, builtIn, variables);
        this.threadLocalFunctions = new HashMap<>();
        for (Function subFunction : subFunctions) {
            threadLocalFunctions.put(subFunction, new HashMap<>());
        }
    }

    public void create() {
        Program program = entryFunction.getProgram();
        for (int i = 0; i < grid.dvSize(); i++) {
            Thread thread = createThreadFromFunction(i);
            program.addThread(thread);
            transformer.setThread(thread);
            for (Function subFunction : subFunctions) {
                Function threadLocalFunction = createThreadLocalFunction(subFunction, i);
                program.addFunction(threadLocalFunction);
                threadLocalFunctions.get(subFunction).put(thread, threadLocalFunction);
            }
            copyThreadEvents(thread);
            transformReturnEvents(thread);
        }
        deleteLocalFunctionVariables();
    }

    private Function createThreadLocalFunction(Function subFunction, int tid) {
        String name = subFunction.getName() + "@T" + tid;
        FunctionType type = subFunction.getFunctionType();
        List<String> args = Lists.transform(subFunction.getParameterRegisters(), Register::getName);
        Function function = new Function(name, type, args, subFunction.getId() + 31 * tid, subFunction.getEntry().getCopy());
        function.copyDummyCountFrom(subFunction);
        copyFunctionEvents(subFunction, function);
        subFunction.copyDummyCountFrom(function);
        return function;
    }

    private Thread createThreadFromFunction(int tid) {
        String name = entryFunction.getName();
        FunctionType type = entryFunction.getFunctionType();
        List<String> args = Lists.transform(entryFunction.getParameterRegisters(), Register::getName);
        ThreadStart start = EventFactory.newThreadStart(null);
        ScopeHierarchy scope = grid.getScoreHierarchy(tid);
        Thread thread = new Thread(name, type, args, tid, start, scope, Set.of());
        thread.copyDummyCountFrom(entryFunction);
        return thread;
    }

    private void copyThreadEvents(Thread thread) {
        List<Event> body = new ArrayList<>();
        Map<Event, Event> eventCopyMap = new HashMap<>();
        entryFunction.getEvents().forEach(e -> body.add(eventCopyMap.computeIfAbsent(e, Event::getCopy)));
        updateCopiedEvents(body, eventCopyMap);
        for (Event copy : body) {
            if (copy instanceof FunctionCall call) {
                call.setCallTarget(threadLocalFunctions.get(call.getCalledFunction()).get(thread));
            }
        }
        thread.getEntry().insertAfter(body);
    }

    private void copyFunctionEvents(Function originalFunction, Function newFunction) {
        List<Event> body = new ArrayList<>();
        Map<Event, Event> eventCopyMap = new HashMap<>();
        originalFunction.getEvents().forEach(e -> body.add(eventCopyMap.computeIfAbsent(e, Event::getCopy)));
        body.remove(0);
        updateCopiedEvents(body, eventCopyMap);
        newFunction.getEntry().insertAfter(body);
    }

    private void updateCopiedEvents(List<Event> events, Map<Event, Event> eventCopyMap) {
        for (Event copy : events) {
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
        Memory entryMemory = entryFunction.getProgram().getMemory();
        variables.forEach(v -> {
            if (v.getAddress().isThreadLocal()) {
                entryMemory.deleteMemoryObject(v.getAddress());
            }
        });
        for (Function subFunction : subFunctions) {
            Memory subMemory = subFunction.getProgram().getMemory();
            variables.forEach(v -> {
                if (v.getAddress().isThreadLocal()) {
                    subMemory.deleteMemoryObject(v.getAddress());
                }
            });
        }
    }
}
