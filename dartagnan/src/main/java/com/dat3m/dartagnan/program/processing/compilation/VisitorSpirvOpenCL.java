package com.dat3m.dartagnan.program.processing.compilation;

import com.dat3m.dartagnan.program.event.Event;
import com.dat3m.dartagnan.program.event.EventFactory;
import com.dat3m.dartagnan.program.event.Tag;
import com.dat3m.dartagnan.program.event.arch.opencl.OpenCLRMWExtremum;
import com.dat3m.dartagnan.program.event.core.ControlBarrier;
import com.dat3m.dartagnan.program.event.core.GenericVisibleEvent;
import com.dat3m.dartagnan.program.event.core.Load;
import com.dat3m.dartagnan.program.event.core.Store;
import com.dat3m.dartagnan.program.event.lang.catomic.AtomicCmpXchg;
import com.dat3m.dartagnan.program.event.lang.catomic.AtomicFetchOp;
import com.dat3m.dartagnan.program.event.lang.catomic.AtomicXchg;
import com.dat3m.dartagnan.program.event.lang.spirv.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.dat3m.dartagnan.program.event.EventFactory.*;

public class VisitorSpirvOpenCL extends VisitorC11 {

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
        e.addTags(Tag.Spirv.MEM_VISIBLE, Tag.Spirv.MEM_NON_PRIVATE);
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
        e.addTags(Tag.Spirv.MEM_AVAILABLE, Tag.Spirv.MEM_NON_PRIVATE);
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
        e.addTags(Tag.Spirv.MEM_VISIBLE, Tag.Spirv.MEM_AVAILABLE, Tag.Spirv.MEM_NON_PRIVATE);
        String mo = moToOpenCLTag(Tag.Spirv.getMoTag(e.getTags()));
        String scope = toSpirvTag(Tag.Spirv.getScopeTag(e.getTags()));
        AtomicXchg rmw = Atomic.newExchange(e.getResultRegister(), e.getAddress(),
                e.getValue(), mo);
        rmw.addTags(scope);
        rmw.addTags(toOpenCLTags(e.getTags()));
        rmw.setFunction(e.getFunction());
        return visitAtomicXchg(rmw);
    }

    @Override
    public List<Event> visitSpirvRMW(SpirvRmw e) {
        e.addTags(Tag.Spirv.MEM_VISIBLE, Tag.Spirv.MEM_AVAILABLE, Tag.Spirv.MEM_NON_PRIVATE);
        String mo = moToOpenCLTag(Tag.Spirv.getMoTag(e.getTags()));
        String scope = toSpirvTag(Tag.Spirv.getScopeTag(e.getTags()));
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
        String scope = toSpirvTag(Tag.Spirv.getScopeTag(e.getTags()));
        eqTags.addAll(Set.of(Tag.Spirv.MEM_VISIBLE, Tag.Spirv.MEM_AVAILABLE, Tag.Spirv.MEM_NON_PRIVATE));
        AtomicCmpXchg cmpXchg = Atomic.newCompareExchange(e.getResultRegister(), e.getAddress(),
                e.getExpectedValue(), e.getStoreValue(), moToOpenCLTag(spvMoEq));
        cmpXchg.addTags(scope);
        cmpXchg.setFunction(e.getFunction());
        cmpXchg.addTags(toOpenCLTags(eqTags));

        return visitAtomicCmpXchg(cmpXchg);
    }

    @Override
    public List<Event> visitSpirvRmwExtremum(SpirvRmwExtremum e) {
        e.addTags(Tag.Spirv.MEM_VISIBLE, Tag.Spirv.MEM_AVAILABLE, Tag.Spirv.MEM_NON_PRIVATE);
        String mo = moToOpenCLTag(Tag.Spirv.getMoTag(e.getTags()));
        String scope = toSpirvTag(Tag.Spirv.getScopeTag(e.getTags()));
        OpenCLRMWExtremum rmw = Atomic.newRMWExtremum(e.getResultRegister(), e.getAddress(),
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
        Event barrier = EventFactory.newControlBarrier(e.getName(), e.getId());
        barrier.removeTags(barrier.getTags());
        barrier.addTags(toOpenCLTags(e.getTags()));
        replaceAcqRelTag(barrier, Tag.C11.MO_ACQUIRE, Tag.C11.MO_RELEASE);
        return eventSequence(barrier);
    }

    private Set<String> toOpenCLTags(Set<String> tags) {
        Set<String> openclTags = new HashSet<>();
        tags.forEach(tag -> {
            if (Tag.Spirv.isSpirvTag(tag)) {
                String vTag = toSpirvTag(tag);
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
        return toSpirvTag(moSpv);
    }

    private String toSpirvTag(String tag) {
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
            case Tag.Spirv.SUBGROUP -> null;
            case Tag.Spirv.WORKGROUP -> Tag.OpenCL.WORK_GROUP;
            case Tag.Spirv.QUEUE_FAMILY -> null;
            case Tag.Spirv.INVOCATION,
                    Tag.Spirv.SHADER_CALL,
                    Tag.Spirv.DEVICE -> Tag.OpenCL.DEVICE;
            case Tag.Spirv.CROSS_DEVICE -> Tag.OpenCL.ALL;

            // Memory access (non-atomic)
            case Tag.Spirv.MEM_VOLATILE,
                    Tag.Spirv.MEM_NONTEMPORAL -> null;
            case Tag.Spirv.MEM_NON_PRIVATE -> null;
            case Tag.Spirv.MEM_AVAILABLE -> null;
            case Tag.Spirv.MEM_VISIBLE -> null;

            // Memory semantics
            case Tag.Spirv.SEM_VOLATILE,
                    Tag.Spirv.SEM_AVAILABLE,
                    Tag.Spirv.SEM_VISIBLE -> throw new UnsupportedOperationException(
                    String.format("Spir-V memory semantics '%s' " +
                            "is not supported by OpenCL memory model", tag));

            // Storage class
            case Tag.Spirv.SC_GENERIC -> null;
            case Tag.Spirv.SC_FUNCTION -> Tag.OpenCL.LOCAL_SPACE;
            case Tag.Spirv.SEM_WORKGROUP,
                    Tag.Spirv.SC_UNIFORM_CONSTANT,
                    Tag.Spirv.SC_INPUT,
                    Tag.Spirv.SC_UNIFORM,
                    Tag.Spirv.SC_PHYS_STORAGE_BUFFER,
                    Tag.Spirv.SC_WORKGROUP,
                    Tag.Spirv.SC_CROSS_WORKGROUP,
                    Tag.Spirv.SEM_SUBGROUP,
                    Tag.Spirv.SEM_CROSS_WORKGROUP,
                    Tag.Spirv.SEM_ATOMIC_COUNTER,
                    Tag.Spirv.SEM_IMAGE-> Tag.OpenCL.GLOBAL_SPACE;

            // Unsupported storage class
            case Tag.Spirv.SEM_UNIFORM,
                    Tag.Spirv.SEM_OUTPUT,
                    Tag.Spirv.SC_PUSH_CONSTANT,
                    Tag.Spirv.SC_STORAGE_BUFFER,
                    Tag.Spirv.SC_PRIVATE-> throw new UnsupportedOperationException(
                    String.format("Spir-V memory semantics '%s' " +
                            "is not supported by OpenCL memory model", tag));

            default -> throw new IllegalArgumentException(
                    String.format("Unexpected non Spir-V tag '%s'", tag));
        };
    }
}
