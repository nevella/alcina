package cc.alcina.framework.gwt.client.cell.tree.res;

import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.ImageResource.ImageOptions;
import com.google.gwt.user.cellview.client.CellList;

public interface RoCellTreeRes extends CellList.Resources {
	@Override
	@Source({ CellList.Style.DEFAULT_CSS, "ro-celllist.css" })
	ListStyle cellListStyle();

	@ImageOptions(flipRtl = true)
	@Source("transparent.png")
	ImageResource none();

	public interface ListStyle extends CellList.Style {
		String cellListDepth0();
	}
}