package cc.alcina.framework.gwt.client.dirndl.model.search;

import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.gwt.client.dirndl.model.Link;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.place.BasePlace;

@TypeSerialization(flatSerializable = false, reflectiveSerializable = false)
public class SearchDefinitionArea extends Model.All {
	public Link link;

	class Contents extends Model.All {
		String name;

		String definition;

		Contents() {
			this.name = definitionObject.getName();
			this.definition = definitionObject.toString();
		}
	}

	public Object delete;

	@Property.Not
	SearchDefinition definitionObject;

	public SearchDefinitionArea(BasePlace place,
			SearchDefinition definitionObject) {
		this.definitionObject = definitionObject;
		Contents contents = new Contents();
		link = Link.of(place).withText(null).withInner(contents);
	}
}
