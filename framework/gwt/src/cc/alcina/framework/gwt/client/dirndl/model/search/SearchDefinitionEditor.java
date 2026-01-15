package cc.alcina.framework.gwt.client.dirndl.model.search;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

import com.google.gwt.user.client.ui.SuggestOracle;

import cc.alcina.framework.common.client.reflection.ClassReflector;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.common.client.search.SearchDefinition.EditSupport;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.annotation.DirectedContextResolver;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.Bind;
import cc.alcina.framework.gwt.client.dirndl.layout.ContextService;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelTransform;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.edit.ChoiceEditor;
import cc.alcina.framework.gwt.client.dirndl.model.edit.ChoicesEditorMultiple;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.Suggestor.StringAsk;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.Suggestor.SuggestOracleRouter;

@TypedProperties
@DirectedContextResolver
public class SearchDefinitionEditor extends Model.Fields
		implements ModelTransform<SearchDefinition, SearchDefinitionEditor> {
	SearchDefinition searchDefinition;

	@Override
	public SearchDefinitionEditor apply(SearchDefinition searchDefinition) {
		this.searchDefinition = searchDefinition;
		searchables = searchDefinition.allCriteria().stream()
				.map(Searchable::new).toList();
		return this;
	}

	interface Service extends ContextService {
		SearchDefinition getSearchDefinition();
	}

	class ServiceImpl implements Service {
		public SearchDefinition getSearchDefinition() {
			return searchDefinition;
		}
	}

	@Override
	public void onBind(Bind event) {
		super.onBind(event);
		if (event.isBound()) {
			provideNode().getResolver().registerService(Service.class,
					new ServiceImpl());
		}
	}

	@Directed.Transform(ChoicesEditorMultiple.ListSuggestions.To.class)
	@ChoiceEditor.RouterType(Router.class)
	public List<Searchable> searchables = new ArrayList<>();

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
						.map(ClassReflector::templateInstance)
						.map(Searchable::new)
						.sorted(Comparator.comparing(Searchable::provideName))
						.toList();
				SuggestOracle.Response response = new SuggestOracle.Response(
						searchables);
				responseHandler.accept(response);
			}
		}
	}
}
