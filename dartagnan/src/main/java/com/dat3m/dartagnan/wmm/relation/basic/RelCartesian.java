package com.dat3m.dartagnan.wmm.relation.basic;

import com.dat3m.dartagnan.program.event.Event;
import com.dat3m.dartagnan.program.utils.EType;
import com.dat3m.dartagnan.wmm.filter.FilterAbstract;
import com.dat3m.dartagnan.wmm.utils.Tuple;
import com.dat3m.dartagnan.wmm.utils.TupleSet;

import java.util.Collection;

public class RelCartesian extends BasicRelation {
    private FilterAbstract filter1;
    private FilterAbstract filter2;

    public static String makeTerm(FilterAbstract filter1, FilterAbstract filter2){
        return "(" + filter1 + "*" + filter2 + ")";
    }

    public RelCartesian(FilterAbstract filter1, FilterAbstract filter2) {
        this.filter1 = filter1;
        this.filter2 = filter2;
        this.term = makeTerm(filter1, filter2);
        isStatic = true;
    }

    public RelCartesian(FilterAbstract filter1, FilterAbstract filter2, String name) {
        super(name);
        this.filter1 = filter1;
        this.filter2 = filter2;
        this.term = makeTerm(filter1, filter2);
        isStatic = true;
    }

    @Override
    public TupleSet getMaxTupleSet(){
        if(maxTupleSet == null){
            maxTupleSet = new TupleSet();
            Collection<Event> events = program.getEventRepository().getEvents(EType.ANY);
            for(Event e1 : events){
                if(filter1.filter(e1)){
                    for(Event e2 : events){
                        if(filter2.filter(e2)){
                            maxTupleSet.add(new Tuple(e1, e2));
                        }
                    }
                }
            }
        }
        return maxTupleSet;
    }
}
