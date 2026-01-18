package cc.alcina.framework.servlet.component.gallery.model.searchdefeditor;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.model.Heading;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.search.SearchDefinitionEditor;
import cc.alcina.framework.servlet.component.gallery.GalleryContents;

/*
@formatter:off
 * TODO 
 
 - create a model (a user list enum, and a model which encapsulates that) editable via a MultipleSuggestions
 - link to the place
 - have a submit button that modifies the place
 - have a current value list  [done]
 - link styles
 - link to this file from the gallery
 - make test
 - iterate on FM/FI

 * @formatter:on
 */
@Registration({ GalleryContents.class,
		SearchDefinitionEditorGalleryPlace.class })
@TypedProperties
class SearchDefinitionEditorGalleryArea
		extends GalleryContents<SearchDefinitionEditorGalleryPlace>
		implements SearchDefinitionEditor.Submit.Handler {
	PackageProperties._SearchDefinitionEditorGalleryArea.InstanceProperties
			properties() {
		return PackageProperties.searchDefinitionEditorGalleryArea
				.instance(this);
	}

	Heading heading = new Heading("Sample flight definition search");

	@Directed.Wrap("definition-editor")
	SearchDefinitionEditorGalleryPlace.Definition definition;

	InfoModel model;

	SearchDefinitionEditorGalleryArea() {
		bindings().from(properties().place())
				.typed(SearchDefinitionEditorGalleryPlace.class)
				.map(place -> place.copy().definition)
				.to(properties().definition()).oneWay();
		bindings().from(properties().place())
				.typed(SearchDefinitionEditorGalleryPlace.class)
				.map(InfoModel::new).to(properties().model()).oneWay();
	}

	@Override
	public void onSubmit(SearchDefinitionEditor.Submit event) {
		SearchDefinitionEditorGalleryPlace to = place.copy();
		to.definition.def = event.getModel();
		Client.refreshOrGoTo(to);
	}

	static class InfoModel extends Model.All {
		String infoMessage;

		InfoModel(SearchDefinitionEditorGalleryPlace place) {
			infoMessage = place.definition.def.toDisplayString();
		}
	}
}
