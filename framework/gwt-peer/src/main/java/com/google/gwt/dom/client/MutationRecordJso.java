package com.google.gwt.dom.client;

import com.google.gwt.core.client.JavaScriptObject;

public final class MutationRecordJso extends JavaScriptObject {
	protected MutationRecordJso() {
	}

	public native NodeListJso<Node> getAddedNodes() /*-{
    return this.addedNodes;
	}-*/;

	public native String getAttributeName() /*-{
    return this.attributeName;
	}-*/;

	public native String getAttributeNamespace() /*-{
    return this.attributeNamespace;
	}-*/;

	public native String getInterchangeJson() /*-{
	var newValue =  @com.google.gwt.dom.client.MutationRecordJso::getNewValue(Lcom/google/gwt/dom/client/MutationRecordJso;)(this);
    var obj = {
      addedNodes : this.addedNodes.length,
      attributeName : this.attributeName,
      attributeNamespace : this.attributeNamespace,
      type : this.type,
      nextSibling : this.nextSibling == null ? 0 : 1,
      oldValue : this.oldValue,
      previousSibling : this.previousSibling == null ? 0 : 1,
      removedNodes : this.removedNodes.length,
	  newValue : newValue
    };
    return JSON.stringify(obj);
	}-*/;

	public MutationRecordType getMutationRecordType() {
		return MutationRecordType.valueOf(getType());
	}

	static native String getNewValue(MutationRecordJso record)/*-{
		var newValue = null;
		switch(record.type){
		case "attributes":
			newValue = record.target.getAttribute(record.attributeName);
			break;
		case "characterData":
			newValue = record.target.nodeValue;
			break;
		default:
			break;
		}
		return newValue;
		}-*/;

	public native String getNewValue() /*-{
	return @com.google.gwt.dom.client.MutationRecordJso::getNewValue(Lcom/google/gwt/dom/client/MutationRecordJso;)(this);
	}-*/;

	public native NodeJso getNextSibling() /*-{
    return this.nextSibling;
	}-*/;

	public native String getOldValue() /*-{
    return this.oldValue;
	}-*/;

	public native NodeJso getPreviousSibling() /*-{
    return this.previousSibling;
	}-*/;

	public native NodeListJso<Node> getRemovedNodes() /*-{
    return this.removedNodes;
	}-*/;

	public native NodeJso getTarget() /*-{
    return this.target;
	}-*/;

	public native String getType() /*-{
    return this.type;
	}-*/;
}