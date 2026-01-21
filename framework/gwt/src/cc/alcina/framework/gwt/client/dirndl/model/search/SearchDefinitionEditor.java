package cc.alcina.framework.gwt.client.dirndl.model.search;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import com.google.gwt.user.client.ui.SuggestOracle;

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
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.annotation.DirectedContextResolver;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.BeforeRender;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.Bind;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.SelectionDirty;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent;
import cc.alcina.framework.gwt.client.dirndl.layout.ContextService;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelTransform;
import cc.alcina.framework.gwt.client.dirndl.model.Link;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.edit.ChoiceEditor;
import cc.alcina.framework.gwt.client.dirndl.model.edit.ChoicesEditorMultiple;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.Suggestor.StringAsk;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.Suggestor.SuggestOracleRouter;

@TypedProperties
@DirectedContextResolver
public class SearchDefinitionEditor extends Model.Fields
		implements ModelTransform<SearchDefinition, SearchDefinitionEditor>,
		ModelEvents.SelectionDirty.Handler, ModelEvents.Submit.Handler {
	interface Service extends ContextService {
		SearchDefinition getSearchDefinition();

		Peer getPeer();
	}

	/**
	 * Models default logic behaviour for the definition, may be overridden
	 */
	public static class Peer {
	}

	class ServiceImpl implements Service {
		public SearchDefinition getSearchDefinition() {
			return originalDefinition;
		}

		public Peer getPeer() {
			return peer;
		}
	}

	static class Router implements SuggestOracleRouter<StringAsk> {
		@Override
		public void ask(Node node, StringAsk ask,
				Consumer<SuggestOracle.Response> responseHandler) {
			Service service = node.service(Service.class);
			SearchDefinition searchDefinition = service.getSearchDefinition();
			if (searchDefinition != null) {
				EditSupport editSupport = searchDefinition.editSupport();
				List<Searchable> searchables = editSupport
						.listAvailableCriteria().stream().map(Reflections::at)
						.map(ClassReflector::newInstance).map(Searchable::new)
						.sorted(Comparator.comparing(Searchable::provideName))
						.toList();
				SuggestOracle.Response response = new SuggestOracle.Response(
						searchables);
				responseHandler.accept(response);
			}
		}
	}

	/**
	 * Note - these won't be changed (they're basically the initialisation
	 * values for the ChoiceEditor)
	 */
	@Directed.Transform(ChoicesEditorMultiple.ListSuggestions.To.class)
	@ChoiceEditor.RouterType(Router.class)
	public List<Searchable> searchables = new ArrayList<>();

	@Directed.Wrap("go-container")
	Link go = Link.button(ModelEvents.Submit.class).withText("Go");

	/**
	 * the model value is the proposed updated Search Definition
	 */
	public static class Submit
			extends ModelEvent<SearchDefinition, Submit.Handler> {
		@Override
		public void dispatch(Submit.Handler handler) {
			handler.onSubmit(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onSubmit(Submit event);
		}

		public interface Binding extends Handler {
			@Override
			default void onSubmit(Submit event) {
				((Model) this).bindings().onNodeEvent(event);
			}
		}
	}

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

	@Override
	public SearchDefinitionEditor apply(SearchDefinition searchDefinition) {
		this.originalDefinition = searchDefinition;
		return this;
	}

	/*
	 * wip - ds.early
	 */
	@Override
	public void onBind(Bind event) {
		super.onBind(event);
		if (event.isBound()) {
			provideNode().getResolver().registerService(Service.class,
					new ServiceImpl());
		}
	}

	@Override
	public void onBeforeRender(BeforeRender event) {
		this.initialSerializedDefinition = FlatTreeSerializer
				.serializeElided(this.originalDefinition);
		this.renderedDefinition = this.originalDefinition.cloneObject();
		searchables = renderedDefinition.allCriteria().stream()
				.map(Searchable::new).toList();
		searchablesListener = new PropertyGraphListener(
				this.renderedDefinition);
		searchablesListener.topicChangeEvent.add(this::onPropertyGraphChange);
		this.peer = new Peer();
		super.onBeforeRender(event);
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

	void updateGoState() {
		String renderedDefinitionSerialized = FlatTreeSerializer
				.serializeElided(renderedDefinition);
		go.properties().disabled().set(Objects.equals(
				initialSerializedDefinition, renderedDefinitionSerialized));
	}

	void onPropertyGraphChange(PropertyGraphListener.ChangeEvent changeEvent) {
		updateGoState();
	}

	PackageProperties._SearchDefinitionEditor.InstanceProperties properties() {
		return PackageProperties.searchDefinitionEditor.instance(this);
	}

	@Override
	public void onSubmit(ModelEvents.Submit event) {
		event.reemitAs(this, SearchDefinitionEditor.Submit.class,
				renderedDefinition.copy());
	}
}
