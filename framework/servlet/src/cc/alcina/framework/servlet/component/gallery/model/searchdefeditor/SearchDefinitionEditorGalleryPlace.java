package cc.alcina.framework.servlet.component.gallery.model.searchdefeditor;

import java.util.Date;

import cc.alcina.framework.common.client.domain.search.criterion.CreatedFromCriterion;
import cc.alcina.framework.common.client.domain.search.criterion.CreatedToCriterion;
import cc.alcina.framework.common.client.publication.ContentDefinition;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.util.TimeConstants;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.SequenceSearchDefinition;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.search.SearchDefinitionEditor;
import cc.alcina.framework.servlet.component.gallery.GalleryPlace;
import cc.alcina.framework.servlet.component.sequence.adapter.FlightEventSearchDefinition;

public class SearchDefinitionEditorGalleryPlace extends GalleryPlace {
	public SearchDefinitionEditorGalleryPlace.Definition definition = new SearchDefinitionEditorGalleryPlace.Definition();

	@Override
	public SearchDefinitionEditorGalleryPlace copy() {
		return (SearchDefinitionEditorGalleryPlace) super.copy();
	}

	public static class Tokenizer
			extends GalleryPlace.Tokenizer<SearchDefinitionEditorGalleryPlace> {
		;
	}

	@Override
	public String getDescription() {
		return "Models a search definition as an editable area";
	}

	@TypedProperties
	public static class Definition extends Model.Fields
			implements ContentDefinition {
		@Directed.Transform(SearchDefinitionEditor.class)
		public SequenceSearchDefinition def;

		Definition() {
			def = new FlightEventSearchDefinition();
			new CreatedFromCriterion().withValue(TimeConstants.nowMinusDays(30))
					.addToSoleCriteriaGroup(def);
			new CreatedToCriterion().withValue(new Date())
					.addToSoleCriteriaGroup(def);
		}
	}
}
