package cc.alcina.framework.gwt.client.dirndl.model.edit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.SuggestOracle;
import com.google.gwt.user.client.ui.SuggestOracle.Response;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.EntityLocator;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.logic.reflection.reachability.ClientVisible;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.remote.ReflectiveCommonRemoteServiceAsync;
import cc.alcina.framework.common.client.search.EntityCriterion;
import cc.alcina.framework.common.client.util.AlcinaCollections;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.NodeContext;
import cc.alcina.framework.gwt.client.dirndl.event.ValueChange;
import cc.alcina.framework.gwt.client.dirndl.layout.ContextService;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;
import cc.alcina.framework.gwt.client.dirndl.layout.LeafModel;
import cc.alcina.framework.gwt.client.dirndl.model.FormModel;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.NodeEditorContextService;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.Suggestor.StringAsk;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.Suggestor.SuggestOracleRouter;
import cc.alcina.framework.gwt.client.gwittir.widget.BoundSuggestBox.BoundSuggestOracleRequest;
import cc.alcina.framework.gwt.client.gwittir.widget.BoundSuggestOracleResponseElement.BoundSuggestOracleModel;
import cc.alcina.framework.gwt.client.gwittir.widget.BoundSuggestOracleResponseElement.BoundSuggestOracleSuggestion;
import cc.alcina.framework.gwt.client.util.Async;

@Directed.Delegating
@Bean(PropertySource.FIELDS)
@TypedProperties
@Registration({ Model.Value.class, FormModel.Editor.class, Entity.class })
public class EntitySelector<E extends Entity> extends Model.Value<E>
		implements ValueChange.Container {
	@ClientVisible
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target({ ElementType.METHOD, ElementType.FIELD, ElementType.TYPE })
	public @interface EntityType {
		Class<? extends Entity> value();
	}

	public interface EntityContextTextProviderService extends ContextService {
		String getEntityDisplayText(Entity entity);
	}

	public static class Router implements SuggestOracleRouter<StringAsk> {
		@Bean(PropertySource.FIELDS)
		public static class EntitySuggestion
				implements SuggestOracle.Suggestion.Noop {
			public EntityLocator entityLocator;

			public EntitySuggestion() {
			}

			public EntitySuggestion(EntityLocator entityLocator) {
				this.entityLocator = entityLocator;
			}
		}

		Function<Entity, Model> entityRenderer = this::renderSuggestionString;

		private EntityContextTextProviderService textProviderService;

		@Override
		public void onNodeContext(NodeContext event) {
			event.getContext().node.service(ChoiceEditor.Service.class)
					.setValueTransformer(entityRenderer);
			textProviderService = event.getContext().node
					.service(EntityContextTextProviderService.class);
		}

		@Override
		public void ask(Node node, StringAsk ask,
				Consumer<SuggestOracle.Response> responseHandler) {
			Function valueTransformer = node.service(ChoiceEditor.Service.class)
					.getValueTransformer();
			Consumer<SuggestOracle.Response> wrappedConsumer = response -> {
				registerResponseStrings(response);
				responseHandler.accept(response);
			};
			AsyncCallback<Response> callback = Async.<SuggestOracle.Response> callbackBuilder()
					.success(wrappedConsumer).build();
			NodeEditorContextService editorContextService = node
					.service(NodeEditorContextService.class);
			BoundSuggestOracleRequest request = new BoundSuggestOracleRequest();
			Class<? extends Entity> entityType = null;
			Bindable editingModel = editorContextService.getEditingModel();
			EntityType entityTypeAnnotation = Reflections.at(editingModel)
					.annotation(EntitySelector.EntityType.class);
			if (entityTypeAnnotation != null) {
				entityType = entityTypeAnnotation.value();
			} else if (editingModel instanceof EntityCriterion) {
				entityType = ((EntityCriterion) editingModel).getObjectClass();
			} else {
				throw new UnsupportedOperationException();
			}
			// boundRequest.setLimit(request.getLimit());
			if (editingModel instanceof BoundSuggestOracleModel) {
				request.setModel((BoundSuggestOracleModel) editingModel);
			}
			request.setQuery(ask.getValue());
			request.setTargetClassName(entityType.getName());
			// boundRequest.setHint(hint);
			ReflectiveCommonRemoteServiceAsync.get().suggest(request, callback);
		}

		Model renderSuggestionString(Entity entity) {
			String text = null;
			text = entityTexts.get(entity);
			if (text == null) {
				if (textProviderService != null) {
					text = textProviderService.getEntityDisplayText(entity);
				}
			}
			if (text == null) {
				text = entity.toStringId();
			}
			return new LeafModel.TagText("entity", text);
		}

		Map<Entity, String> entityTexts = AlcinaCollections.newLinkedHashMap();

		void registerResponseStrings(Response response) {
			entityTexts.clear();
			response.getSuggestions().forEach(suggestion -> {
				if (suggestion instanceof BoundSuggestOracleSuggestion) {
					Object typedValue = ((BoundSuggestOracleSuggestion) suggestion)
							.getTypedValue();
					if (typedValue instanceof Entity.Suggestion) {
						Entity.Suggestion entitySuggestion = (Entity.Suggestion) typedValue;
						entityTexts.put(entitySuggestion.entity,
								entitySuggestion.suggestionString);
					}
				}
			});
		}
	}

	private E value;

	@Override
	@Directed.Transform(ChoicesEditorSingle.SingleSuggestions.To.class)
	@ChoiceEditor.RouterType(Router.class)
	public E getValue() {
		return value;
	}

	@Override
	public void setValue(E value) {
		set("value", this.value, value, () -> this.value = value);
	}
}
