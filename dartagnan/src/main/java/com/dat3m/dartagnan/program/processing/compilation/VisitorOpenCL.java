package com.dat3m.dartagnan.program.processing.compilation;

import com.dat3m.dartagnan.configuration.Arch;
import com.dat3m.dartagnan.expression.Expression;
import com.dat3m.dartagnan.expression.type.BooleanType;
import com.dat3m.dartagnan.expression.Type;
import com.dat3m.dartagnan.program.Register;
import com.dat3m.dartagnan.program.event.Tag;
import com.dat3m.dartagnan.program.event.Tag.C11;
import com.dat3m.dartagnan.program.event.Event;
import com.dat3m.dartagnan.program.event.MemoryEvent;
import com.dat3m.dartagnan.program.event.core.*;
import com.dat3m.dartagnan.program.event.core.RMWStore;
import com.dat3m.dartagnan.program.event.lang.catomic.*;
import com.dat3m.dartagnan.program.event.lang.opencl.OpenCLBarrier;
import com.dat3m.dartagnan.program.event.lang.opencl.OpenCLFence;
import com.dat3m.dartagnan.program.event.metadata.MemoryOrder;

import java.util.List;

import static com.dat3m.dartagnan.program.event.EventFactory.*;

public class VisitorOpenCL extends VisitorBase {

    @Override
    public List<Event> visitOpenCLFence(OpenCLFence e) {
        return eventSequence(
                newFence(e.getName())
        );
    }

    @Override
    public List<Event> visitOpenCLBarrier(OpenCLBarrier e) {
        FenceWithId entryFence = new FenceWithId(e.getName() + "_entry", e.getFenceID());
        FenceWithId exitFence = new FenceWithId(e.getName() + "_exit", e.getFenceID());
        String scope = Tag.getScopeTag(e, Arch.OPENCL);
        String fenceFlag = Tag.OpenCL.getFenceFlagTag(e);
        entryFence.addTags(Tag.OpenCL.ENTRY_FENCE, C11.MO_RELEASE, fenceFlag);
        exitFence.addTags(Tag.OpenCL.EXIT_FENCE, C11.MO_ACQUIRE, fenceFlag);
        return tagList(scope, eventSequence(
                entryFence,
                exitFence
        ));
    }

    @Override
    public List<Event> visitAtomicCmpXchg(AtomicCmpXchg e) {
        String scope = Tag.getScopeTag(e, Arch.OPENCL);
        Register resultRegister = e.getResultRegister();
        Expression address = e.getAddress();
        String mo = e.getMo();
        Expression expectedAddr = e.getAddressOfExpected();
        Type type = resultRegister.getType();
        Register booleanResultRegister = type instanceof BooleanType ? resultRegister :
                e.getFunction().newRegister(types.getBooleanType());
        Local castResult = type instanceof BooleanType ? null :
                newLocal(resultRegister, expressions.makeCast(booleanResultRegister, type));
        Register regExpected = e.getFunction().newRegister(type);
        Register regValue = e.getFunction().newRegister(type);
        Load loadExpected = newLoad(regExpected, expectedAddr);
        Store storeExpected = newStore(expectedAddr, regValue);
        Label casFail = newLabel("CAS_fail");
        Label casEnd = newLabel("CAS_end");
        Local casCmpResult = newLocal(booleanResultRegister, expressions.makeEQ(regValue, regExpected));
        CondJump branchOnCasCmpResult = newJumpUnless(booleanResultRegister, casFail);
        CondJump gotoCasEnd = newGoto(casEnd);
        Load loadValue = newRMWLoadWithMo(regValue, address, mo);
        Store storeValue = newRMWStoreWithMo(loadValue, address, e.getStoreValue(), mo);

        return tagList(scope, eventSequence(
                loadExpected,
                loadValue,
                casCmpResult,
                branchOnCasCmpResult,
                storeValue,
                gotoCasEnd,
                casFail,
                storeExpected,
                casEnd,
                castResult
        ));
    }

    @Override
    public List<Event> visitAtomicFetchOp(AtomicFetchOp e) {
        String scope = Tag.getScopeTag(e, Arch.OPENCL);
        Register resultRegister = e.getResultRegister();
        Expression address = e.getAddress();
        String mo = e.getMo();

        Register dummyReg = e.getFunction().newRegister(resultRegister.getType());
        Load load = newRMWLoadWithMo(resultRegister, address, mo);
        Local localOp = newLocal(dummyReg, expressions.makeBinary(resultRegister, e.getOperator(), e.getOperand()));
        RMWStore store = newRMWStoreWithMo(load, address, dummyReg, mo);

        return tagList(scope, eventSequence(
                load,
                localOp,
                store
        ));
    }

    @Override
    public List<Event> visitAtomicLoad(AtomicLoad e) {
        String scope = Tag.getScopeTag(e, Arch.OPENCL);
        return tagList(scope, eventSequence(
                newLoadWithMo(e.getResultRegister(), e.getAddress(), e.getMo())
        ));
    }

    @Override
    public List<Event> visitAtomicStore(AtomicStore e) {
        String scope = Tag.getScopeTag(e, Arch.OPENCL);
        return tagList(scope, eventSequence(
                newStoreWithMo(e.getAddress(), e.getMemValue(), e.getMo())
        ));
    }

    private List<Event> tagList(String scope, List<Event> in) {
        in.forEach(this::tagEvent);
        in.forEach(e -> e.addTags(scope));
        return in;
    }

    private void tagEvent(Event e) {
        if (e instanceof MemoryEvent) {
            final MemoryOrder mo = e.getMetadata(MemoryOrder.class);
            final boolean canRace = mo == null || mo.value().equals(C11.NONATOMIC);
            e.addTags(canRace ? C11.NONATOMIC : C11.ATOMIC);
        }
    }
}
