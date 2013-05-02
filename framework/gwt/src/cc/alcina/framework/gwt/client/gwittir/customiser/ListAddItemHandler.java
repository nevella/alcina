package cc.alcina.framework.gwt.client.gwittir.customiser;


public interface ListAddItemHandler{

	String getPrompt();

	String validateCanAdd(Object value);

	String getDefaultName();


	Object createNewItem(String nameValue);
	
}