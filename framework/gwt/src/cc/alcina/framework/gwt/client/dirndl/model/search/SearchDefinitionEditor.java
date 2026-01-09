package cc.alcina.framework.gwt.client.dirndl.model.search;

import java.util.ArrayList;
import java.util.List;

import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelTransform;
import cc.alcina.framework.gwt.client.dirndl.model.Choices;
import cc.alcina.framework.gwt.client.dirndl.model.Choices.Values;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.edit.ChoicesEditorMultiple;

@TypedProperties
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

	@Directed
	String ed = "bruce";

	@Choices.Values(To.class)
	@Directed.Transform(ChoicesEditorMultiple.ListSuggestions.To.class)
	public List<Searchable> searchables = new ArrayList<>();

	@Reflected
	static class To implements Choices.Values.ValueSupplier.ContextSensitive {
		@Override
		public List<?> apply(Node contextNode, Values t) {
			return List.of();
		}
	}
}
