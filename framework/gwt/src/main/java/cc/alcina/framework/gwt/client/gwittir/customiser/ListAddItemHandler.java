package cc.alcina.framework.gwt.client.gwittir.customiser;

public interface ListAddItemHandler {
	Object createNewItem(String nameValue);

	String getDefaultName();

	String getPrompt();

	String validateCanAdd(Object value);
}