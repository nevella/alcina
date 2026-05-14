package cc.alcina.framework.gwt.client.dirndl.cmp.sequence;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.gwt.dom.client.StyleElement;
import com.google.gwt.dom.client.StyleInjector;
import com.google.gwt.dom.client.Text;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.domain.search.BindableSearchFilter;
import cc.alcina.framework.common.client.domain.search.criterion.PropertyCriterion;
import cc.alcina.framework.common.client.domain.search.criterion.PropertyOrderCriterion;
import cc.alcina.framework.common.client.logic.reflection.InstanceProperty;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.search.SearchCriterion;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer;
import cc.alcina.framework.common.client.service.InstanceOracle;
import cc.alcina.framework.common.client.service.InstanceQuery;
import cc.alcina.framework.common.client.util.AlcinaCollections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.ClassUtil;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.HasFilterableText;
import cc.alcina.framework.common.client.util.HasFilterableText.Query;
import cc.alcina.framework.common.client.util.IntPair;
import cc.alcina.framework.common.client.util.Timer;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.HighlightModel.Match;
import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.SequenceEvents.FilterElements;
import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.SequenceEvents.HighlightElements;
import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.SequenceEvents.NextSelectable;
import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.SequenceEvents.PreviousSelectable;
import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.SequenceEvents.SetSettingMaxElementRows;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.Bind;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.NodeContext;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.layout.ContextService;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelTransform;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.StreamBinding.InstanceDistinctLambda;
import cc.alcina.framework.gwt.client.dirndl.model.TableColumnsMetadata;
import cc.alcina.framework.gwt.client.dirndl.model.TableEvents;
import cc.alcina.framework.gwt.client.dirndl.model.TableEvents.SortTable;
import cc.alcina.framework.gwt.client.dirndl.model.TableModel;
import cc.alcina.framework.gwt.client.dirndl.model.TableModel.TableColumn;

/*
 * wip - ds.late - add orderservice.provider interface
 */
@TypedProperties
@Directed.Delegating
@ReflectiveSerializer.Checks(ignore = true)
public class SequenceArea extends Model.Fields
		implements SequenceEvents.FilterElements.Handler,
		SequenceEvents.HighlightElements.Handler,
		SequenceEvents.NextSelectable.Handler,
		SequenceEvents.PreviousSelectable.Handler,
		SequenceEvents.SetSettingMaxElementRows.Handler,
		SequenceEvents.HighlightModelChanged.Emitter,
		SequenceEvents.SelectedIndexChanged.Emitter,
		HasFilteredSequenceElements, TableEvents.ColumnsBound.Binding,
		TableColumnsMetadata.Change.Emitter {
	/**
	 * The service required by the SequenceArea (provided by the host, generally
	 * the directed paren t)
	 */
	public interface Service extends ContextService {
		public interface ServiceProvider extends ContextService.Provider {
		}

		/**
		 * Return the definition editor/viewer, rendered as a header
		 */
		Model getSequenceDefinitionHeader();

		InstanceProperty<?, SequencePlace> getPlaceProperty();

		InstanceQuery getInstanceQuery();

		SequenceSettings getSettings();

		long getElementLimit();
	}

	class IndexPredicate implements Predicate {
		int index = 0;

		IntPair selectedRange;

		IndexPredicate(IntPair selectedRange) {
			this.selectedRange = selectedRange;
		}

		@Override
		public boolean test(Object o) {
			if (selectedRange == null) {
				return true;
			}
			return selectedRange.contains(index++);
		}
	}

	/*
	 * backing for shared order/filter functionality
	 */
	class ColumnService
			implements TableEvents.ColumnsBound.Handler, TableColumnsMetadata {
		ColumnService() {
			on(TableEvents.ColumnsBound.class).accept(this::onColumnsBound);
		}

		List<TableColumn> columns;

		@Override
		public void onColumnsBound(TableEvents.ColumnsBound event) {
			TableEvents.ColumnsBound.Data data = event.getModel();
			open = null;
			if (data.bound) {
				this.columns = data.columns;
				onMetadataChange(event);
			}
		}

		void onMetadataChange(ModelEvent event) {
			event.reemitAs(SequenceArea.this, TableColumnsMetadata.Change.class,
					this);
		}

		TableColumn open;

		@Override
		public ColumnMetadata getColumnMetadata(Property property) {
			TableColumn column = columns.stream().filter(
					c -> Objects.equals(c.getField().getProperty(), property))
					.findFirst().get();
			ColumnMetadata metadata = new ColumnMetadata();
			metadata.filterOpen = column == open;
			boolean propertyTypeIsComparable = Reflections.isAssignableFrom(
					Comparable.class,
					ClassUtil.getWrapperType(property.getType()));
			metadata.sortVisible = propertyTypeIsComparable;
			/*
			 * a decent approximation
			 */
			metadata.filterVisible = propertyTypeIsComparable;
			SequenceSearchDefinition def = service.getPlaceProperty()
					.get().search;
			if (def != null) {
				def.allCriteria(PropertyCriterion.class).stream()
						.filter(pc -> pc.isProperty(property)).findFirst()
						.ifPresent(criterion -> {
							metadata.filtered = true;
						});
				def.allCriteria(PropertyOrderCriterion.class).stream()
						.filter(pc -> pc.isProperty(property)).findFirst()
						.ifPresent(criterion -> {
							metadata.sortDirection = TableModel.SortDirection
									.valueOf(criterion.value.direction.name());
						});
			}
			return metadata;
		}
	}

	class OrderServiceImpl implements TableModel.OrderService {
		@Override
		public void onSortTable(SortTable event) {
			TableColumn tableColumn = event.getModel();
			Property property = tableColumn.getField().getProperty();
			if (property.name().equals("index")) {
				/*
				 * not from the transformed bindable
				 */
				// return;
			}
			SequencePlace place = getPlace().copy();
			SequenceSearchDefinition search = place.search;
			if (search == null || !search
					.permitsCriterionType(PropertyOrderCriterion.class)) {
				return;
			}
			PropertyOrderCriterion criterion = search
					.firstCriterion(PropertyOrderCriterion.class);
			if (criterion == null) {
				criterion = new PropertyOrderCriterion();
				search.addCriterionToSoleCriteriaGroup(criterion);
			}
			if (Objects.equals(criterion.value.propertyName, property.name())) {
				criterion.value.direction = criterion.value.direction.reverse();
			} else {
				criterion.value.propertyName = property.name();
			}
			place.updateInstanceQueryDef(search);
			event.reemitAs(SequenceArea.this,
					SequenceEvents.SequencePlaceChanged.class, place);
			event.setHandled(true);
		}

		@Override
		public Class<? extends Bindable> renderedBindableClass() {
			return sequence.getRowType();
		}
	}

	@Directed(className = "definition-header")
	Model definitionHeader;

	@Directed
	SequenceTable sequenceTable;

	@Directed
	DetailArea detailArea;

	Sequence<?> sequence;

	List<?> filteredSequenceElements;

	HighlightModel highlightModel;

	StyleElement styleElement;

	SequencePlace lastSequenceTestPlace = null;

	SequencePlace lastHighlightTestPlace = null;

	SequencePlace lastSelectedIndexChangePlace = null;

	Timer observableObservedTimer;

	Service service;

	InstanceOracle.Query<?> oracleQuery;

	public Runnable reloadSequenceLambda = InstanceDistinctLambda.of(this,
			this::reloadSequence);

	Runnable updateStylesLambda = this::updateStyles;

	public int preFilterCount;

	TransformedCache transformedCache;

	ColumnService columnService = new ColumnService();

	public SequenceArea() {
	}

	public PackageProperties._SequenceArea.InstanceProperties properties() {
		return PackageProperties.sequenceArea.instance(this);
	}

	@Override
	public void onNodeContext(NodeContext event) {
		service = service(Service.class);
		definitionHeader = service.getSequenceDefinitionHeader();
		from(service.getPlaceProperty())
				// todo - add ignoreable change filter
				.filter(this::filterUnchangedSequencePlaceChange)
				.dispatchDistinct(reloadSequenceLambda);
		from(properties().filteredSequenceElements())
				.signal(this::computeHighlightModel);
		from(service.getPlaceProperty())
				// todo - add ignoreable change filter
				.filter(this::filterUnchangedHighlightPlaceChange)
				.signal(this::computeHighlightModel);
		from(properties().filteredSequenceElements()).value(this).debug()
				.map(SequenceTable::new).to(properties().sequenceTable())
				.oneWay();
		from(properties().filteredSequenceElements()).value(this)
				.map(DetailArea::new).to(properties().detailArea()).oneWay();
		from(service.getPlaceProperty()).filter(this::filterSelectedIndexChange)
				.signal(this::onSelectedIndexChange);
		from(properties().sequence()).dispatchDistinct(updateStylesLambda);
		bindings().from(service.getSettings())
				.dispatchDistinct(updateStylesLambda);
		bindings().from(service.getSettings().properties().columnSet())
				.dispatchDistinct(reloadSequenceLambda);
		columnService = new ColumnService();
		event.registerService(TableModel.OrderService.class,
				new OrderServiceImpl());
		exec(reloadSequenceLambda).distinct().dispatch();
		/*
		 * Initialise with an empty sequence, it's easier than adding null
		 * checks throughout
		 */
		putSequence(Sequence.Blank.createInstance());
	}

	@Override
	public void onFilterElements(FilterElements event) {
		getPlace().copy().withHighlight(null).withFilter(event.getModel()).go();
	}

	@Override
	public void onHighlightElements(HighlightElements event) {
		getPlace().copy().withHighlight(event.getModel()).go();
	}

	@Override
	public void onPreviousSelectable(PreviousSelectable event) {
		if (highlightModel.hasMatches()) {
			highlightModel.moveIndex(getSelectedSequenceElement(), -1);
			goToHighlightModelIndex();
		} else {
			getPlace().copy()
					.deltaSelectedRow(-1, filteredSequenceElements.size()).go();
		}
	}

	@Override
	public void onNextSelectable(NextSelectable event) {
		if (highlightModel.hasMatches()) {
			highlightModel.moveIndex(getSelectedSequenceElement(), +1);
			goToHighlightModelIndex();
		} else {
			getPlace().copy()
					.deltaSelectedRow(1, filteredSequenceElements.size()).go();
		}
	}

	@Property.Not
	public List<IntPair> getSelectedElementHighlights() {
		return highlightModel.elementMatches
				.getAndEnsure(getSelectedSequenceElement()).stream()
				.map(m -> m.range).collect(Collectors.toList());
	}

	@Property.Not
	public int getSelectedElementHighlightIndex() {
		Match match = highlightModel.getMatch(getPlace().highlightIdx);
		return match == null ? -1 : match.getIndexInSelectedElementMatches();
	}

	@Property.Not
	public Object getSelectedSequenceElement() {
		int selectedElementIdx = getPlace().selectedElementIdx;
		if (selectedElementIdx == -1
				|| selectedElementIdx > filteredSequenceElements.size() - 1) {
			return null;
		} else {
			return filteredSequenceElements.get(selectedElementIdx);
		}
	}

	@Override
	public void onBind(Bind event) {
		super.onBind(event);
		if (event.isBound()) {
		} else {
			derefOracleQuery();
		}
	}

	@Override
	public void onSetSettingMaxElementRows(SetSettingMaxElementRows event) {
		String model = event.getModel();
		service.getSettings().putMaxElementRows(model);
	}

	@Override
	public List<?> provideFilteredSequenceElements(boolean ignoreRowsLimit,
			boolean onlySelectedIfAnySelected) {
		return filteredSequenceElements(sequence, true,
				onlySelectedIfAnySelected);
	}

	@Property.Not
	public int getHighlightMatchesCount() {
		return highlightModel.matches.size();
	}

	void onSelectedIndexChange() {
		properties().detailArea().set(new DetailArea(this));
		emitEvent(SequenceEvents.SelectedIndexChanged.class);
	}

	/*
	 * The sequence will be changed if the filter is changed (essentially)
	 */
	boolean filterUnchangedSequencePlaceChange(SequencePlace place) {
		boolean result = place.hasSequenceChange(lastSequenceTestPlace);
		lastSequenceTestPlace = place;
		return result;
	}

	/*
	 * Test for unchanged highlight model
	 */
	boolean filterUnchangedHighlightPlaceChange(SequencePlace place) {
		boolean result = place.hasHighlightChange(lastHighlightTestPlace);
		lastHighlightTestPlace = place;
		return result;
	}

	/*
	 * Test for unchanged selected index
	 */
	boolean filterSelectedIndexChange(SequencePlace place) {
		boolean result = place
				.hasSelectedIndexChange(lastSelectedIndexChangePlace);
		lastSelectedIndexChangePlace = place;
		return result;
	}

	void computeHighlightModel() {
		highlightModel = new HighlightModel(filteredSequenceElements,
				(Function) sequence.getDetailTransform(), getPlace().highlight,
				getPlace().highlightIdx);
		highlightModel.computeMatches();
		if (highlightModel.hasMatches()
				&& highlightModel.highlightIndex == -1) {
			highlightModel.goTo(0);
			goToHighlightModelIndex();
		}
		emitEvent(SequenceEvents.HighlightModelChanged.class);
	}

	//
	void reloadSequence() {
		InstanceQuery instanceQuery = getPlace().instanceQuery;
		if (instanceQuery.isBlank()) {
			instanceQuery = service.getInstanceQuery();
		}
		InstanceOracle.Query<? extends Sequence> oracleQuery = instanceQuery
				.toOracleQuery();
		oracleQuery.withExceptionConsumer(exception -> emitEvent(
				SequenceEvents.SequenceGenerationExceptionEvent.class,
				exception));
		if (oracleQuery.equals(this.oracleQuery)) {
			this.oracleQuery.reemit();
			return;
		}
		derefOracleQuery();
		this.oracleQuery = oracleQuery;
		oracleQuery.withInstanceConsumer(this::putSequence);
		oracleQuery.withOneOff(false);
		oracleQuery.submit();
	}

	void putSequence(Sequence sequence) {
		properties().sequence().set(sequence);
		List<?> filteredSequenceElements = filteredSequenceElements(sequence,
				false, false);
		properties().filteredSequenceElements()
				.setIfNotEqual(filteredSequenceElements);
		emitEvent(SequenceEvents.SequenceChanged.class, sequence);
	}

	void derefOracleQuery() {
		if (this.oracleQuery != null) {
			this.oracleQuery.unbind();
			this.sequence = null;// just de-ref, no need to fire changes
			this.oracleQuery = null;
		}
	}

	void updateStyles() {
		SequenceSettings settings = service.getSettings();
		FormatBuilder builder = new FormatBuilder();
		{
			/*
			 * body > page grid-template-areas - default:
			 * "header header header header" "sequence sequence sequence props"
			 * "input input output output";
			 */
			List<String> rows = new ArrayList<>();
			rows.add("header header header header");
			switch (settings.detailDisplayMode) {
			case QUARTER_WIDTH:
				rows.add("sequence sequence sequence detail");
				break;
			case HALF_WIDTH:
				rows.add("sequence sequence detail detail");
				break;
			case FULL_WIDTH:
				rows.add("detail detail detail detail");
				builder.line("body > page > sequence{display: none;}");
				break;
			case NONE:
				rows.add("sequence sequence sequence sequence");
				builder.line("body > page > detail{display: none;}");
				break;
			default:
				throw new UnsupportedOperationException();
			}
			builder.line("body > page {grid-template-rows: min-content 1fr}");
			//
			String areas = rows.stream().map(s -> Ax.format("\"%s\"", s))
					.collect(Collectors.joining(" "));
			builder.line("body > page {grid-template-areas: %s;}", areas);
		}
		String text = builder.toString();
		if (styleElement == null) {
			styleElement = StyleInjector.createAndAttachElement(text);
		} else {
			((Text) styleElement.getChild(0)).setTextContent(text);
		}
	}

	/*
	 * implementation note - property filtering/ordering are persisted in the
	 * place, but applied to the --transformed-- rows - not the sequence
	 * elements
	 */
	List<?> filteredSequenceElements(Sequence sequence, boolean ignoreRowsLimit,
			boolean onlySelectedIfAnySelected) {
		Stream<?> stream = sequence.getElements().stream();
		SequenceSearchDefinition search = getPlace().search;
		SequenceSearchDefinition preTransformSearch = null;
		SequenceSearchDefinition postTransformSearch = null;
		if (search != null) {
			preTransformSearch = search.copy();
			postTransformSearch = search.copy();
			Predicate<SearchCriterion> propertyPredicate = sc -> (sc instanceof PropertyCriterion)
					|| (sc instanceof PropertyOrderCriterion);
			preTransformSearch.removeFromSoleCriteriaGroup(propertyPredicate);
			postTransformSearch
					.removeFromSoleCriteriaGroup(propertyPredicate.negate());
			BindableSearchFilter bsf = new BindableSearchFilter(
					preTransformSearch);
			Stream<?> f_stream = stream;
			stream = bsf.callInContext(() -> f_stream.filter(bsf).sorted(bsf));
		}
		/*
		 * because *text* filtering works better on the transformed elts,
		 * transform to test - but the elements of SequenceArea.filteredElements
		 * are the original sequence elements.
		 * 
		 * There's a double-transform cost there, but it preserves the dirndl
		 * way of 'delay transformation til yr at the edge', and makes
		 * back-propagation (e.g. what event was selected?) easier
		 */
		Query<Model> query = HasFilterableText.Query.of(getPlace().filter)
				.withCaseInsensitive(true).withRegex(true);
		long limit = ignoreRowsLimit ? Integer.MAX_VALUE
				: service.getElementLimit();
		List<?> preFiltered = stream.toList();
		transformedCache = new TransformedCache(sequence);
		Stream<?> transformedElements = preFiltered.stream()
				.filter(new IndexPredicate(getPlace().selectedRange))
				.filter(e -> query.test(transformedCache.get(e)));
		if (postTransformSearch != null
				&& postTransformSearch.allCriteria().size() > 0) {
			BindableSearchFilter bsf = new BindableSearchFilter(
					postTransformSearch);
			Stream<?> f_stream = transformedElements.toList().stream();
			/*
			 * filter/sort the transformed elements
			 */
			transformedElements = bsf.callInContext(() -> f_stream
					.filter(e -> bsf.test(transformedCache.get(e)))
					.sorted((o1, o2) -> bsf.compare(transformedCache.get(o1),
							transformedCache.get(o2))));
		}
		List<?> filteredElements = transformedElements.limit(limit)
				.collect(Collectors.toList());
		preFilterCount = preFiltered.size();
		if (onlySelectedIfAnySelected) {
			List<?> selectedElements = sequenceTable.selectionSupport
					.getSelectedModels();
			if (selectedElements.size() > 0) {
				filteredElements = selectedElements;
			}
		}
		return filteredElements;
	}

	class TransformedCache implements ModelTransform {
		Map<Object, Object> transformed = AlcinaCollections.newHashMap();

		ModelTransform sequenceRowTransform;

		TransformedCache(Sequence sequence) {
			sequenceRowTransform = sequence.getRowTransform();
		}

		Object get(Object e) {
			return transformed.computeIfAbsent(e, sequenceRowTransform::apply);
		}

		@Override
		public Object apply(Object t) {
			return get(t);
		}
	}

	void goToHighlightModelIndex() {
		if (!highlightModel.hasMatches()) {
			return;
		}
		Object highlightedSequenceElement = highlightModel
				.getHighlightedElement();
		getPlace().copy().withHighlightIndicies(highlightModel.highlightIndex,
				filteredSequenceElements.indexOf(highlightedSequenceElement))
				.go();
	}

	@Property.Not
	SequencePlace getPlace() {
		return service.getPlaceProperty().get();
	}
}
