package cc.alcina.framework.gwt.client.objecttree.search.packs;

import java.util.Arrays;
import java.util.List;

import com.totsp.gwittir.client.ui.AbstractBoundWidget;

import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.domain.DomainFilter;
import cc.alcina.framework.common.client.search.TxtCriterion;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.gwittir.widget.TextBox;
import cc.alcina.framework.gwt.client.objecttree.search.FlatSearchable;
import cc.alcina.framework.gwt.client.objecttree.search.StandardSearchOperator;
import cc.alcina.framework.gwt.client.util.TextUtils;

public class BaseTextCriterionPack {
	public interface BaseTextCriterionHandler<T> {
		public boolean test(T t, String text);

		default DomainFilter getFilter0(TxtCriterion sc) {
			String text = TextUtils.normalisedLcKey(sc.getText());
			if (text.isEmpty()) {
				return null;
			}
			return new DomainFilter(new CollectionFilter<T>() {
				@Override
				public boolean allow(T o) {
					return BaseTextCriterionHandler.this.test(o, text);
				}
			}).invertIf(sc
					.getOperator() == StandardSearchOperator.DOES_NOT_CONTAIN);
		}
	}

	public static abstract class BaseTextCriterionSearchable
			extends SubTextCriterionSearchable<TxtCriterion> {
		public BaseTextCriterionSearchable(String objectName) {
			super(TxtCriterion.class, objectName);
		}

		public BaseTextCriterionSearchable(String objectName,
				String fieldName) {
			super(TxtCriterion.class, objectName, fieldName);
		}

		public BaseTextCriterionSearchable(String category, String name,
				List<StandardSearchOperator> operators) {
			super(TxtCriterion.class, category, name, operators);
		}
	}

	public static abstract class SubTextCriterionSearchable<T extends TxtCriterion>
			extends FlatSearchable<T> {
		public SubTextCriterionSearchable(Class<T> clazz, String objectName) {
			this(clazz, objectName, "Text");
		}

		public SubTextCriterionSearchable(Class<T> clazz, String objectName,
				String name) {
			super(clazz, objectName, name,
					Arrays.asList(StandardSearchOperator.CONTAINS,
							StandardSearchOperator.DOES_NOT_CONTAIN));
		}

		public SubTextCriterionSearchable(Class<T> clazz, String category,
				String name, List<StandardSearchOperator> operators) {
			super(clazz, category, name, operators);
		}

		@Override
		public AbstractBoundWidget createEditor() {
			return new TextBox();
		}

		@Override
		public String getCriterionPropertyName() {
			return "text";
		}

		@Override
		public boolean hasValue(TxtCriterion sc) {
			return CommonUtils.isNotNullOrEmpty(sc.getText());
		}
	}
}