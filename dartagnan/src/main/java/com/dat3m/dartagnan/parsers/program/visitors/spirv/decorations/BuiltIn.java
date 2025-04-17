package com.dat3m.dartagnan.parsers.program.visitors.spirv.decorations;

import com.dat3m.dartagnan.exception.ParsingException;
import com.dat3m.dartagnan.expression.Expression;
import com.dat3m.dartagnan.expression.ExpressionFactory;
import com.dat3m.dartagnan.expression.Type;
import com.dat3m.dartagnan.expression.aggregates.ConstructExpr;
import com.dat3m.dartagnan.expression.type.ArrayType;
import com.dat3m.dartagnan.expression.type.IntegerType;
import com.dat3m.dartagnan.expression.type.TypeFactory;
import com.dat3m.dartagnan.program.ScopeNames;
import com.dat3m.dartagnan.program.ThreadGridHelper;
import com.dat3m.dartagnan.program.event.Tag;
import com.dat3m.dartagnan.program.memory.MemoryObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BuiltIn implements Decoration {

    private static final TypeFactory types = TypeFactory.getInstance();
    private static final ExpressionFactory expressions = ExpressionFactory.getInstance();
    private final Map<String, String> mapping;
    private List<Integer> grid;
    private int tid;
    private ScopeNames scopeNames;

    public BuiltIn() {
        this.mapping = new HashMap<>();
    }
    public void setGrid(List<Integer> grid) {
        this.grid = grid;
    }
    public void setScopeNames(ScopeNames scopeNames) {
        this.scopeNames = scopeNames;
    }

    public void setThreadId(int tid) {
        this.tid = tid;
    }

    @Override
    public void addDecoration(String id, String... params) {
        if (params.length != 1) {
            throw new ParsingException("Illegal decoration '%s' for '%s'",
                    getClass().getSimpleName(), id);
        }
        if (mapping.containsKey(params[0])) {
            throw new ParsingException("Multiple '%s' decorations for '%s'",
                    getClass().getSimpleName(), id);
        }
        mapping.put(id, params[0]);
    }

    public void decorate(String id, MemoryObject memObj, Type type) {
        if (mapping.containsKey(id)) {
            Expression expression = getDecoration(id, type);
            if (expression instanceof ConstructExpr cExpr) {
                Type elementType = getArrayElementType(id, type);
                int size = types.getMemorySizeInBytes(elementType);
                memObj.setInitialValue(0, cExpr.getOperands().get(0));
                memObj.setInitialValue(size, cExpr.getOperands().get(1));
                memObj.setInitialValue(size * 2, cExpr.getOperands().get(2));
            } else {
                memObj.setInitialValue(0, expression);
            }
        }
    }

    public boolean hasDecoration(String id) {
        return mapping.containsKey(id);
    }

    public Expression getDecoration(String id, Type type) {
        if (mapping.containsKey(id)) {
            return getDecorationExpressions(id, type);
        }
        return null;
    }

    private Expression getDecorationExpressions(String id, Type type) {
        if (scopeNames == ScopeNames.VULKAN) {
            return switch (mapping.get(id)) {
                // BuiltIn decorations according to the Vulkan API
                case "SubgroupLocalInvocationId" ->
                        makeScalar(id, type, tid % ThreadGridHelper.getSize(scopeNames, Tag.Vulkan.SUB_GROUP, grid));
                case "LocalInvocationId" ->
                        makeArray(id, type, tid % ThreadGridHelper.getSize(scopeNames, Tag.Vulkan.WORK_GROUP, grid), 0, 0);
                case "LocalInvocationIndex" ->
                        makeScalar(id, type, tid % ThreadGridHelper.getSize(scopeNames, Tag.Vulkan.WORK_GROUP, grid)); // scalar of LocalInvocationId
                case "GlobalInvocationId" ->
                        makeArray(id, type, tid % ThreadGridHelper.getSize(scopeNames, Tag.Vulkan.DEVICE, grid), 0, 0);
                case "DeviceIndex" ->
                        makeScalar(id, type, ThreadGridHelper.getIndex(scopeNames, Tag.Vulkan.DEVICE, grid));
                case "SubgroupId" ->
                        makeScalar(id, type, ThreadGridHelper.getIndex(scopeNames, Tag.Vulkan.SUB_GROUP, grid));
                case "WorkgroupId" ->
                        makeArray(id, type, ThreadGridHelper.getIndex(scopeNames, Tag.Vulkan.WORK_GROUP, grid), 0, 0);
                case "SubgroupSize" ->
                        makeScalar(id, type, ThreadGridHelper.getSize(scopeNames, Tag.Vulkan.SUB_GROUP, grid));
                case "WorkgroupSize" ->
                        makeArray(id, type, ThreadGridHelper.getSize(scopeNames, Tag.Vulkan.WORK_GROUP, grid), 1, 1);
                case "GlobalSize" ->
                        makeArray(id, type, ThreadGridHelper.getSize(scopeNames, Tag.Vulkan.DEVICE, grid), 1, 1);
                case "NumWorkgroups" ->
                        makeArray(id, type, ThreadGridHelper.getSize(scopeNames, Tag.Vulkan.DEVICE, grid) / ThreadGridHelper.getSize(scopeNames, Tag.Vulkan.WORK_GROUP, grid), 1, 1);
                default -> throw new ParsingException("Unsupported decoration '%s'", mapping.get(id));
            };
        }
        if (scopeNames == ScopeNames.OPENCL) {
            return switch (mapping.get(id)) {
                // BuiltIn decorations according to the OpenCL API
                case "GlobalInvocationId" ->
                        makeArray(id, type, tid % ThreadGridHelper.getSize(scopeNames, Tag.OpenCL.ALL, grid), 0, 0);
                case "SubgroupLocalInvocationId" ->
                        makeScalar(id, type, tid % ThreadGridHelper.getSize(scopeNames, Tag.OpenCL.SUB_GROUP, grid));
                case "SubgroupId" ->
                        makeScalar(id, type, ThreadGridHelper.getIndex(scopeNames, Tag.OpenCL.SUB_GROUP, grid));
                case "SubgroupSize" ->
                        makeScalar(id, type, ThreadGridHelper.getSize(scopeNames, Tag.OpenCL.SUB_GROUP, grid));
                case "GlobalSize" ->
                        makeArray(id, type, ThreadGridHelper.getSize(scopeNames, Tag.OpenCL.ALL, grid), 1, 1);
                case "LocalInvocationId" ->
                        makeArray(id, type, tid % ThreadGridHelper.getSize(scopeNames, Tag.OpenCL.WORK_GROUP, grid), 0, 0);
                case "WorkgroupId" ->
                        makeArray(id, type, ThreadGridHelper.getIndex(scopeNames, Tag.OpenCL.WORK_GROUP, grid), 0, 0);
                case "WorkgroupSize" ->
                        makeArray(id, type, ThreadGridHelper.getSize(scopeNames, Tag.OpenCL.WORK_GROUP, grid), 1, 1);
                case "NumWorkgroups" ->
                        makeArray(id, type, ThreadGridHelper.getSize(scopeNames, Tag.OpenCL.DEVICE, grid) / ThreadGridHelper.getSize(scopeNames, Tag.OpenCL.WORK_GROUP, grid), 1, 1);
                default -> throw new ParsingException("Unsupported decoration '%s'", mapping.get(id));
            };
        }
        throw new ParsingException("Unsupported scope '%s'", scopeNames);
    }

    private Expression makeArray(String id, Type type, int x, int y, int z) {
        List<Expression> operands = new ArrayList<>();
        IntegerType elementType = getArrayElementType(id, type);
        operands.add(expressions.makeValue(x, elementType));
        operands.add(expressions.makeValue(y, elementType));
        operands.add(expressions.makeValue(z, elementType));
        return expressions.makeArray(elementType, operands, true);
    }

    private Expression makeScalar(String id, Type type, int x) {
        IntegerType iType = getIntegerType(id, type);
        return expressions.makeValue(x, iType);
    }

    private IntegerType getArrayElementType(String id, Type type) {
        if (type instanceof ArrayType aType && aType.getNumElements() == 3) {
            return getIntegerType(id, aType.getElementType());
        }
        throw new ParsingException("Illegal type of element '%s', " +
                "expected array of three elements but received '%s'", id, type);
    }

    private IntegerType getIntegerType(String id, Type type) {
        if (type instanceof IntegerType iType && iType.getBitWidth() == 32) {
            return iType;
        }
        throw new ParsingException("Illegal type in '%s', " +
                "expected a 32-bit integer but received '%s'", id, type);
    }
}
