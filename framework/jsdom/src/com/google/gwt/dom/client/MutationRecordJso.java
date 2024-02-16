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

    var obj = {
      addedNodes : this.addedNodes.length,
      attributeName : this.attributeName,
      attributeNamespace : this.aattributeNamespace,
      type : this.type,
      nextSibling : this.nextSibling == null ? 0 : 1,
      oldValue : this.oldValue,
      previousSibling : this.previousSibling == null ? 0 : 1,
      removedNodes : this.removedNodes.length
    };
    return JSON.stringify(obj);
	}-*/;

	public final MutationRecordType getMutationRecordType() {
		return MutationRecordType.valueOf(getType());
	}

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