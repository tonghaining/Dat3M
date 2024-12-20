package com.dat3m.dartagnan.parsers.program.visitors.spirv;

import com.dat3m.dartagnan.expression.Expression;
import com.dat3m.dartagnan.expression.ExpressionFactory;
import com.dat3m.dartagnan.expression.integers.IntLiteral;
import com.dat3m.dartagnan.expression.type.ArrayType;
import com.dat3m.dartagnan.expression.type.TypeFactory;
import com.dat3m.dartagnan.parsers.program.visitors.spirv.helpers.HelperTypes;
import com.dat3m.dartagnan.parsers.program.visitors.spirv.mocks.MockProgramBuilder;
import com.dat3m.dartagnan.parsers.program.visitors.spirv.mocks.MockSpirvParser;
import com.dat3m.dartagnan.program.Register;
import com.dat3m.dartagnan.program.event.Event;
import com.dat3m.dartagnan.program.event.EventFactory;
import org.junit.Test;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class VisitorOpsCompositeTest {

    @Test
    public void doTestCompositeExtractArray() {
        // given
        MockProgramBuilder builder = new MockProgramBuilder();
        builder.mockFunctionStart(true);
        builder.mockIntType("%uint", 32);
        ArrayType arrayType = builder.mockVectorType("%uint_4", "%uint", 4);
        builder.mockPtrType("%_ptr_Function_uint_4", "%uint_4", "Function");
        Expression pointer = builder.mockVariable("%value", "%_ptr_Function_uint_4");
        List<Expression> registers = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            Register register = builder.addRegister("%r" + i, "%uint");
            List<Expression> index = List.of(new IntLiteral(TypeFactory.getInstance().getArchType(), new BigInteger(Long.toString(i))));
            Expression elementPointer = HelperTypes.getMemberAddress("%value", pointer, arrayType, index);
            Event load = EventFactory.newLoad(register, elementPointer);
            builder.addEvent(load);
            registers.add(register);
        }
        Expression arrayRegister = ExpressionFactory.getInstance().makeArray(arrayType.getElementType(), registers, true);
        builder.addExpression("%composite_value", arrayRegister);
        String input = "%extract_value = OpCompositeExtract %uint %composite_value 0";

        // when
        visit(builder, input);

        // then
        Register compositeExtract = (Register) builder.getExpression("%extract_value");
        assertEquals(builder.getType("%uint"), compositeExtract.getType());
        assertEquals(registers.get(0), compositeExtract);
    }

    private void visit(MockProgramBuilder builder, String input) {
        new MockSpirvParser(input).op().accept(new VisitorOpsComposite(builder));
    }
}
