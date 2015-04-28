package java.util.stream;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

public class Collectors {
	private static class ToListCollector<T> implements
			java.util.stream.Collector<T, T, List<T>> {
		public List<T> collect(Stream<T> stream) {
			List<T> result = new ArrayList<T>();
			for (Iterator<T> itr = stream.iterator(); itr.hasNext();) {
				result.add(itr.next());
			}
			return result;
		}
	}

	public static <T> Collector<T, ?, List<T>> toList() {
		return new ToListCollector<T>();
	}
}
