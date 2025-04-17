package com.dat3m.dartagnan.program;

import com.dat3m.dartagnan.exception.ParsingException;

import java.util.List;
import java.util.stream.IntStream;

public class ThreadGridHelper {

    public ThreadGridHelper() {
    }

    public static int getSize(ScopeNames scopeNames, String scopeName, List<Integer> scopeSizes) {
        return getSize(scopeNames.getScopes().indexOf(scopeName), scopeSizes);
    }

    public static int getSize(int index, List<Integer> scopeSizes) {
        if (index < 0 || index >= scopeSizes.size()) {
            throw new ParsingException("Thread grid not supported for index: " + index);
        }
        return scopeSizes.subList(0, index).stream().reduce(1, (a, b) -> a * b);
    }

    public static int getIndex(ScopeNames scopeNames, String scopeName, List<Integer> scopeSizes) {
        return getIndex(scopeNames.getScopes().indexOf(scopeName), scopeSizes);
    }

    public static int getIndex(int index, List<Integer> scopeSizes) {
        if (index < 0 || index >= scopeSizes.size()) {
            throw new ParsingException("Thread grid not supported for index: " + index);
        }
        return index == scopeSizes.size() - 1
                ? index / getSize(index, scopeSizes)
                : (index % scopeSizes.get(index + 1)) / getSize(index, scopeSizes);
    }

    public static List<Integer> getIndexes(List<Integer> scopeSizes) {
        return IntStream.range(0, scopeSizes.size())
                .mapToObj(i -> getIndex(i, scopeSizes))
                .toList();
    }

    public static List<Integer> getSizes(List<Integer> scopeSizes) {
        return IntStream.range(0, scopeSizes.size())
                .mapToObj(i -> getSize(i, scopeSizes))
                .toList();
    }
}
