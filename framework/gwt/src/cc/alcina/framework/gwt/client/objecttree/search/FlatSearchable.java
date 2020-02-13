package cc.alcina.framework.gwt.client.objecttree.search;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.totsp.gwittir.client.ui.AbstractBoundWidget;
import com.totsp.gwittir.client.validator.Validator;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.search.SearchCriterion;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.common.client.util.Ax;

public abstract class FlatSearchable<SC extends SearchCriterion>
		implements Comparable<FlatSearchable> {
	private static transient Comparator<FlatSearchable> comparator;
	static {
		comparator = Comparator.comparing(FlatSearchable::getCategory);
		comparator = comparator.thenComparing(FlatSearchable::getName);
	}

	private Class<SC> clazz;

	private String category;

	protected String name;

	@SuppressWarnings("unused")
	private SearchDefinition def;

	private SC criterion;

	private List<? extends SearchOperator> operators;

	public FlatSearchable(Class<SC> clazz, String category, String name,
			List<StandardSearchOperator> operators) {
		this.clazz = clazz;
		this.category = category;
		this.name = name;
		this.operators = operators;
	}

	@Override
	public int compareTo(FlatSearchable o) {
		return comparator.compare(this, o);
	}

	public SC createCriterionInstance() {
		return Reflections.classLookup().newInstance(clazz);
	}

	public AbstractBoundWidget createEditor(SC criterion) {
		return createEditor();
	}

	public abstract AbstractBoundWidget createEditor();

	public String getCategory() {
		return this.category;
	}

	public SC getCriterion() {
		return this.criterion;
	}

	public Class<SC> getCriterionClass() {
		return this.clazz;
	}

	public abstract String getCriterionPropertyName();

	public String getName() {
		return this.name;
	}

	public SearchOperator getOperator(SC value) {
		return listOperators().get(0);
	}

	public Optional<String> getOperatorPropertyName() {
		return Optional.of("operator");
	}

	public Validator getValidator() {
		return null;
	}

	public abstract boolean hasValue(SC sc);

	public boolean isNonDefaultValue(SC sc) {
		Object value = Reflections.propertyAccessor().getPropertyValue(sc,
				getCriterionPropertyName());
		if (value instanceof Collection) {
			return ((Collection) value).size() > 0;
		} else {
			return value != null;
		}
	}

	public List<? extends SearchOperator> listOperators() {
		return operators;
	}

	public void setCriterion(SC criterion) {
		this.criterion = criterion;
	}

	public void setDef(SearchDefinition def) {
		this.def = def;
	}

	@Override
	public String toString() {
		return Ax.format("%s : %s", category, name);
	}
}
