package cc.alcina.framework.gwt.client.dirndl.model;

import java.util.ArrayList;
import java.util.List;

public class GridData extends Model.All {
	public static class Row extends Model.All {
		public String caption = "";

		public List<String> data = new ArrayList<>();
	}

	public Row header = new Row();

	public List<Row> rows = new ArrayList<>();
}
