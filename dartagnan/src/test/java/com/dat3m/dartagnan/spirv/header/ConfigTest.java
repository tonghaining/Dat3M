package com.dat3m.dartagnan.spirv.header;

import com.dat3m.dartagnan.exception.ParsingException;
import com.dat3m.dartagnan.program.Program;
import com.dat3m.dartagnan.program.ScopeNames;
import com.dat3m.dartagnan.program.ThreadGridHelper;
import com.dat3m.dartagnan.program.event.Tag;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ConfigTest extends AbstractTest {

    @Test
    public void testExplicitConfig() {
        doTestLegalConfig("", List.of(1, 1, 1));
        doTestLegalConfig("; @Config: 1, 1, 1", List.of(1, 1, 1));
        doTestLegalConfig("; @Config: 2, 3, 4", List.of(2, 3, 4));
        doTestLegalConfig("; @Config: 4, 3, 2", List.of(4, 3, 2));
    }

    private void doTestLegalConfig(String input, List<Integer> scopes) {
        // when
        Program program = parse(input);

        // then
        int size = scopes.stream().reduce(1, (a, b) -> a * b);
        List<Integer> grid = program.getGrid();
        ScopeNames scopeNames = ScopeNames.getByArch(program.getArch());
        assertEquals(size, ThreadGridHelper.getSize(scopeNames, Tag.Vulkan.DEVICE, grid));

        int sg_size = scopes.get(0);
        int wg_size = scopes.get(1) * sg_size;
        int qf_size = scopes.get(2) * wg_size;
        for (int i = 0; i < size; i++) {
            assertEquals(((i % qf_size) % wg_size) / sg_size, ThreadGridHelper.getIndex(scopeNames, Tag.Vulkan.SUB_GROUP, grid));
            assertEquals((i % qf_size) / wg_size, ThreadGridHelper.getIndex(scopeNames, Tag.Vulkan.WORK_GROUP, grid));
            assertEquals(i / qf_size, ThreadGridHelper.getIndex(scopeNames, Tag.Vulkan.QUEUE_FAMILY, grid));
            assertEquals(0, ThreadGridHelper.getIndex(scopeNames, Tag.Vulkan.DEVICE, grid));
        }
    }

    @Test
    public void testIllegalConfig() {
        doTestIllegalConfig("; @Config: 1, 1",
                "Line 2:16 mismatched input 'Op' expecting ','");
        doTestIllegalConfig("; @Config: 1, 1, 0",
                "Thread grid of Vulkan dimensions must be of length 3 and positive");
        doTestIllegalConfig("""
                        ; @Output: forall (1 == 1)
                        ; @Config: 1, 1, 1
                        ; @Config: 1, 1, 1
                        """,
                "Multiple config headers are not allowed");
    }

    private void doTestIllegalConfig(String input, String error) {
        try {
            // when
            parse(input);
            fail("Should throw exception");
        } catch (ParsingException e) {
            // then
            assertEquals(error, e.getMessage());
        }
    }
}
