package com.dat3m.dartagnan.analysis.graphRefinement.graphs.eventGraph.utils;

import com.dat3m.dartagnan.analysis.graphRefinement.graphs.eventGraph.EventGraph;
import com.dat3m.dartagnan.verification.model.Edge;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

// Encapsulates a Graph into a read-only Set
public class GraphSetView implements Set<Edge> {

    private final EventGraph graph;

    public GraphSetView(EventGraph graph) {
        this.graph = graph;
    }

    @Override
    public int size() {
        return graph.size();
    }

    @Override
    public boolean isEmpty() {
        return graph.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        if (!(o instanceof Edge)) {
            return false;
        }
        return graph.contains((Edge)o);
    }

    @Override
    public Iterator<Edge> iterator() {
        return graph.edgeIterator();
    }

    @Override
    public Object[] toArray() {
        ArrayList<Edge> edgeList = new ArrayList<>(this.size());
        iterator().forEachRemaining(edgeList::add);
        return edgeList.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        ArrayList<Edge> edgeList = new ArrayList<>(this.size());
        iterator().forEachRemaining(edgeList::add);
        return edgeList.toArray(a);
    }

    @Override
    public boolean add(Edge edge) {
        return false;
    }

    @Override
    public boolean remove(Object o) {
        return false;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        for (Object e : c) {
            if (!contains(e))
                return false;
        }
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends Edge> c) {
        return false;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return false;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return false;
    }

    @Override
    public void clear() {

    }
}
