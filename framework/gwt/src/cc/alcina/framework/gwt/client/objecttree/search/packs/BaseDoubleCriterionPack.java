package cc.alcina.framework.gwt.client.objecttree.search.packs;

import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Stream;

import com.totsp.gwittir.client.ui.AbstractBoundWidget;
import com.totsp.gwittir.client.validator.Validator;

import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.domain.DomainFilter;
import cc.alcina.framework.common.client.gwittir.validator.DoubleValidator;
import cc.alcina.framework.common.client.search.DoubleCriterion;
import cc.alcina.framework.gwt.client.gwittir.widget.TextBox;
import cc.alcina.framework.gwt.client.objecttree.search.FlatSearchable;
import cc.alcina.framework.gwt.client.objecttree.search.StandardSearchOperator;

public class BaseDoubleCriterionPack {
	public interface BaseDoubleCriterionHandler<T> extends Function<T, Double> {
		@Override
		public Double apply(T t);

		default DomainFilter getFilter0(DoubleCriterion sc) {
			Double value = sc.getDouble();
			if (value == null) {
				return null;
			}
			double p_value = value;
			return new DomainFilter(new CollectionFilter<T>() {
				@Override
				public boolean allow(T o) {
					Double d = apply(o);
					if (d == null) {
						return false;
					}
					double p_d = d;
					switch (sc.getOperator()) {
					case GREATER_THAN:
						return p_d > p_value;
					case LESS_THAN:
						return p_d < p_value;
					case EQUALS:
						return p_d == p_value;
					default:
						return false;
					}
				}
			});
		}
	}

	public static abstract class BaseDoubleCriterionSearchable<T extends DoubleCriterion>
			extends FlatSearchable<T> {
		public BaseDoubleCriterionSearchable(Class<T> clazz, String objectName,
				String fieldName) {
			super(clazz, objectName, fieldName,
					Arrays.asList(StandardSearchOperator.EQUALS,
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
			return new DoubleValidator();
		}

		@Override
		public boolean hasValue(DoubleCriterion sc) {
			return sc.getDouble() != null;
		}
	}

	public interface BaseDoubleStreamCriterionHandler<T>
			extends Function<T, Stream<Double>> {
		@Override
		public Stream<Double> apply(T t);

		default DomainFilter getFilter0(DoubleCriterion sc) {
			Double value = sc.getDouble();
			if (value == null) {
				return null;
			}
			double p_value = value;
			return new DomainFilter(new CollectionFilter<T>() {
				@Override
				public boolean allow(T o) {
					Stream<Double> stream = apply(o);
					if (stream == null) {
						return false;
					}
					return stream.anyMatch(d -> {
						double p_d = d;
						switch (sc.getOperator()) {
						case GREATER_THAN:
							return p_d > p_value;
						case LESS_THAN:
							return p_d < p_value;
						case EQUALS:
							return p_d == p_value;
						default:
							return false;
						}
					});
				}
			});
		}
	}
}