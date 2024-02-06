package cc.alcina.framework.gwt.client.objecttree.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.totsp.gwittir.client.ui.AbstractBoundWidget;
import com.totsp.gwittir.client.validator.Validator;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.search.SearchCriterion;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.common.client.search.TruncatedObjectCriterion;
import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.common.client.util.Ax;

@Reflected
public abstract class FlatSearchable<SC extends SearchCriterion>
		implements Comparable<FlatSearchable>, HasValueChangeHandlers,
		Registration.Ensure {
	private static transient Comparator<FlatSearchable> comparator;
	static {
		comparator = Comparator.comparing(FlatSearchable::getCategory);
		comparator = comparator.thenComparing(FlatSearchable::getName);
	}

	private HandlerManager handlerManager;

	public transient SearchDefinition def;

	private Class<SC> clazz;

	protected String category;

	protected String name;

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
	public HandlerRegistration
			addValueChangeHandler(ValueChangeHandler handler) {
		return ensureHandlers().addHandler(ValueChangeEvent.getType(), handler);
	}

	@Override
	public int compareTo(FlatSearchable o) {
		return comparator.compare(this, o);
	}

	public SC createCriterionInstance() {
		return Reflections.newInstance(clazz);
	}

	public abstract AbstractBoundWidget createEditor();

	public AbstractBoundWidget createEditor(SC criterion) {
		return createEditor();
	}

	HandlerManager ensureHandlers() {
		return handlerManager == null
				? handlerManager = new HandlerManager(this)
				: handlerManager;
	}

	@Override
	public void fireEvent(GwtEvent<?> event) {
		if (handlerManager != null) {
			handlerManager.fireEvent(event);
		}
	}

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
		Object value = Reflections.at(sc).property(getCriterionPropertyName())
				.get(sc);
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
		return Ax.isBlank(category) ? name
				: Ax.format("%s : %s", category, name);
	}

	public FlatSearchable withDef(SearchDefinition def) {
		this.def = def;
		return this;
	}

	@Registration.NonGenericSubtypes(HasSearchables.class)
	public static abstract class HasSearchables<B extends Bindable> {
		private Map<Class<? extends SearchCriterion>, FlatSearchable> searchables;

		protected abstract List<FlatSearchable> createSearchables();

		public String criterionDisplayName(SearchCriterion criterion) {
			if (criterion instanceof TruncatedObjectCriterion) {
				return ((TruncatedObjectCriterion) criterion)
						.provideTypeDisplayName();
			}
			return searchableForCriterion(criterion)
					.map(FlatSearchable::toString)
					.orElse(criterion.getClass().getSimpleName());
		}

		public String criterionValue(SearchCriterion criterion) {
			return criterion.provideValueAsRenderableText();
		}

		private void ensureSearchables() {
			if (searchables == null) {
				searchables = createSearchables().stream()
						.collect(AlcinaCollectors
								.toKeyMap(FlatSearchable::getCriterionClass));
			}
		}

		public List<FlatSearchable> getSearchables() {
			ensureSearchables();
			return this.searchables.values().stream()
					.collect(Collectors.toList());
		}

		private Optional<FlatSearchable>
				searchableForCriterion(SearchCriterion criterion) {
			ensureSearchables();
			return Optional.ofNullable(searchables.get(criterion.getClass()));
		}

		public static class Bindables extends HasSearchables<Bindable> {
			@Override
			protected List<FlatSearchable> createSearchables() {
				return new ArrayList<>();
			}
		}
	}
}
