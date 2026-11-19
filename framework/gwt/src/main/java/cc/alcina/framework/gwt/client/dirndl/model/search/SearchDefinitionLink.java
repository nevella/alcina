package cc.alcina.framework.gwt.client.dirndl.model.search;

import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.model.Link;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.place.BasePlace;

public class SearchDefinitionLink extends Model.Fields {
	class Contents extends Model.All {
		String name;

		String definition;

		Contents() {
			this.name = SearchDefinitionLink.this.definition.getName();
			this.definition = SearchDefinitionLink.this.definition.toString();
		}
	}

	@Directed
	public Link link;

	@Directed
	public Link delete;

	@Property.Not
	SearchDefinition definition;

	public transient Object modelEventData;

	public SearchDefinitionLink(BasePlace searchPlace,
			SearchDefinition definition, Object modelEventData) {
		this.definition = definition;
		this.modelEventData = modelEventData;
		Contents contents = new Contents();
		link = Link.of(searchPlace).withText(null).withInner(contents);
	}

	public SearchDefinitionLink withDelete() {
		delete = Link.button(ModelEvents.Delete.class).withModelEventData(this)
				.withText(null);
		return this;
	}
}
