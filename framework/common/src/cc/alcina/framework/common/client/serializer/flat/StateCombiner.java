package cc.alcina.framework.common.client.serializer.flat;

import java.util.ArrayList;
import java.util.List;

/*
 * A generalised 'state combiner' - like a Collector in the Streams package.
 */
public abstract class StateCombiner<A, B, C> {
	protected A lastValue;

	protected B intermediateState;

	public List<C> out = new ArrayList<>();

	public void add(A value) {
		if (lastValue == null || !canCombine(value)) {
			flush();
			newIntermediateState();
			lastValue = value;
		}
		addToIntermediate(value);
	}

	public void flush() {
		if (intermediateState != null) {
			C toOut = intermediateToOut();
			if (toOut != null) {
				out.add(toOut);
			}
		}
	}

	protected abstract void addToIntermediate(A value);

	protected abstract boolean canCombine(A value);

	protected abstract C intermediateToOut();

	protected abstract void newIntermediateState();
}