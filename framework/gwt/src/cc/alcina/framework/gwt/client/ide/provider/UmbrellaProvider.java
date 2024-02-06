package cc.alcina.framework.gwt.client.ide.provider;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.provider.TextProvider;
import cc.alcina.framework.common.client.util.Multimap;

public interface UmbrellaProvider<T> {
	public void forCollection(Collection<T> collection, Predicate<T> predicate);

	public List<String> getUmbrellaNames(String prefix);

	public List<T> getUmbrellaObjects(String prefix);

	public static class UmbrellaProviderLetterSecondLetter<T>
			implements UmbrellaProvider<T> {
		private final boolean withSecondLetter;

		private Set<String> childPrefixLkp;

		private Multimap<String, List<String>> childPrefixes;

		private Multimap<String, List<T>> childObjects;

		public UmbrellaProviderLetterSecondLetter(boolean withSecondLetter) {
			this.withSecondLetter = withSecondLetter;
		}

		private void ensureLookups(Collection<T> collection,
				Predicate<T> predicate) {
			childObjects = new Multimap<String, List<T>>();
			childPrefixes = new Multimap<String, List<String>>();
			childPrefixLkp = new HashSet<String>();
			if (collection.isEmpty()) {
				return;
			}
			TextProvider textProvider = TextProvider.get();
			for (T t : collection) {
				if (predicate != null && !predicate.test(t)) {
					continue;
				}
				String objectName = textProvider.getObjectName(t).toLowerCase();
				String s1 = objectName.length() > 0 ? objectName.substring(0, 1)
						: "-";
				String s2 = objectName.length() > 1 ? objectName.substring(0, 2)
						: "-";
				if (!childPrefixLkp.contains(s1)) {
					childPrefixes.add("", s1);
					childPrefixLkp.add(s1);
				}
				if (withSecondLetter) {
					if (!childPrefixLkp.contains(s2)) {
						childPrefixes.add(s1, s2);
						childPrefixLkp.add(s2);
					}
					childObjects.add(s2, t);
				} else {
					childObjects.add(s1, t);
				}
			}
		}

		@Override
		public void forCollection(Collection<T> collection,
				Predicate<T> predicate) {
			ensureLookups(collection, predicate);
		}

		@Override
		public List<String> getUmbrellaNames(String prefix) {
			return childPrefixes.getAndEnsure(prefix);
		}

		@Override
		public List<T> getUmbrellaObjects(String prefix) {
			return childObjects.getAndEnsure(prefix);
		}
	}

	public static class UmbrellaProviderPassthrough<T>
			implements UmbrellaProvider<T> {
		private List<T> objects;

		@Override
		public void forCollection(Collection<T> collection,
				Predicate<T> predicate) {
			objects = collection.stream().filter(predicate)
					.collect(Collectors.toList());
		}

		@Override
		public List<String> getUmbrellaNames(String prefix) {
			return Collections.emptyList();
		}

		@Override
		public List<T> getUmbrellaObjects(String prefix) {
			return objects;
		}
	}
}
