package com.google.gwt.dom.client;

import com.google.gwt.core.client.JavaScriptObject;

public class MutationRecordJso extends JavaScriptObject {
	protected MutationRecordJso() {
	}

	public final native NodeListJso<Node> getAddedNodes() /*-{
    return this.addedNodes;
	}-*/;

	public final native String getAttributeName() /*-{
    return this.attributeName;
	}-*/;

	public final native String getAttributeNamespace() /*-{
    return this.attributeNamespace;
	}-*/;

	public final native String getInterchangeJson() /*-{
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

	public final MutationRecordType getMutationRecordType() {
		return MutationRecordType.valueOf(getType());
	}

	static final native String getNewValue(MutationRecordJso record)/*-{
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

	public final native String getNewValue() /*-{
	return @com.google.gwt.dom.client.MutationRecordJso::getNewValue(Lcom/google/gwt/dom/client/MutationRecordJso;)(this);
	}-*/;

	public final native NodeJso getNextSibling() /*-{
    return this.nextSibling;
	}-*/;

	public final native String getOldValue() /*-{
    return this.oldValue;
	}-*/;

	public final native NodeJso getPreviousSibling() /*-{
    return this.previousSibling;
	}-*/;

	public final native NodeListJso<Node> getRemovedNodes() /*-{
    return this.removedNodes;
	}-*/;

	public final native NodeJso getTarget() /*-{
    return this.target;
	}-*/;

	public final native String getType() /*-{
    return this.type;
	}-*/;
}