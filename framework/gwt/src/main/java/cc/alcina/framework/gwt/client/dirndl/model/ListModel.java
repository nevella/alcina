package cc.alcina.framework.gwt.client.dirndl.model;

import java.util.ArrayList;
import java.util.List;

import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;

/**
 * wraps a list for rendering
 */
@Directed.Delegating
public class ListModel extends Model.All {
	public List<Model> list = new ArrayList<>();
}
