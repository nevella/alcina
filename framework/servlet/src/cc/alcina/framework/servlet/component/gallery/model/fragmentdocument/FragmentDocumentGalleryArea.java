package cc.alcina.framework.servlet.component.gallery.model.fragmentdocument;

import cc.alcina.framework.common.client.dom.DomDocument;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.NestedName;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.NodeContext;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.Submit;
import cc.alcina.framework.gwt.client.dirndl.layout.LeafModel;
import cc.alcina.framework.gwt.client.dirndl.model.Link;
import cc.alcina.framework.gwt.client.dirndl.model.edit.FocusOnBindMarker;
import cc.alcina.framework.gwt.client.dirndl.model.edit.StringInput;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.FragmentDocument;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.MarkupSchema;
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
@Registration({ GalleryContents.class, FragmentDocumentGalleryPlace.class })
@TypedProperties
@Directed(tag = "fragment-document-area")
@Directed(tag = "contents")
class FragmentDocumentGalleryArea
		extends GalleryContents<FragmentDocumentGalleryPlace>
		implements ModelEvents.Submit.Handler {
	PackageProperties._FragmentDocumentGalleryArea.InstanceProperties
			subtypeProperties() {
		return PackageProperties.fragmentDocumentGalleryArea.instance(this);
	}

	@Directed(tag = "sub-heading")
	String heading = "Markup and parsed markup trees, demonstrating FragmentDocument usage";

	@Directed.Transform(StringInput.To.class)
	@FocusOnBindMarker
	@StringInput.TextArea
	String markup = "<recipe>\n\t<ingredient>flour</ingredient>\n\t<ingredient>water</ingredient>\n\t<div>some generic div</div>\n\t<unknown/>\n</recipe>";

	@Directed.Wrap("document-tree-representation")
	@Directed.Transform(LeafModel.HtmlBlock.To.class)
	String documentTreeRepresentation;

	Link submit = Link.of(ModelEvents.Submit.class);

	@Override
	public void onNodeContext(NodeContext event) {
		parse();
	}

	void parse() {
		DomDocument fromDocument = DomDocument.from(markup, true);
		FragmentDocument fragmentDocument = FragmentDocument.builder()
				.withDocumentClass(RecipeDocument.class)
				.withMarkupSchema(MarkupSchema.FragmentDocumentClass.TYPE)
				.withFromDocument(fromDocument).build().link();
		FormatBuilder format = new FormatBuilder();
		format.line("parsed! %s %s nodes", System.currentTimeMillis(),
				fragmentDocument.getRoot().stream().count());
		format.newLine();
		fragmentDocument.getRoot().stream().forEach(node -> {
			format.indent((int) (node.ancestors().stream().count() * 2 - 2));
			format.line(NestedName.get(node));
		});
		String string = format.toString();
		subtypeProperties().documentTreeRepresentation().set(string);
	}

	@Override
	public void onSubmit(Submit event) {
		parse();
	}
}
