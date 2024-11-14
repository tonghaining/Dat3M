package com.dat3m.dartagnan.parsers.program.visitors.spirv;

import com.dat3m.dartagnan.expression.Expression;
import com.dat3m.dartagnan.expression.type.ScopedPointerType;
import com.dat3m.dartagnan.parsers.program.visitors.spirv.mocks.MockProgramBuilder;
import com.dat3m.dartagnan.parsers.program.visitors.spirv.mocks.MockSpirvParser;
import com.dat3m.dartagnan.program.memory.ScopedPointerVariable;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class VisitorOpsConversionTest {
    @Test
    public void doTestOpsBitcastUint32ToUchar() {
        // given
        MockProgramBuilder builder = new MockProgramBuilder();
        builder.mockIntType("%uint", 32);
        builder.mockIntType("%uchar", 8);
        builder.mockPtrType("%_ptr_Function_uint", "%uint", "Function");
        builder.mockPtrType("%_ptr_Function_uchar", "%uchar", "Function");
        builder.mockVariable("%value1", "%_ptr_Function_uint");
        String input = "%value2 = OpBitcast %_ptr_Function_uchar %value1";

        // when
        visit(builder, input);

        // then
        ScopedPointerVariable value1 = (ScopedPointerVariable) builder.getExpression("%value1");
        ScopedPointerVariable value2 = (ScopedPointerVariable) builder.getExpression("%value2");
        assertEquals(((ScopedPointerType) builder.getType("%_ptr_Function_uint")).getPointedType(), value1.getInnerType());
        assertEquals(((ScopedPointerType) builder.getType("%_ptr_Function_uchar")).getPointedType(), value2.getInnerType());
        assertEquals(value1.getAddress(), value2.getAddress());
    }

    @Test
    public void doTestOpsBitcastUint32VectorToUcharVector() {
        // given
        MockProgramBuilder builder = new MockProgramBuilder();
        builder.mockIntType("%uint", 32);
        builder.mockIntType("%uchar", 8);
        builder.mockVectorType("%uint_4", "%uint", 4);
        builder.mockVectorType("%uchar_4", "%uchar", 4);
        builder.mockPtrType("%_ptr_Function_uint_4", "%uint_4", "Function");
        builder.mockPtrType("%_ptr_Function_uchar_4", "%uchar_4", "Function");
        builder.mockVariable("%value1", "%_ptr_Function_uint_4");
        String input = "%value2 = OpBitcast %_ptr_Function_uchar_4 %value1";

        // when
        visit(builder, input);

        // then
        ScopedPointerVariable value1 = (ScopedPointerVariable) builder.getExpression("%value1");
        ScopedPointerVariable value2 = (ScopedPointerVariable) builder.getExpression("%value2");
        assertEquals(((ScopedPointerType) builder.getType("%_ptr_Function_uint_4")).getPointedType(), value1.getInnerType());
        assertEquals(((ScopedPointerType) builder.getType("%_ptr_Function_uchar_4")).getPointedType(), value2.getInnerType());
        assertEquals(value1.getAddress(), value2.getAddress());
    }

    private void visit(MockProgramBuilder builder, String input) {
        builder.mockFunctionStart(true);
        new MockSpirvParser(input).op().accept(new VisitorOpsConversion(builder));
    }
}
