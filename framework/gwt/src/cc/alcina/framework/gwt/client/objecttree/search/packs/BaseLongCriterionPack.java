package cc.alcina.framework.gwt.client.objecttree.search.packs;

import java.util.Arrays;
import java.util.function.Predicate;

import com.totsp.gwittir.client.ui.AbstractBoundWidget;
import com.totsp.gwittir.client.validator.Validator;

import cc.alcina.framework.common.client.domain.DomainFilter;
import cc.alcina.framework.common.client.domain.search.DomainCriterionFilter;
import cc.alcina.framework.common.client.gwittir.validator.LongValidator;
import cc.alcina.framework.common.client.search.LongCriterion;
import cc.alcina.framework.gwt.client.gwittir.widget.TextBox;
import cc.alcina.framework.gwt.client.objecttree.search.FlatSearchable;
import cc.alcina.framework.gwt.client.objecttree.search.StandardSearchOperator;

public class BaseLongCriterionPack {
	public interface BaseLongCriterionHandler<SC extends LongCriterion, T>
			extends DomainCriterionFilter<SC> {
		public boolean test(T t, Long value);

		@Override
		default DomainFilter getFilter(SC sc) {
			Long value = sc.getValue();
			if (value == null) {
				return null;
			}
			return new DomainFilter(new Predicate<T>() {
				@Override
				public boolean test(T o) {
					return BaseLongCriterionHandler.this.test(o, value);
				}
			}).invertIf(sc
					.getOperator() == StandardSearchOperator.DOES_NOT_CONTAIN);
		}
	}

	public static abstract class BaseLongCriterionSearchable
			extends SubLongCriterionSearchable<LongCriterion> {
		public BaseLongCriterionSearchable(String objectName) {
			super(LongCriterion.class, objectName);
		}
	}

	public static abstract class SubLongCriterionSearchable<T extends LongCriterion>
			extends FlatSearchable<T> {
		public SubLongCriterionSearchable(Class<T> clazz, String objectName) {
			super(clazz, "", objectName,
					Arrays.asList(StandardSearchOperator.CONTAINS,
							StandardSearchOperator.DOES_NOT_CONTAIN));
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
}