package cc.alcina.framework.gwt.client.dirndl.model.search;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.SuggestOracle;
import com.google.gwt.user.client.ui.SuggestOracle.Response;

import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.common.client.reflection.ClassReflector;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.PropertyGraphListener;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.search.SearchCriterion;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.common.client.search.SearchDefinition.EditSupport;
import cc.alcina.framework.common.client.serializer.FlatTreeSerializer;
import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.annotation.DirectedContextResolver;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.NodeContext;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.Closed;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.Commit;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.Opened;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.SelectionDirty;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent;
import cc.alcina.framework.gwt.client.dirndl.layout.ContextService;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelTransform;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.edit.ChoiceEditor;
import cc.alcina.framework.gwt.client.dirndl.model.edit.ChoicesEditorMultiple;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.Suggestor.StringAsk;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.Suggestor.SuggestOracleRouter;
import cc.alcina.framework.gwt.client.dirndl.overlay.Overlay;
import cc.alcina.framework.gwt.client.objecttree.search.packs.SearchUtils;
import cc.alcina.framework.gwt.client.util.Async;

@TypedProperties
@DirectedContextResolver
@Feature.Parent(Feature_Dirndl_SearchDefinitionEditor_Impl.class)
public class SearchDefinitionEditor extends Model.Fields
		implements ModelTransform<SearchDefinition, SearchDefinitionEditor>,
		ModelEvents.SelectionDirty.Handler, ModelEvents.Submit.Handler,
		ModelEvents.Commit.Handler, ModelEvents.Opened.Handler,
		ModelEvents.Closed.Handler {
	/**
	 * Models default logic behaviour for the definition, may be overridden
	 */
	public static class Peer {
	}

	/**
	 * the model value is the proposed updated Search Definition
	 */
	public static class Submit
			extends ModelEvent<SearchDefinition, Submit.Handler> {
		public interface Handler extends NodeEvent.Handler {
			void onSubmit(Submit event);
		}

		public interface Binding extends Handler {
			@Override
			default void onSubmit(Submit event) {
				((Model) this).bindings().onNodeEvent(event);
			}
		}

		@Override
		public void dispatch(Submit.Handler handler) {
			handler.onSubmit(this);
		}
	}

	/**
	 * Inform ancestors that the definition has changed
	 */
	public static class Changed
			extends ModelEvent<SearchDefinition, Changed.Handler> {
		@Override
		public void dispatch(Changed.Handler handler) {
			handler.onChanged(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onChanged(Changed event);
		}

		public interface Binding extends Handler {
			@Override
			default void onChanged(Changed event) {
				((Model) this).bindings().onNodeEvent(event);
			}
		}
	}

	interface Service extends ContextService {
		SearchDefinition getSearchDefinition();

		Peer getPeer();

		boolean isInitialRenderComplete();
	}

	class ServiceImpl implements Service {
		public SearchDefinition getSearchDefinition() {
			return originalDefinition;
		}

		public Peer getPeer() {
			return peer;
		}

		@Override
		public boolean isInitialRenderComplete() {
			return initialRenderComplete;
		}
	}

	public static class Router implements SuggestOracleRouter<StringAsk> {
		@Bean(PropertySource.FIELDS)
		public static class CriterionSuggestion
				implements SuggestOracle.Suggestion.Noop {
			public CriterionSuggestion() {
			}

			public CriterionSuggestion(SearchCriterion searchCriterion) {
				this.searchCriterion = searchCriterion;
			}

			public SearchCriterion searchCriterion;
		}

		@Override
		public void ask(Node node, StringAsk ask,
				Consumer<SuggestOracle.Response> responseHandler) {
			Service service = node.service(Service.class);
			SearchDefinition searchDefinition = service.getSearchDefinition();
			if (searchDefinition != null) {
				AsyncCallback<SuggestOracle.Response> criteriaResponseCallback = Async.<SuggestOracle.Response> callbackBuilder()
						.success(criteriaResponse -> mapCriteria(ask,
								criteriaResponse, responseHandler))
						.build();
				requestCriteria(ask, searchDefinition,
						criteriaResponseCallback);
			}
		}

		public void requestCriteria(StringAsk ask,
				SearchDefinition searchDefinition,
				AsyncCallback<SuggestOracle.Response> criteriaResponseCallback) {
			List<CriterionSuggestion> suggestions = listCriteria(
					searchDefinition).map(CriterionSuggestion::new).toList();
			SuggestOracle.Response response = new SuggestOracle.Response(
					(List) suggestions);
			criteriaResponseCallback.onSuccess(response);
		}

		protected boolean isSortCriteria() {
			return true;
		}

		void mapCriteria(StringAsk ask, SuggestOracle.Response criteriaResponse,
				Consumer<Response> responseHandler) {
			Stream<Searchable> stream = criteriaResponse.getSuggestions()
					.stream()
					.map(suggestion -> ((CriterionSuggestion) suggestion).searchCriterion)
					.filter(criterion -> SearchUtils.containsIgnoreCase(
							criterion.toString(), ask.getValue()))
					.map(Searchable::new);
			if (isSortCriteria()) {
				stream = stream
						.sorted(Comparator.comparing(Searchable::provideName));
			}
			List<SuggestOracle.Suggestion> searchables = stream
					.collect(Collectors.toList());
			SuggestOracle.Response response = new SuggestOracle.Response(
					searchables);
			responseHandler.accept(response);
		}

		Stream<SearchCriterion>
				listCriteria(SearchDefinition searchDefinition) {
			EditSupport editSupport = searchDefinition.editSupport();
			return (Stream) editSupport.listAvailableCriteria().stream()
					.map(Reflections::at).map(ClassReflector::newInstance)
					.sorted(Comparator
							.comparing(SearchCriterion::provideDisplayName));
		}
	}

	/*
	 * searchables attached post initial render should focus the editor
	 */
	@Property.Not
	boolean initialRenderComplete;

	/**
	 * Note - these won't be changed (they're basically the initialisation
	 * values for the ChoiceEditor)
	 */
	@Directed.Transform(ChoicesEditorMultiple.ListSuggestions.To.class)
	@ChoiceEditor.RouterType(Router.class)
	public List<Searchable> searchables = new ArrayList<>();

	@Binding(type = Type.PROPERTY)
	boolean popupsOpen;

	@Binding(type = Type.PROPERTY)
	boolean modified;

	@Property.Not
	SearchDefinition originalDefinition;

	@Property.Not
	String initialSerializedDefinition;

	@Property.Not
	PropertyGraphListener searchablesListener;

	@Property.Not
	SearchDefinition renderedDefinition;

	@Property.Not
	Peer peer;

	@Property.Not
	int openPopupCount;

	@Override
	public SearchDefinitionEditor apply(SearchDefinition searchDefinition) {
		this.originalDefinition = searchDefinition;
		return this;
	}

	@Override
	public void onNodeContext(NodeContext event) {
		event.registerService(Service.class, new ServiceImpl());
		this.initialSerializedDefinition = FlatTreeSerializer
				.serializeElided(this.originalDefinition);
		this.renderedDefinition = this.originalDefinition.cloneObject();
		searchables = renderedDefinition.allCriteria().stream()
				.map(Searchable::new).toList();
		searchablesListener = new PropertyGraphListener(
				this.renderedDefinition);
		searchablesListener.topicChangeEvent.add(this::onPropertyGraphChange);
		this.peer = new Peer();
		exec(() -> initialRenderComplete = true).deferred().dispatch();
	}

	/**
	 * one event path - choicesuggestor selected - fires DecoratorsChanged -
	 * fires SelectionDirty
	 */
	@Override
	public void onSelectionDirty(SelectionDirty event) {
		List<Searchable> model = event.getModel();
		Set<SearchCriterion> renderedCriteria = model.stream()
				.map(Searchable::searchCriterion)
				.collect(AlcinaCollectors.toLinkedHashSet());
		renderedDefinition.soleCriteriaGroup().setCriteria(renderedCriteria);
		updateGoState();
	}

	@Override
	public void onSubmit(ModelEvents.Submit event) {
		emitSubmitEvent(event);
	}

	void emitSubmitEvent(ModelEvent event) {
		if (!modified) {
			return;
		}
		event.reemitAs(this, SearchDefinitionEditor.Submit.class,
				renderedDefinition.copy());
	}

	/**
	 * from the searchables/ChoicesEditor
	 */
	@Override
	public void onCommit(Commit event) {
		emitSubmitEvent(event);
	}

	void updateGoState() {
		String renderedDefinitionSerialized = FlatTreeSerializer
				.serializeElided(renderedDefinition);
		boolean modified = !Objects.equals(initialSerializedDefinition,
				renderedDefinitionSerialized);
		properties().modified().set(modified);
		/*
		 * revisit - fires spuriously during setup, with empty criteria?
		 */
		if (modified) {
			emitEvent(SearchDefinitionEditor.Changed.class,
					renderedDefinition.copy());
		}
	}

	void onPropertyGraphChange(PropertyGraphListener.ChangeEvent changeEvent) {
		updateGoState();
	}

	public static PackageProperties._SearchDefinitionEditor properties = PackageProperties.searchDefinitionEditor;

	PackageProperties._SearchDefinitionEditor.InstanceProperties properties() {
		return PackageProperties.searchDefinitionEditor.instance(this);
	}

	@Override
	public void onClosed(Closed event) {
		if (event.getContext().getOriginatingContext().node
				.getModel() instanceof Overlay) {
			deltaOpenPopupCount(-1);
		}
		event.bubble();
	}

	@Override
	public void onOpened(Opened event) {
		if (event.getContext().getOriginatingContext().node
				.getModel() instanceof Overlay) {
			deltaOpenPopupCount(1);
		}
		event.bubble();
	}

	private void deltaOpenPopupCount(int delta) {
		openPopupCount += delta;
		properties().popupsOpen().set(openPopupCount != 0);
	}
}
