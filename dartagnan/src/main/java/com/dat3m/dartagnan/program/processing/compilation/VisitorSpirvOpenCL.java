package com.dat3m.dartagnan.program.processing.compilation;

import com.dat3m.dartagnan.expression.Expression;
import com.dat3m.dartagnan.parsers.program.visitors.spirv.helpers.HelperTags;
import com.dat3m.dartagnan.program.Register;
import com.dat3m.dartagnan.program.event.Event;
import com.dat3m.dartagnan.program.event.EventFactory;
import com.dat3m.dartagnan.program.event.Tag;
import com.dat3m.dartagnan.program.event.arch.opencl.OpenCLRMWExtremumBase;
import com.dat3m.dartagnan.program.event.core.*;
import com.dat3m.dartagnan.program.event.lang.catomic.AtomicFetchOp;
import com.dat3m.dartagnan.program.event.lang.catomic.AtomicXchg;
import com.dat3m.dartagnan.program.event.lang.spirv.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.dat3m.dartagnan.program.event.EventFactory.*;

public class VisitorSpirvOpenCL extends VisitorC11 {

    @Override
    public List<Event> visitInit(Init e) {
        Event init = EventFactory.newInit(e.getBase(), e.getOffset());
        init.removeTags(init.getTags());
        init.addTags(toOpenCLTags(e.getTags()));
        return eventSequence(init);
    }

    @Override
    public List<Event> visitLoad(Load e) {
        Event load = EventFactory.newLoad(e.getResultRegister(), e.getAddress());
        load.removeTags(load.getTags());
        load.addTags(toOpenCLTags(e.getTags()));
        return eventSequence(load);
    }

    @Override
    public List<Event> visitStore(Store e) {
        Event store = EventFactory.newStore(e.getAddress(), e.getMemValue());
        store.removeTags(store.getTags());
        store.addTags(toOpenCLTags(e.getTags()));
        return eventSequence(store);
    }

    @Override
    public List<Event> visitSpirvLoad(SpirvLoad e) {
        String mo = moToOpenCLTag(Tag.Spirv.getMoTag(e.getTags()));
        Load load = newLoadWithMo(e.getResultRegister(), e.getAddress(), mo);
        load.setFunction(e.getFunction());
        load.addTags(Tag.C11.ATOMIC);
        load.addTags(toOpenCLTags(e.getTags()));
        replaceAcqRelTag(load, Tag.C11.MO_ACQUIRE);
        return eventSequence(load);
    }

    @Override
    public List<Event> visitSpirvStore(SpirvStore e) {
        String mo = moToOpenCLTag(Tag.Spirv.getMoTag(e.getTags()));
        Store store = newStoreWithMo(e.getAddress(), e.getMemValue(), mo);
        store.setFunction(e.getFunction());
        store.addTags(Tag.C11.ATOMIC);
        store.addTags(toOpenCLTags(e.getTags()));
        replaceAcqRelTag(store, Tag.C11.MO_RELEASE);
        return eventSequence(store);
    }

    @Override
    public List<Event> visitSpirvXchg(SpirvXchg e) {
        String mo = moToOpenCLTag(Tag.Spirv.getMoTag(e.getTags()));
        String scope = toOpenCLTag(Tag.Spirv.getScopeTag(e.getTags()));
        AtomicXchg rmw = Atomic.newExchange(e.getResultRegister(), e.getAddress(),
                e.getValue(), mo);
        rmw.addTags(scope);
        rmw.addTags(toOpenCLTags(e.getTags()));
        rmw.setFunction(e.getFunction());
        return visitAtomicXchg(rmw);
    }

    @Override
    public List<Event> visitSpirvRMW(SpirvRmw e) {
        String mo = moToOpenCLTag(Tag.Spirv.getMoTag(e.getTags()));
        String scope = toOpenCLTag(Tag.Spirv.getScopeTag(e.getTags()));
        AtomicFetchOp rmwOp = Atomic.newFetchOp(e.getResultRegister(), e.getAddress(),
                e.getOperand(), e.getOperator(), mo);
        rmwOp.addTags(scope);
        rmwOp.setFunction(e.getFunction());
        rmwOp.addTags(toOpenCLTags(e.getTags()));
        return visitAtomicFetchOp(rmwOp);
    }

    @Override
    public List<Event> visitSpirvCmpXchg(SpirvCmpXchg e) {
        Set<String> eqTags = new HashSet<>(e.getEqTags());
        Set<String> neqTags = new HashSet<>(e.getTags());
        String spvMoEq = Tag.Spirv.getMoTag(eqTags);
        String spvMoNeq = Tag.Spirv.getMoTag(neqTags);
        eqTags.remove(spvMoEq);
        neqTags.remove(spvMoNeq);
        if (!eqTags.equals(neqTags) ||
                spvMoNeq.equals(Tag.Spirv.RELAXED) && Set.of(Tag.Spirv.ACQUIRE, Tag.Spirv.ACQ_REL).contains(spvMoEq) ||
                spvMoNeq.equals(Tag.Spirv.ACQUIRE) && spvMoEq.equals(Tag.Spirv.RELEASE)) {
            throw new UnsupportedOperationException(
                    "Spir-V CmpXchg with unequal tag sets is not supported");
        }
        String scope = toOpenCLTag(Tag.Spirv.getScopeTag(e.getTags()));
        String storageClass = toOpenCLTag(Tag.Spirv.getStorageClassTag(e.getTags()));
        String mo = toOpenCLTag(spvMoEq);
        if (mo == null) {
            mo = Tag.C11.MO_RELAXED;
        }
        Register resultRegister = e.getResultRegister();
        Expression address = e.getAddress();
        Expression expected = e.getExpectedValue();
        Expression value = e.getStoreValue();
        Register cmpResultRegister = e.getFunction().newRegister(types.getBooleanType());
        Label casEnd = newLabel("CAS_end");
        Load load = newRMWLoadWithMo(resultRegister, address, Tag.C11.loadMO(mo));
        RMWStore store = newRMWStoreWithMo(load, address, value, Tag.C11.storeMO(mo));
        Local local = newLocal(cmpResultRegister, expressions.makeEQ(resultRegister, expected));
        CondJump condJump = newJumpUnless(cmpResultRegister, casEnd);
        load.addTags(scope, storageClass);
        store.addTags(scope, storageClass);
        return tagList(e, eventSequence(
                load,
                local,
                condJump,
                store,
                casEnd
        ));
    }

    @Override
    public List<Event> visitSpirvRmwExtremum(SpirvRmwExtremum e) {
        String mo = moToOpenCLTag(Tag.Spirv.getMoTag(e.getTags()));
        String scope = toOpenCLTag(Tag.Spirv.getScopeTag(e.getTags()));
        OpenCLRMWExtremumBase rmw = Atomic.newRMWExtremum(e.getResultRegister(), e.getAddress(),
                e.getOperator(), e.getValue(), mo, scope);
        rmw.setFunction(e.getFunction());
        rmw.addTags(toOpenCLTags(e.getTags()));
        return visitOpenCLRMWExtremum(rmw);
    }

    @Override
    public List<Event> visitGenericVisibleEvent(GenericVisibleEvent e) {
        Event fence = new GenericVisibleEvent(e.getName(), Tag.FENCE);
        fence.removeTags(fence.getTags());
        fence.addTags(toOpenCLTags(e.getTags()));
        replaceAcqRelTag(fence, Tag.C11.MO_ACQUIRE, Tag.C11.MO_RELEASE);
        return eventSequence(fence);
    }

    @Override
    public List<Event> visitControlBarrier(ControlBarrier e) {
        Event entryFence = EventFactory.newControlBarrier(e.getName() + "_entry", e.getId());
        entryFence.addTags(Tag.OpenCL.ENTRY_FENCE, Tag.C11.MO_RELEASE);
        Event exitFence = EventFactory.newControlBarrier(e.getName() + "_exit", e.getId());
        exitFence.addTags(Tag.OpenCL.EXIT_FENCE, Tag.C11.MO_ACQUIRE);
        entryFence.addTags(toOpenCLTags(e.getTags()));
        exitFence.addTags(toOpenCLTags(e.getTags()));
        return eventSequence(entryFence, exitFence);
    }

    private Set<String> toOpenCLTags(Set<String> tags) {
        Set<String> openclTags = new HashSet<>();
        tags.forEach(tag -> {
            if (Tag.Spirv.isSpirvTag(tag)) {
                String vTag = toOpenCLTag(tag);
                if (vTag != null) {
                    openclTags.add(vTag);
                }
            } else {
                openclTags.add(tag);
            }
        });
        return openclTags;
    }

    private void replaceAcqRelTag(Event e, String... tags) {
        if (e.getTags().contains(Tag.C11.MO_ACQUIRE_RELEASE)) {
            e.addTags(tags);
            e.removeTags(Tag.C11.MO_ACQUIRE_RELEASE);
        }
    }

    private String moToOpenCLTag(String moSpv) {
        if (Tag.Spirv.RELAXED.equals(moSpv)) {
            return Tag.C11.ATOMIC;
        }
        return toOpenCLTag(moSpv);
    }

    private String toOpenCLTag(String tag) {
        if (HelperTags.getOpenCLStorageClass(tag) != null) {
            return HelperTags.getOpenCLStorageClass(tag);
        }
        return switch (tag) {
            // Barriers
            case Tag.Spirv.CONTROL -> null;

            // Memory order
            case Tag.Spirv.RELAXED -> Tag.C11.MO_RELAXED;
            case Tag.Spirv.ACQUIRE -> Tag.C11.MO_ACQUIRE;
            case Tag.Spirv.RELEASE -> Tag.C11.MO_RELEASE;
            case Tag.Spirv.ACQ_REL -> Tag.C11.MO_ACQUIRE_RELEASE;
            case Tag.Spirv.SEQ_CST -> Tag.C11.MO_SC;

            // Scope
            // TODO: OpenCL Kernel supports sub_group but it's not mentioned in the model
            case Tag.Spirv.INVOCATION -> Tag.OpenCL.WORK_ITEM;
            case Tag.Spirv.SUBGROUP,
                    Tag.Spirv.WORKGROUP -> Tag.OpenCL.WORK_GROUP;
            case Tag.Spirv.DEVICE -> Tag.OpenCL.DEVICE;
            case Tag.Spirv.CROSS_DEVICE -> Tag.OpenCL.ALL;
            case Tag.Spirv.QUEUE_FAMILY,
                    Tag.Spirv.SHADER_CALL -> throw new UnsupportedOperationException(
                    String.format("Spir-V scope '%s' " +
                            "is not supported by OpenCL memory model", tag));


            // Memory access (non-atomic)
            case Tag.Spirv.MEM_VOLATILE,
                    Tag.Spirv.MEM_NONTEMPORAL -> null;
            case Tag.Spirv.MEM_NON_PRIVATE,
                    Tag.Spirv.MEM_AVAILABLE,
                    Tag.Spirv.MEM_VISIBLE -> throw new UnsupportedOperationException(
                    String.format("Spir-V memory access '%s' " +
                            "is not supported by OpenCL memory model", tag));

            // Memory semantics
            case Tag.Spirv.SEM_IMAGE -> null;
            case Tag.Spirv.SEM_SUBGROUP,
                    Tag.Spirv.SEM_WORKGROUP -> Tag.OpenCL.LOCAL_SPACE;
            case Tag.Spirv.SEM_CROSS_WORKGROUP,
                    Tag.Spirv.SEM_ATOMIC_COUNTER -> Tag.OpenCL.GLOBAL_SPACE;
            case Tag.Spirv.SEM_VOLATILE,
                    Tag.Spirv.SEM_UNIFORM,
                    Tag.Spirv.SEM_OUTPUT,
                    Tag.Spirv.SEM_AVAILABLE,
                    Tag.Spirv.SEM_VISIBLE -> throw new UnsupportedOperationException(
                    String.format("Spir-V memory semantics '%s' " +
                            "is not supported by OpenCL memory model", tag));

            // Storage class
            case Tag.Spirv.SC_PUSH_CONSTANT,
                    Tag.Spirv.SC_UNIFORM,
                    Tag.Spirv.SC_OUTPUT,
                    Tag.Spirv.SC_STORAGE_BUFFER,
                    Tag.Spirv.SC_PRIVATE-> throw new UnsupportedOperationException(
                    String.format("Spir-V storage class '%s' " +
                            "is not supported by OpenCL memory model", tag));

            default -> throw new IllegalArgumentException(
                    String.format("Unexpected non Spir-V tag '%s'", tag));
        };
    }
}
