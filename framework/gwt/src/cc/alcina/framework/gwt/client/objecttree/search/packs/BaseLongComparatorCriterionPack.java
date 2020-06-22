package cc.alcina.framework.gwt.client.objecttree.search.packs;

import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Stream;

import com.totsp.gwittir.client.ui.AbstractBoundWidget;
import com.totsp.gwittir.client.validator.Validator;

import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.domain.DomainFilter;
import cc.alcina.framework.common.client.gwittir.validator.LongValidator;
import cc.alcina.framework.common.client.search.DoubleCriterion;
import cc.alcina.framework.common.client.search.LongCriterion;
import cc.alcina.framework.gwt.client.gwittir.widget.TextBox;
import cc.alcina.framework.gwt.client.objecttree.search.FlatSearchable;
import cc.alcina.framework.gwt.client.objecttree.search.StandardSearchOperator;

public class BaseLongComparatorCriterionPack {
	public interface BaseLongComparatorCriterionHandler<T> extends Function<T, Long> {
		@Override
		public Long apply(T t);

		default DomainFilter getFilter0(LongCriterion sc) {
			Long value = sc.getLong();
			if (value == null) {
				return null;
			}
			long p_value = value;
			return new DomainFilter(new CollectionFilter<T>() {
				@Override
				public boolean allow(T o) {
					Long d = apply(o);
					if (d == null) {
						return false;
					}
					long p_d = d;
					switch (sc.getOperator()) {
					case GREATER_THAN:
						return p_d > p_value;
					case LESS_THAN:
						return p_d < p_value;
					case EQUALS:
						return p_d == p_value;
					case DOES_NOT_EQUAL:
						return p_d != p_value;
					default:
						return false;
					}
				}
			});
		}
	}

	public static abstract class BaseLongComparatorCriterionSearchable<T extends LongCriterion>
			extends FlatSearchable<T> {
		public BaseLongComparatorCriterionSearchable(Class<T> clazz, String objectName,
				String fieldName) {
			super(clazz, objectName, fieldName,
					Arrays.asList(StandardSearchOperator.EQUALS,
							StandardSearchOperator.DOES_NOT_EQUAL,
							StandardSearchOperator.GREATER_THAN,
							StandardSearchOperator.LESS_THAN));
		}

		@Override
		public AbstractBoundWidget createEditor() {
			return new TextBox();
		}

		@Override
		public String getCriterionPropertyName() {
			return "value";
		}

		@Override
		public Validator getValidator() {
			return new LongValidator();
		}

		@Override
		public boolean hasValue(LongCriterion sc) {
			return sc.getLong() != null;
		}
	}

	public interface BaseLongStreamCriterionHandler<T>
			extends Function<T, Stream<Long>> {
		@Override
		public Stream<Long> apply(T t);

		default DomainFilter getFilter0(LongCriterion sc) {
			Long value = sc.getLong();
			if (value == null) {
				return null;
			}
			long p_value = value;
			return new DomainFilter(new CollectionFilter<T>() {
				@Override
				public boolean allow(T o) {
					Stream<Long> stream = apply(o);
					if (stream == null) {
						return false;
					}
					return stream.anyMatch(d -> {
						long p_d = d;
						switch (sc.getOperator()) {
						case GREATER_THAN:
							return p_d > p_value;
						case LESS_THAN:
							return p_d < p_value;
						case EQUALS:
							return p_d == p_value;
						case DOES_NOT_EQUAL:
							return p_d != p_value;
						default:
							return false;
						}
					});
				}
			});
		}
	}
}