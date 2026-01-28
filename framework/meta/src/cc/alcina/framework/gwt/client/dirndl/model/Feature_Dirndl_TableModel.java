package cc.alcina.framework.gwt.client.dirndl.model;

import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.gwt.client.dirndl.layout.Feature_Dirndl;

/**
 * The Dirndl TableModel - a fairly rich system for rendering bean tables
 *
 */
@Feature.Status.Ref(Feature.Status.In_Progress.class)
@Feature.Parent(Feature_Dirndl.class)
public interface Feature_Dirndl_TableModel extends Feature {
	/**
	 * <p>
	 * Table column models. These can optionally display sort + filter icons
	 * 
	 * @see _OrderService
	 * 
	 */
	/*
	 * @formatter:off
	 * 
	 * implementation sketch:
	 * - get test running
	 * - render column filter
	 * - table *container* emits a 'ColumnFilterOracle' descent event - hooks filters up to layer filters
	 * - render sortdirection
	 * - sass should be a table mixin
	 * 
	 * @formatter:on
	 */
	@Feature.Parent(Feature_Dirndl_TableModel.class)
	public interface _TableColumn extends Feature {
	}

	/**
	 * <p>
	 * WIP - collaboration with ancestor OrderService. Table ordering may be
	 * better handled by the container (if say the table rows are the result of
	 * a search). So the table will preferentially delegate Sort requests to an
	 * ancestor service, if one exists
	 */
	@Feature.Parent(Feature_Dirndl_TableModel.class)
	public interface _OrderService extends Feature {
	}
}
