package cc.alcina.framework.gwt.client.dirndl.model.edit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.Consumer;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.SuggestOracle;
import com.google.gwt.user.client.ui.SuggestOracle.Response;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.EntityLocator;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.logic.reflection.reachability.ClientVisible;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.remote.ReflectiveCommonRemoteServiceAsync;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.ValueChange;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;
import cc.alcina.framework.gwt.client.dirndl.model.FormModel;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.NodeEditorContextService;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.Suggestor.StringAsk;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.Suggestor.SuggestOracleRouter;
import cc.alcina.framework.gwt.client.gwittir.widget.BoundSuggestBox.BoundSuggestOracleRequest;
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

	public static class Router implements SuggestOracleRouter<StringAsk> {
		@Bean(PropertySource.FIELDS)
		public static class EntitySuggestion
				implements SuggestOracle.Suggestion.Noop {
			public EntitySuggestion() {
			}

			public EntitySuggestion(EntityLocator entityLocator) {
				this.entityLocator = entityLocator;
			}

			public EntityLocator entityLocator;
		}

		@Override
		public void ask(Node node, StringAsk ask,
				Consumer<SuggestOracle.Response> responseHandler) {
			AsyncCallback<Response> callback = Async.<SuggestOracle.Response> callbackBuilder()
					.success(responseHandler).build();
			NodeEditorContextService editorContextService = node
					.service(NodeEditorContextService.class);
			BoundSuggestOracleRequest request = new BoundSuggestOracleRequest();
			EntityType entityType = Reflections
					.at(editorContextService.getEditingModel())
					.annotation(EntitySelector.EntityType.class);
			// boundRequest.setLimit(request.getLimit());
			request.setQuery(ask.getValue());
			request.setTargetClassName(entityType.value().getName());
			// boundRequest.setHint(hint);
			ReflectiveCommonRemoteServiceAsync.get().suggest(request, callback);
		}
	}
}
