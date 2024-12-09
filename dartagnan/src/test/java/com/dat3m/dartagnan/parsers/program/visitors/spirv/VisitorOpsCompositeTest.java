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
        builder.mockVariable("%value1", "%_ptr_Function_uint_4");
        String input = "%value2 = OpCompositeExtract %uint %value1 0";

        // when
        visit(builder, input);

        // then
        Register value2 = (Register) builder.getExpression("%value2");
        assertEquals(builder.getType("%uint"), value2.getType());
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
        builder.mockVariable("%value1", "%_ptr_Function_struct");
        String input = "%value2 = OpCompositeExtract %uchar %value1 1";

        // when
        visit(builder, input);

        // then
        Register value2 = (Register) builder.getExpression("%value2");
        assertEquals(builder.getType("%uchar"), value2.getType());
    }

    private void visit(MockProgramBuilder builder, String input) {
        new MockSpirvParser(input).op().accept(new VisitorOpsComposite(builder));
    }
}
