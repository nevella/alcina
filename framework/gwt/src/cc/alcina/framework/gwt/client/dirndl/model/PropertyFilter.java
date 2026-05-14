package cc.alcina.framework.gwt.client.dirndl.model;

import cc.alcina.framework.gwt.client.dirndl.model.TableColumnsMetadata.EditFilter;
import cc.alcina.framework.gwt.client.dirndl.model.TableView.TableContainer;

/**
 * Support for filtering beans by a property
 */
class PropertyFilter {
	TableContainer tableContainer;

	EditFilter event;

	TableModel.FilterService filterService;

	PropertyFilter(TableContainer tableContainer, EditFilter event,
			TableModel.FilterService filterService) {
		this.tableContainer = tableContainer;
		this.event = event;
		this.filterService = filterService;
	}

	void editFilter() {
	}
}
