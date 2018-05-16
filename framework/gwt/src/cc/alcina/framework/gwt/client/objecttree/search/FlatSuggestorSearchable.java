package cc.alcina.framework.gwt.client.objecttree.search;

import java.util.List;

import com.totsp.gwittir.client.ui.AbstractBoundWidget;
import com.totsp.gwittir.client.ui.Renderer;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.search.TruncatedObjectCriterion;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.gwittir.widget.BoundSuggestBox;
import cc.alcina.framework.gwt.client.gwittir.widget.BoundSuggestBox.BoundSuggestOracle;

public class FlatSuggestorSearchable<TC extends TruncatedObjectCriterion>
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
		TC newInstance = Reflections.classLookup()
				.newInstance(getCriterionClass());
		boundSuggestBox.suggestOracle(
				new BoundSuggestOracle().clazz(newInstance.getObjectClass()).hint(getHint()));
		boundSuggestBox
				.setRenderer(new TruncatedObjectHelperRenderer(criterion));
		return boundSuggestBox;
	}

	protected String getHint() {
		return "";
	}

	class TruncatedObjectHelperRenderer implements Renderer<Object, String> {
		private Object initialObject;

		private String initialDisplayText;

		public TruncatedObjectHelperRenderer(TC criterion) {
			initialDisplayText = criterion.getDisplayText();
			initialObject = criterion.ensurePlaceholderObject();
		}

		@Override
		public String render(Object o) {
			if (o == initialObject) {
				return initialDisplayText;
			}
			return CommonUtils.nullSafeToString(o);
		}
	}

	@Override
	public String getCriterionPropertyName() {
		return "value";
	}

	@Override
	public SearchOperator getOperator(TC value) {
		return value.getOperator();
	}

	@Override
	public boolean hasValue(TC sc) {
		return sc.getId() != 0;
	}
}
