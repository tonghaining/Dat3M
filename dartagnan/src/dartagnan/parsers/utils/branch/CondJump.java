package dartagnan.parsers.utils.branch;

import dartagnan.expression.Atom;
import dartagnan.program.Thread;
import dartagnan.program.event.Event;
import dartagnan.program.event.If;

import java.util.Map;
import static java.util.Map.entry;

public class CondJump extends Event {

    private static final Map<String, String> condMap = Map.ofEntries(
            entry("beq", "=="),
            entry("bne", "!="),
            entry("bgt", ">"),
            entry("blt", "<"),
            entry("bge", ">="),
            entry("ble", "<=")
    );

    private final String op;
    private Label label;
    private Cmp cmp;

    public CondJump(String op, Label label){
        this.op = op.toLowerCase();
        if(!condMap.containsKey(this.op)){
            throw new RuntimeException("Unrecognised conditional jump " + op);
        }
        this.label = label;
    }

    public void setCmp(Cmp cmp){
        this.cmp = cmp;
    }

    public Label getLabel(){
        return label;
    }

    public If toIf(Thread t1, Thread t2){
        return new If(new Atom(cmp.getLeft(), condMap.get(op), cmp.getRight()), t1, t2);
    }

    @Override
    public String toString(){
        return op + " " + label;
    }

    @Override
    public CondJump clone(){
        // Instances of Label should not duplicated during cloning (multiple CondJump can refer to the same Label).
        // Similar to conditional RMW cloning
        throw new RuntimeException("Method clone is not implemented for " + getClass().getName());
    }
}
