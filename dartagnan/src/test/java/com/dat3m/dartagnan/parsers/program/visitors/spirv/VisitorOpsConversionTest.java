package com.dat3m.dartagnan.parsers.program.visitors.spirv;

import com.dat3m.dartagnan.expression.type.ScopedPointerType;
import com.dat3m.dartagnan.parsers.program.visitors.spirv.mocks.MockProgramBuilder;
import com.dat3m.dartagnan.parsers.program.visitors.spirv.mocks.MockSpirvParser;
import com.dat3m.dartagnan.program.Register;
import com.dat3m.dartagnan.program.event.Event;
import com.dat3m.dartagnan.program.event.core.Local;
import com.dat3m.dartagnan.program.memory.ScopedPointerVariable;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class VisitorOpsConversionTest {
    private MockProgramBuilder builder = new MockProgramBuilder();

    @Test
    public void doTestOpsBitcastUint32ToUchar() {
        // given
        builder.mockIntType("%uint", 32);
        builder.mockIntType("%uchar", 8);
        builder.mockPtrType("%_ptr_Function_uint", "%uint", "Function");
        builder.mockPtrType("%_ptr_Function_uchar", "%uchar", "Function");
        builder.mockVariable("%value1", "%_ptr_Function_uint");
        String input = "%value2 = OpBitcast %_ptr_Function_uchar %value1";

        // when
        visit(input);

        // then
        ScopedPointerVariable value1 = (ScopedPointerVariable) builder.getExpression("%value1");
        Register value2 = (Register) builder.getExpression("%value2");
        assertEquals(((ScopedPointerType) builder.getType("%_ptr_Function_uint")).getPointedType(), value1.getInnerType());
        Local local = (Local) getLastEvent();
        assertEquals(value2, local.getResultRegister());
        assertEquals(builder.getType("%uchar"), local.getExpr().getType());
    }

    private Event getLastEvent() {
        return getLastNEvent(0);
    }

    private Event getLastNEvent(int n) {
        List<Event> events = builder.getCurrentFunction().getEvents();
        if (!events.isEmpty() && events.size() > n) {
            return events.get(events.size() - 1 - n);
        }
        return null;
    }

    private void visit(String input) {
        builder.mockFunctionStart(true);
        new MockSpirvParser(input).op().accept(new VisitorOpsConversion(builder));
    }
}
