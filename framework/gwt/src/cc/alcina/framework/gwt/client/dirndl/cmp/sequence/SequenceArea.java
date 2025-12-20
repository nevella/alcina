package cc.alcina.framework.gwt.client.dirndl.cmp.sequence;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.gwt.dom.client.StyleElement;
import com.google.gwt.dom.client.StyleInjector;
import com.google.gwt.dom.client.Text;

import cc.alcina.framework.common.client.domain.search.BindableSearchFilter;
import cc.alcina.framework.common.client.logic.reflection.InstanceProperty;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.service.InstanceOracle;
import cc.alcina.framework.common.client.service.InstanceQuery;
import cc.alcina.framework.common.client.util.Ax;
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
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.BeforeRender;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.Bind;
import cc.alcina.framework.gwt.client.dirndl.layout.ContextService;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelTransform;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.servlet.component.shared.CopyToClipboardHandler;
import cc.alcina.framework.servlet.environment.RemoteUi;

@TypedProperties
@Directed.Delegating
public class SequenceArea extends Model.Fields
		implements SequenceEvents.FilterElements.Handler,
		SequenceEvents.HighlightElements.Handler,
		SequenceEvents.NextSelectable.Handler,
		SequenceEvents.PreviousSelectable.Handler,
		SequenceEvents.SetSettingMaxElementRows.Handler,
		SequenceEvents.HighlightModelChanged.Emitter,
		SequenceEvents.SelectedIndexChanged.Emitter, CopyToClipboardHandler,
		HasFilteredSequenceElements {
	/**
	 * The service required by the SequenceArea (provided by the host, generally
	 * the directed paren t)
	 */
	public interface Service extends ContextService {
		/**
		 * Return the definition editor/viewer, rendered as a header
		 */
		Model getSequenceDefinitionHeader();

		InstanceProperty<?, SequencePlace> getPlaceProperty();

		public interface Provider extends ContextService.Provider {
			Service getSequenceAreaService();
		}

		static class ProviderInvoker
				implements ContextService.ProviderInvoker<Service.Provider> {
			@Override
			public ContextService get(Provider provider) {
				return ((Service.Provider) provider).getSequenceAreaService();
			}

			@Override
			public Class<? extends ContextService> getServiceClass() {
				return Service.class;
			}
		}

		InstanceQuery getInstanceQuery();

		SequenceSettings getSettings();

		long getElementLimit();
	}

	public PackageProperties._SequenceArea.InstanceProperties properties() {
		return PackageProperties.sequenceArea.instance(this);
	}

	@Directed(className = "definition-header")
	Model definitionHeader;

	@Directed
	SequenceTable sequenceTable;

	@Directed
	DetailArea detailArea;

	Sequence<?> sequence;

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

	List<?> filteredSequenceElements;

	HighlightModel highlightModel;

	StyleElement styleElement;

	SequencePlace lastSequenceTestPlace = null;

	SequencePlace lastHighlightTestPlace = null;

	SequencePlace lastSelectedIndexChangePlace = null;

	Timer observableObservedTimer;

	Service service;

	public SequenceArea() {
	}

	void onSelectedIndexChange() {
		properties().detailArea().set(new DetailArea(this));
		emitEvent(SequenceEvents.SelectedIndexChanged.class);
	}

	@Override
	public void onBeforeRender(BeforeRender event) {
		service = event.node.getResolver().getService(Service.class).get();
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
		exec(reloadSequenceLambda).distinct().dispatch();
		/*
		 * Initialise with an empty sequence, it's easier than adding null
		 * checks throughout
		 */
		putSequence(Sequence.Blank.createInstance());
		super.onBeforeRender(event);
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

	InstanceOracle.Query<?> oracleQuery;

	public Runnable reloadSequenceLambda = this::reloadSequence;

	Runnable updateStylesLambda = this::updateStyles;

	//
	void reloadSequence() {
		InstanceQuery instanceQuery = getPlace().instanceQuery;
		if (instanceQuery.isBlank()) {
			instanceQuery = service.getInstanceQuery();
		}
		InstanceOracle.Query<? extends Sequence> oracleQuery = instanceQuery
				.toOracleQuery();
		oracleQuery.withExceptionConsumer(RemoteUi.Invoke.exceptionNotifier());
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
		List<?> filteredSequenceElements = filteredSequenceElements(sequence);
		properties().filteredSequenceElements().set(filteredSequenceElements);
	}

	void derefOracleQuery() {
		if (this.oracleQuery != null) {
			this.oracleQuery.unbind();
			this.sequence = null;// just de-ref, no need to fire changes
			this.oracleQuery = null;
		}
	}

	@Override
	public void onBind(Bind event) {
		super.onBind(event);
		if (event.isBound()) {
			observableObservedTimer = Timer.Provider.get()
					.getTimer(this::observableAccessed);
		} else {
			observableObservedTimer.cancel();
			derefOracleQuery();
		}
	}

	void observableAccessed() {
		SequenceObserver.get().observableObserved(sequence);
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
			builder.line("body > page {grid-template-rows: 50px 1fr}");
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

	List<?> filteredSequenceElements(Sequence sequence) {
		Stream<?> stream = sequence.getElements().stream();
		SequenceSearchDefinition search = getPlace().search;
		if (search != null) {
			BindableSearchFilter bsf = new BindableSearchFilter(search);
			stream = stream.filter(bsf).sorted(bsf);
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
		ModelTransform sequenceRowTransform = sequence.getRowTransform();
		List<?> filteredElements = (List<?>) stream
				.filter(new IndexPredicate(getPlace().selectedRange))
				.filter(e -> query.test(sequenceRowTransform.apply(e)))
				.limit(service.getElementLimit()).collect(Collectors.toList());
		return filteredElements;
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

	@Override
	public void onSetSettingMaxElementRows(SetSettingMaxElementRows event) {
		String model = event.getModel();
		service.getSettings().putMaxElementRows(model);
	}

	@Property.Not
	SequencePlace getPlace() {
		return service.getPlaceProperty().get();
	}

	@Override
	public List<?> provideFiltereedSequenceElements() {
		return filteredSequenceElements;
	}

	@Property.Not
	public int getHighlightMatchesCount() {
		return highlightModel.matches.size();
	}
}
