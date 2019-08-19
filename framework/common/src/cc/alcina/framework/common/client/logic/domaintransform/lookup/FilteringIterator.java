package cc.alcina.framework.common.client.logic.domaintransform.lookup;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Function;

public class FilteringIterator<E> implements Iterator<E> {
    private Iterator<E> source;

    private Function<E, E> mapping;

    private E next;

    private boolean finished;

    public FilteringIterator(Iterator<E> source, Function<E, E> mapping) {
        this.source = source;
        this.mapping = mapping;
    }

    @Override
    public boolean hasNext() {
        if (!finished) {
            peek();
            finished = next != null;
        }
        return !finished;
    }

    @Override
    public E next() {
        if (finished) {
            throw new NoSuchElementException();
        }
        if (next == null) {
            peek();
        }
        if (finished) {
            throw new NoSuchElementException();
        }
        E mapped = next;
        next = null;
        return mapped;
    }

    private void peek() {
        next = null;
        while (source.hasNext()) {
            E sourceNext = source.next();
            E mapped = mapping.apply(sourceNext);
            if (mapped != null) {
                next = mapped;
                break;
            }
        }
        finished = true;
    }
}
