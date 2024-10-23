package com.dat3m.dartagnan.parsers.program.visitors.spirv;

import com.dat3m.dartagnan.exception.ParsingException;
import com.dat3m.dartagnan.expression.Expression;
import com.dat3m.dartagnan.expression.ExpressionFactory;
import com.dat3m.dartagnan.expression.Type;
import com.dat3m.dartagnan.expression.type.AggregateType;
import com.dat3m.dartagnan.expression.type.ArrayType;
import com.dat3m.dartagnan.parsers.SpirvBaseVisitor;
import com.dat3m.dartagnan.parsers.SpirvParser;
import com.dat3m.dartagnan.parsers.program.visitors.spirv.builders.ProgramBuilder;
import com.dat3m.dartagnan.program.Register;
import com.dat3m.dartagnan.program.event.Event;
import com.dat3m.dartagnan.program.event.EventFactory;
import com.dat3m.dartagnan.program.event.core.Local;
import org.antlr.v4.runtime.RuleContext;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class VisitorOpsComposite extends SpirvBaseVisitor<Event> {

    private static final ExpressionFactory expressions = ExpressionFactory.getInstance();

    private final ProgramBuilder builder;

    public VisitorOpsComposite(ProgramBuilder builder) {
        this.builder = builder;
    }

    @Override
    public Event visitOpCompositeExtract(SpirvParser.OpCompositeExtractContext ctx) {
        String id = ctx.idResult().getText();
        String typeId = ctx.idResultType().getText();
        String compositeId = ctx.composite().getText();
        Expression composite = builder.getExpression(compositeId);
        int index = Integer.parseInt(ctx.indexesLiteralInteger().stream().map(RuleContext::getText)
                .collect(Collectors.joining()));
        Expression result = extractCompositeElement(composite, index, typeId);
        Register register = builder.addRegister(id, typeId);
        Local event = EventFactory.newLocal(register, result);
        return builder.addEvent(event);
    }

    private Expression extractCompositeElement(Expression composite, int index, String typeId) {
        Type type = builder.getType(typeId);
        if (composite.getType() instanceof AggregateType aType) {
            List<Type> elementTypes = aType.getDirectFields();
            if (index >= elementTypes.size()) {
                throw new ParsingException("Index out of bounds for composite '%s', " +
                        "expected index less than %d but received %d", composite, elementTypes.size(), index);
            }
            Type elementType = elementTypes.get(index);
            if (!elementType.equals(type)) {
                throw new ParsingException("Mismatching type of a composite '%s' element '%d', " +
                        "expected '%s' but received '%s'", composite, index, elementType, type);
            }
            return expressions.makeExtract(index, composite);
        } else if (composite.getType() instanceof ArrayType aType) {
            if (index >= aType.getNumElements()) {
                throw new ParsingException("Index out of bounds for array '%s', " +
                        "expected index less than %d but received %d", composite, aType.getNumElements(), index);
            }
            Type elementType = aType.getElementType();
            if (!elementType.equals(type)) {
                throw new ParsingException("Mismatching type of an array '%s' element '%d', " +
                        "expected '%s' but received '%s'", composite, index, elementType, type);
            }
            return expressions.makeExtract(index, composite);
        }
        throw new ParsingException("Illegal composite type '%s'", composite.getType());
    }

    public Set<String> getSupportedOps() {
        return Set.of(
                "OpCompositeExtract"
        );
    }
}
