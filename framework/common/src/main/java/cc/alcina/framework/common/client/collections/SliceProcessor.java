package cc.alcina.framework.common.client.collections;

import java.util.List;

public class SliceProcessor<T> {
	public void process(List<T> objects, int sliceSize,
			SliceSubProcessor<T> processor) {
		for (int i = 0; i < objects.size(); i += sliceSize) {
			List<T> slice = objects.subList(i,
					Math.min(i + sliceSize, objects.size()));
			processor.process(slice, i);
		}
	}

	@FunctionalInterface
	public static interface SliceSubProcessor<T> {
		void process(List<T> sublist, int startIndex);
	}
}
