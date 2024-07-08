package cc.alcina.framework.common.client.util;

import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.reflection.Reflections;

public interface HasFilterableText {
	public interface Has {
		HasFilterableText provideHasFilterableText();
	}

	public Stream<String> toFilterableStrings();

	static HasFilterableText to(Object o) {
		if (o instanceof HasFilterableText) {
			return (HasFilterableText) o;
		}
		if (o instanceof HasFilterableText.Has) {
			return ((HasFilterableText.Has) o).provideHasFilterableText();
		}
		return Registry.impl(HasFilterableTextAdapter.class, o.getClass())
				.withObject(o);
	}

	/*
	 * TODO - override for entities (definitely don't want to traverse the
	 * domain here)
	 */
	@Registration(HasFilterableTextAdapter.class)
	public static class HasFilterableTextAdapter implements HasFilterableText {
		Object o;

		public HasFilterableTextAdapter withObject(Object o) {
			this.o = o;
			return this;
		}

		@Override
		public Stream<String> toFilterableStrings() {
			return Reflections.at(o).properties().stream()
					.map(p -> String.valueOf(p.get(o)));
		}
	}

	/*
	 * Note that this can also be used as a general "find in multiple strings"
	 * matcher
	 */
	public static class Query<T> implements Predicate<T> {
		public static Query<?> of(String query) {
			return new Query<>(query);
		}

		String query;

		boolean regex;

		String lastString;

		public Query(String query) {
			this.query = Ax.blankToEmpty(query);
			this.resolvedQuery = query;
		}

		public Query withRegex(boolean regex) {
			this.regex = regex;
			return this;
		}

		boolean caseInsensitive;

		public Query withCaseInsensitive(boolean caseInsensitive) {
			this.caseInsensitive = caseInsensitive;
			this.resolvedQuery = query.toLowerCase();
			return this;
		}

		RegExp regExp;

		int fromIndex = 0;

		String resolvedQuery;

		public boolean test(Object o) {
			HasFilterableText filterable = HasFilterableText.to(o);
			return filterable.toFilterableStrings().filter(Objects::nonNull)
					.anyMatch(s -> next(s) != null);
		}

		public IntPair next(String s) {
			// string identity check for once ok
			if (s != lastString) {
				fromIndex = 0;
				regExp = null;
			}
			lastString = s;
			if (regex && regExp == null) {
				regExp = RegExp.compile(query);
			}
			if (regex) {
				MatchResult matchResult = regExp.exec(s);
				if (matchResult != null) {
					return IntPair.of(matchResult.getIndex(),
							matchResult.getIndex()
									+ matchResult.getGroup(0).length());
				}
			} else {
				int idx = s.indexOf(resolvedQuery, fromIndex);
				if (idx != -1) {
					IntPair result = IntPair.of(idx,
							idx + resolvedQuery.length());
					fromIndex = result.i2;
					return result;
				}
			}
			return null;
		}
	}
}
