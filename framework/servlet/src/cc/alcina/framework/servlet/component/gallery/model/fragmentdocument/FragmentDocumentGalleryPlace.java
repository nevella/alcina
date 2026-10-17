package cc.alcina.framework.servlet.component.gallery.model.fragmentdocument;

import java.util.ArrayList;
import java.util.List;

import cc.alcina.framework.common.client.gwittir.validator.NotEmptyValidator;
import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.reflection.Display;
import cc.alcina.framework.common.client.logic.reflection.ObjectPermissions;
import cc.alcina.framework.common.client.logic.reflection.Permission;
import cc.alcina.framework.common.client.logic.reflection.Validator;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.publication.ContentDefinition;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.HasDisplayName;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.model.Choices;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.edit.ChoicesEditorMultiple;
import cc.alcina.framework.servlet.component.gallery.GalleryPlace;

public class FragmentDocumentGalleryPlace extends GalleryPlace {
	public FragmentDocumentGalleryPlace.Definition definition = new FragmentDocumentGalleryPlace.Definition();

	@Override
	public FragmentDocumentGalleryPlace copy() {
		return (FragmentDocumentGalleryPlace) super.copy();
	}

	public static class Tokenizer
			extends GalleryPlace.Tokenizer<FragmentDocumentGalleryPlace> {
		;
	}

	@Override
	public String getDescription() {
		return "Shows example (client-side!) fragmentdoc parsing";
	}

	@Display.AllProperties
	@ObjectPermissions(
		read = @Permission(access = AccessLevel.EVERYONE),
		write = @Permission(access = AccessLevel.EVERYONE))
	@TypedProperties
	public static class Definition extends Model.Fields
			implements ContentDefinition {
		@Choices.EnumValues(DemoUser.class)
		@Directed.Transform(ChoicesEditorMultiple.ListSuggestions.To.class)
		@Validator(NotEmptyValidator.class)
		public List<DemoUser> users = new ArrayList<>();

		public Definition() {
			// users.add(DemoUser.nick);
		}

		public boolean testUserName(String userName) {
			return users.stream()
					.anyMatch(u -> u.toString().contains(userName));
		}

		@Property.Not
		boolean isRenderable() {
			return !users.isEmpty();
		}
	}

	@Reflected
	enum DemoUser implements HasDisplayName {
		nick, lars, jumail, vlad, jess, jian;

		@Override
		public String displayName() {
			return Ax.friendly(this);
		}
	}
}
