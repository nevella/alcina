package cc.alcina.framework.gwt.client.objecttree.search;

import java.util.List;
import java.util.function.Function;

import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.search.EntityCriterion;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.gwittir.widget.BoundSuggestBox;
import cc.alcina.framework.gwt.client.gwittir.widget.BoundSuggestBox.BoundSuggestOracle;
import cc.alcina.framework.gwt.shadow.com.totsp.gwittir.client.ui.AbstractBoundWidget;

public class FlatSuggestorSearchable<TC extends EntityCriterion>
		extends FlatSearchable<TC> {
	public FlatSuggestorSearchable(Class<TC> clazz, String category,
			String name) {
		this(clazz, category, name, StandardSearchOperator.EQUAL_OR_NOT);
	}

	public FlatSuggestorSearchable(Class<TC> clazz, String category,
			String name, List<StandardSearchOperator> operators) {
		super(clazz, category, name, operators);
	}

	@Override
	public AbstractBoundWidget createEditor() {
		throw new UnsupportedOperationException();
	}

	@Override
	public AbstractBoundWidget createEditor(TC criterion) {
		BoundSuggestBox<Object> boundSuggestBox = new BoundSuggestBox<>();
		TC newInstance = Reflections.newInstance(getCriterionClass());
		boundSuggestBox.suggestOracle(new BoundSuggestOracle()
				.clazz(newInstance.getObjectClass()).hint(getHint()));
		boundSuggestBox
				.setRenderer(new TruncatedObjectHelperRenderer(criterion));
		boundSuggestBox.setShowOnFocus(true);
		return boundSuggestBox;
	}

	@Override
	public String getCriterionPropertyName() {
		return "value";
	}

	protected String getHint() {
		return "";
	}

	@Override
	public SearchOperator getOperator(TC value) {
		return value.getOperator();
	}

	@Override
	public boolean hasValue(TC sc) {
		return sc.getId() != 0;
	}

	class TruncatedObjectHelperRenderer implements Function<Object, String> {
		private Object initialObject;

		private String initialDisplayText;

		public TruncatedObjectHelperRenderer(TC criterion) {
			initialDisplayText = criterion.getDisplayText();
			initialObject = criterion.ensurePlaceholderObject();
		}

		@Override
		public String apply(Object o) {
			if (o == initialObject) {
				return initialDisplayText;
			}
			return CommonUtils.nullSafeToString(o);
		}
	}
}
