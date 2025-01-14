package cc.alcina.framework.servlet.component.gallery.model.multiplesuggestions;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.Submit;
import cc.alcina.framework.gwt.client.dirndl.model.BeanForm;
import cc.alcina.framework.gwt.client.dirndl.model.BeanForm.ClassName;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
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
@Registration({ GalleryContents.class, MultipleSuggestionsGalleryPlace.class })
@TypedProperties
class MultipleSuggestionsGalleryArea
		extends GalleryContents<MultipleSuggestionsGalleryPlace>
		implements ModelEvents.Submit.Handler {
	static PackageProperties._MultipleSuggestionsGalleryArea properties = PackageProperties.multipleSuggestionsGalleryArea;

	String self = "multiple suggers";

	@Directed(tag = "definition-editor")
	@Directed(bindToModel = false)
	@Directed.Transform(BeanForm.Editor.Adjunct.class)
	@BeanForm.Classes({ ClassName.vertical, ClassName.grid,
			ClassName.tight_rows, ClassName.horizontal_validation })
	@BeanForm.Headings(heading = "Report Definition")
	MultipleSuggestionsGalleryPlace.Definition definition;

	InfoModel model;

	MultipleSuggestionsGalleryArea() {
		bindings().from(this).on(properties.place)
				.typed(MultipleSuggestionsGalleryPlace.class)
				.map(place -> place.copy().definition).to(this)
				.on(properties.definition).oneWay();
		bindings().from(this).on(properties.place)
				.typed(MultipleSuggestionsGalleryPlace.class)
				.filter(place -> place.definition.isRenderable())
				.map(InfoModel::new).to(this).on(properties.model).oneWay();
	}

	@Override
	public void onSubmit(Submit event) {
		MultipleSuggestionsGalleryPlace to = place.copy();
		to.definition = this.definition;
		Client.refreshOrGoTo(to);
	}

	static class InfoModel extends Model.All {
		String infoMessage;

		InfoModel(MultipleSuggestionsGalleryPlace place) {
			infoMessage = Ax.format("Users: %s",
					place.definition.users.toString());
		}
	}
}
