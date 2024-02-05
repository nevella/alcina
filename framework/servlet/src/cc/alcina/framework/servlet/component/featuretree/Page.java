package cc.alcina.framework.servlet.component.featuretree;

import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

@Directed
class Page extends Model.All {
	Header header = new Header();

	Main main = new Main();

	static class Header extends Model.All {
		String name = FeatureTree.Ui.get().getMainCaption();
	}

	static class Main extends Model.All {
		FeatureTable featureTable;

		Properties properties;

		Documentation documentation = new Documentation();

		Main() {
			featureTable = new FeatureTable();
			properties = new Properties(featureTable.features);
		}
	}
}
