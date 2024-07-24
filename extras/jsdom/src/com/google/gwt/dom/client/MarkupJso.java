package com.google.gwt.dom.client;

import java.util.Iterator;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.impl.JavaScriptIntList;
import com.google.gwt.core.client.impl.JavaScriptObjectList;
import com.google.gwt.dom.client.DomIds.IdList;

import cc.alcina.framework.common.client.util.Ax;

/**
 * <h2>LocalDom 3.0</h2>
 * <p>
 * This class is responsible for writing markup chunks to the jso (browser) dom,
 * and ensuring the resultant remote dom *exactly* matches the local dom. It
 * also marks each remote dom node with the correct refid.
 * <p>
 * Unlike previous approaches, this means a little more (one-time) up-front cost
 * when rendering, but no more traversal-on-event, and a much simpler mutations
 * sync algorithm.
 * <p>
 * The algorithm:
 * <ul>
 * <li>Takes a container remote node, a markup string and a list of ref-ids
 * <li>Sets the inner html of the remote node, and begins to iterate (in js)
 * with the refids (passed as a json string to avoid perf issues in devmode)
 * <li>As it iterates, any adjacent text nodes generated from the markup
 * (webkit/blink oddity) will be combined
 * <li>As it iterates, it applies the elements of the ref-id list/array to the
 * nodes.
 * <li>On iteration complete, if the ref-id list size exactly matches the
 * traversed node count, all is well
 * <li>If it does *not* match - rerun iteration, this time generating client
 * ref-ids, and send the result back to the local dom ( more expensive, but
 * should work). The localdom/java code should only hit this issue when
 * inserting markup chunks anyway (such as a document segment), so it won't
 * break because it should be able to handle changes to that document segment
 * markup anyway
 * </ul>
 */
class MarkupJso {
	/*
	 * Because of the nature of the dev protocol, this i/o token object must be
	 * passed into the #markup call (rather than with discrete input/output
	 * parameters)
	 * 
	 */
	static class MarkupToken {
		boolean ok;

		Element container;

		ElementJso remote;

		MarkupToken(Element container, String localMarkup, IdList idList) {
			this.container = container;
			this.localMarkup = localMarkup;
			localRefIds.javaArray = idList.toIntArray();
		}

		String localMarkup;

		String remoteMarkup;

		JavaScriptObjectList createdJsos = new JavaScriptObjectList();

		JavaScriptIntList localRefIds = new JavaScriptIntList();

		public void populateRemotes() {
			Iterator<JavaScriptObject> itr = createdJsos.iterator();
			container.traverse()
					.forEach(n -> n.putRemote((NodeJso) itr.next()));
		}
	}

	void markup(MarkupToken token) {
		token.remote = (ElementJso) token.container.remote();
		markup0(token);
		if (!token.ok) {
			token.remoteMarkup = token.remote.getInnerHTML0();
			LocalDom.consoleLog(Ax.format("MarkupJso :: local ::\n%s",
					token.localMarkup.replace("\n", "")), true);
			LocalDom.consoleLog(Ax.format("MarkupJso :: remote ::\n%s",
					token.remoteMarkup.replace("\n", "")), true);
		}
	}

	void markup0(MarkupToken token) {
		token.remote.setInnerHTML(token.localMarkup);
		long start = System.currentTimeMillis();
		JavaScriptObjectList createdJsos = traverseAndMark(token.remote,
				token.createdJsos, token.localRefIds);
		token.ok = createdJsos != null;
		if (token.ok) {
			token.populateRemotes();
		}
		long end = System.currentTimeMillis();
		// FIXME - logging
		// LocalDom.consoleLog(Ax.format("traverse-and-mark :: %s nodes - %sms",
		// refIds.size(), end - start), false);
		if (!token.ok) {
			LocalDom.consoleLog("MarkupJso :: !!success", true);
		}
	}

	final native JavaScriptObjectList traverseAndMark(ElementJso container,
			JavaScriptObjectList createdJsos, JavaScriptIntList refIdList) /*-{
		//traverse the node tree depth first, maintaining an array of cursors to track node position
		var resultJsos = createdJsos.@com.google.gwt.core.client.impl.JavaScriptObjectList::ensureJsArray()();
		var ids = refIdList.@com.google.gwt.core.client.impl.JavaScriptIntList::ensureJsArray()();
		var idsIdx = 0;
		var itr = document.createNodeIterator(container);
		var coalesceLists = [];
		for (; ;) {
			var node = itr.nextNode();
			resultJsos.push(node);
			if (node == null) {
				break;
			}
			if (idsIdx == ids.length) {
				return false;
			}
			node.__refid = ids[idsIdx++];
			if (node.nodeType == Node.TEXT_NODE) {
				var coalesceList = null;
				var cursor = node;
				for (; ;) {
					var next = cursor.nextSibling;
					if (next != null && next.nodeType == Node.TEXT_NODE ) {
						if(coalesceList == null){
							coalesceList = [];
							coalesceLists.push(coalesceList);
							coalesceList.push(node);
						}
						coalesceList.push(next);
						itr.nextNode();
						cursor = next;
					} else {
						break;
					}
				}
			}
		}
		//this optimises combining-multiple-nodes (to workaround webkit/style splitting of text nodes). 
		// It may be better to use the create-style::-- create-text-node::flush -- text-node.setNodeValue:: flush
		// note that large text node creation is normally injection of a style or script node, and those do not use 
		// this codepath
		for (var idx=0; idx<coalesceLists.length; idx++) {
			var coalesceList = coalesceLists[idx];
			var content='';
			for (var idx1=0; idx1<coalesceList.length; idx1++) {
				var node = coalesceList[idx1];
				content += node.nodeValue;
				if(idx1>0){
					node.remove();
				}
			}
			coalesceList[0].nodeValue = content;
		}
		if(idsIdx != ids.length){
			debugger;
		}
			// magic - return the createdJsos object to avoid round-tripping (devmode)
		return idsIdx == ids.length? createdJsos: null;
	}-*/;
}
