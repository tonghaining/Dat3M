package com.dat3m.dartagnan.parsers.program.visitors.spirv;

import com.dat3m.dartagnan.expression.Expression;
import com.dat3m.dartagnan.expression.aggregates.ExtractExpr;
import com.dat3m.dartagnan.expression.type.ScopedPointerType;
import com.dat3m.dartagnan.parsers.program.visitors.spirv.mocks.MockProgramBuilder;
import com.dat3m.dartagnan.parsers.program.visitors.spirv.mocks.MockSpirvParser;
import com.dat3m.dartagnan.program.Register;
import com.dat3m.dartagnan.program.event.EventFactory;
import com.dat3m.dartagnan.program.event.core.Load;
import com.dat3m.dartagnan.program.memory.ScopedPointerVariable;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class VisitorOpsCompositeTest {

    @Test
    public void doTestCompositeExtractArray() {
        // given
        MockProgramBuilder builder = new MockProgramBuilder();
        builder.mockFunctionStart(true);
        builder.mockIntType("%uint", 32);
        builder.mockVectorType("%uint_4", "%uint", 4);
        builder.mockPtrType("%_ptr_Function_uint_4", "%uint_4", "Function");
        Expression pointer = builder.mockVariable("%value1", "%_ptr_Function_uint_4");
        Register register = builder.addRegister("%value2", "%uint_4");
        Load load = EventFactory.newLoad(register, pointer);
        builder.addEvent(load);
        String input = "%value3 = OpCompositeExtract %uint %value2 0";

        // when
        visit(builder, input);

        // then
        ExtractExpr value3 = (ExtractExpr) builder.getExpression("%value3");
        assertEquals(builder.getType("%uint"), value3.getType());
        assertEquals(0, value3.getFieldIndex());
        assertEquals(register, value3.getOperand());
    }

    @Test
    public void doTestCompositeExtractStruct() {
        // given
        MockProgramBuilder builder = new MockProgramBuilder();
        builder.mockFunctionStart(true);
        builder.mockIntType("%uint", 32);
        builder.mockIntType("%uchar", 8);
        builder.mockAggregateType("%struct", "%uint", "%uchar");
        builder.mockPtrType("%_ptr_Function_struct", "%struct", "Function");
        Expression pointer = builder.mockVariable("%value1", "%_ptr_Function_struct");
        Register register = builder.addRegister("%value2", "%struct");
        Load load = EventFactory.newLoad(register, pointer);
        builder.addEvent(load);
        String input = "%value3 = OpCompositeExtract %uchar %value2 1";

        // when
        visit(builder, input);

        // then
        ExtractExpr value3 = (ExtractExpr) builder.getExpression("%value3");
        assertEquals(builder.getType("%uchar"), value3.getType());
        assertEquals(1, value3.getFieldIndex());
        assertEquals(register, value3.getOperand());
    }

    private void visit(MockProgramBuilder builder, String input) {
        new MockSpirvParser(input).op().accept(new VisitorOpsComposite(builder));
    }
}
