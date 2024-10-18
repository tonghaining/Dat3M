package com.dat3m.dartagnan.parsers.program.visitors.spirv;

import com.dat3m.dartagnan.exception.ParsingException;
import com.dat3m.dartagnan.expression.Expression;
import com.dat3m.dartagnan.expression.ExpressionFactory;
import com.dat3m.dartagnan.expression.Type;
import com.dat3m.dartagnan.expression.integers.IntLiteral;
import com.dat3m.dartagnan.expression.type.AggregateType;
import com.dat3m.dartagnan.expression.type.ArrayType;
import com.dat3m.dartagnan.expression.type.BooleanType;
import com.dat3m.dartagnan.expression.type.IntegerType;
import com.dat3m.dartagnan.parsers.SpirvBaseVisitor;
import com.dat3m.dartagnan.parsers.SpirvParser;
import com.dat3m.dartagnan.parsers.program.visitors.spirv.builders.ProgramBuilder;
import com.dat3m.dartagnan.parsers.program.visitors.spirv.decorations.BuiltIn;
import com.dat3m.dartagnan.parsers.program.visitors.spirv.decorations.SpecId;
import com.dat3m.dartagnan.program.Register;
import org.antlr.v4.runtime.RuleContext;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.dat3m.dartagnan.parsers.program.visitors.spirv.decorations.DecorationType.BUILT_IN;
import static com.dat3m.dartagnan.parsers.program.visitors.spirv.decorations.DecorationType.SPEC_ID;

public class VisitorOpsComposite extends SpirvBaseVisitor<Expression> {

    private static final ExpressionFactory expressions = ExpressionFactory.getInstance();

    private final ProgramBuilder builder;

    public VisitorOpsComposite(ProgramBuilder builder) {
        this.builder = builder;
    }

    @Override
    public Expression visitOpCompositeExtract(SpirvParser.OpCompositeExtractContext ctx) {
        String id = ctx.idResult().getText();
        Type type = builder.getType(ctx.idResultType().getText());
        String compositeId = ctx.composite().getText();
        Expression composite = builder.getExpression(compositeId);
        int index = Integer.parseInt(ctx.indexesLiteralInteger().stream().map(RuleContext::getText)
                .collect(Collectors.joining()));
        return builder.addExpression(id, makeCompositeElement(composite, index, type));
    }

    private Expression makeCompositeElement(Expression composite, int index, Type type) {
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
