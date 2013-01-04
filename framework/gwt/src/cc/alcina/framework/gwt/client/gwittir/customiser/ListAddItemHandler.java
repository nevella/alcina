package cc.alcina.framework.gwt.client.gwittir.customiser;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;

public interface ListAddItemHandler{

	String getPrompt();

	String validateCanAdd(Object value);

	String getDefaultName();


	Object createNewItem(String nameValue);
	
}