package cc.alcina.framework.servlet.component.featuretree;

import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

@Directed
@Directed.AllProperties
class Page extends Model.Fields {
	Header header = new Header();

	Main main = new Main();

	@Directed.AllProperties
	static class Header extends Model.Fields {
		String name = FeatureTree.Ui.get().getMainCaption();
	}

	@Directed.AllProperties
	static class Main extends Model.Fields {
		FeatureTable featureTable = new FeatureTable();

		Properties properties = new Properties();

		Documentation documentation = new Documentation();
	}
}
