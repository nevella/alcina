package cc.alcina.framework.servlet.component.gallery.model.fragmentdocument;

import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.Block;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.FragmentDocument;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.FragmentDocumentRoot;
import cc.alcina.framework.servlet.component.gallery.model.fragmentdocument.RecipeDocument.Recipe;
import cc.alcina.framework.servlet.component.gallery.model.fragmentdocument.RecipeDocument.Recipe.Descendant;

public class RecipeDocument extends FragmentDocument<Recipe> {
	@Mapping(directed = @Directed(tag = "recipe"))
	public static class Recipe extends FragmentDocumentRoot {
		@Mapping(directed = @Directed(tag = "ingredient"))
		public static class Ingredient extends Block implements Descendant {
		}

		interface Descendant extends SpecialisedDescendant {
		}
	}

	@Override
	public Class<? extends SpecialisedDescendant>
			permittedSpecialisedDescendants() {
		return Descendant.class;
	}
}
