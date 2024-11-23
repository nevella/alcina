package cc.alcina.framework.servlet.component.test.client;

import java.util.List;

import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Rendered;
import cc.alcina.framework.gwt.client.dirndl.layout.RestrictedHtmlTag;
import cc.alcina.framework.gwt.client.dirndl.model.CollectionDeltaModel;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

class TestChubbyTree {
	void run() {
		AlcinaGwtTestClient.Utils.clearRootPanel();
		TestContainer cont = new TestContainer();
		Rendered rendered = new DirectedLayout().render(cont).getRendered();
		rendered.appendToRoot();
		TestContainer.properties.collection.set(cont, List.of("bruce", "was"));
		TestContainer.properties.collection.set(cont,
				List.of("bruce", "was", "here", "fully"));
		TestContainer.properties.collection.set(cont,
				List.of("bruce", "there", "was", "here", "fully"));
	}

	@Directed
	@TypedProperties
	static class TestContainer extends Model.Fields {
		static PackageProperties._TestChubbyTree_TestContainer properties = PackageProperties.testChubbyTree_testContainer;

		static class Style extends Model.All implements RestrictedHtmlTag {
			@Binding(type = Type.INNER_TEXT)
			String contents = "test-container, coll, element{display: block}\ncoll{margin: 1rem; border: solid 1px #ccc; padding: 1rem;}";
		}

		@Directed
		Style style = new Style();

		@Directed
		String heading = "chubby tree";

		List<String> collection;

		@Directed.Wrap("coll")
		CollectionDeltaModel collectionRepresentation = new CollectionDeltaModel();

		TestContainer() {
			bindings().from(this).on(properties.collection)
					.to(collectionRepresentation)
					.on(CollectionDeltaModel.properties.collection).oneWay();
		}
	}
}
