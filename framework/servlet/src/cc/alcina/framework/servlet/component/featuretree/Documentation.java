package cc.alcina.framework.servlet.component.featuretree;

import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.model.HeaderModel;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

@Directed.AllProperties
class Documentation extends Model.Fields {
	HeaderModel header = new HeaderModel("Documentation");
}
