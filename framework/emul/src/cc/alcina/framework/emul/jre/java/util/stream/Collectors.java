package java.util.stream;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.stream.Collector;
import java.util.stream.Collectors.CollectorImpl;

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

	private static class JoiningCollector<T> implements
			java.util.stream.Collector<T, T, String> {
		private CharSequence separator;

		public JoiningCollector(CharSequence separator) {
			this.separator = separator;
		}

		public String collect(Stream<T> stream) {
			StringBuilder sb = new StringBuilder();
			boolean first = true;
			for (Iterator<T> itr = stream.iterator(); itr.hasNext();) {
				if (first) {
					first = false;
				} else {
					sb.append(separator);
				}
				sb.append(itr.next().toString());
			}
			return sb.toString();
		}
	}

	public static <T> Collector<T, ?, List<T>> toList() {
		return new ToListCollector<T>();
	}

	public static <T> Collector<T, T, String> joining() {
		return new JoiningCollector<T>("");
	}

	public static <T> Collector<T, T, String> joining(CharSequence separator) {
		return new JoiningCollector<T>(separator);
	}
}
