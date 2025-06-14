package com.google.gwt.dom.client;

import java.util.Iterator;
import java.util.List;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.impl.JavaScriptIntList;
import com.google.gwt.core.client.impl.JavaScriptObjectList;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.util.Ax;

/**
 * <h2>LocalDom 3.0</h2>
 * <p>
 * This class is responsible for writing markup chunks to the jso (browser) dom,
 * and ensuring the resultant remote dom *exactly* matches the local dom. It
 * also marks each remote dom node with the correct attachId.
 * <p>
 * Unlike previous approaches, this means a little more (one-time) up-front cost
 * when rendering, but no more traversal-on-event, and a much simpler mutations
 * sync algorithm.
 * <p>
 * The algorithm:
 * <ul>
 * <li>Takes a container remote node, a markup string and a list of ref-ids
 * <li>Sets the inner html of the remote node, and begins to iterate (in js)
 * with the attachIds (passed as a json string to avoid perf issues in devmode)
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

		MarkupToken(Element container, String localMarkup,
				IdProtocolList idList) {
			this.container = container;
			this.localMarkup = localMarkup;
			localAttachIds.javaArray = idList.toIntArray();
		}

		String localMarkup;

		String remoteMarkup;

		JavaScriptObjectList createdJsos = new JavaScriptObjectList();

		JavaScriptIntList localAttachIds = new JavaScriptIntList();

		void populateRemotes() {
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

	/**
	 * This will throw if there's an issue with matching the resultant browser
	 * node structure to the local
	 * 
	 * There are two possible causes - invalid local markup (say nested A tags
	 * or some other browser no-no), or an issue with the protocol, probably
	 * around text nodes. Check the first first, and dodge with
	 * LocalDom.validateMarkup if invalid structure is indeed the issue
	 */
	void markup0(MarkupToken token) {
		if (token.localMarkup != null) {
			token.remote.setInnerHTML(token.localMarkup);
		}
		long start = System.currentTimeMillis();
		try {
			JavaScriptObjectList createdJsos = traverseAndMark(token.remote,
					token.createdJsos, token.localAttachIds);
			token.ok = true;
			token.populateRemotes();
		} catch (RuntimeException e) {
			token.remoteMarkup = token.remote.getInnerHTML0();
			List<DomNode> redundantAs = token.container.asDomNode().stream()
					.filter(n -> n.tagIs("a") && !n.has("href")).toList();
			Exception reportedException = new IllegalArgumentException(Ax
					.format("invalid markup -- mismatched remote, local markup -- \nlocal:\n%s\n\nremote:\n%s",
							Ax.ntrim(token.localMarkup, 500),
							Ax.ntrim(token.remoteMarkup, 500)));
			LocalDom.topicPublishException().publish(reportedException);
			Ax.out("Local attachIds:\n%s",
					token.container.implAccess().toLocalAttachIdString(true));
			/* probably some odd dom - compare to roundtripped
			@formatter:off
			 java.nio.file.Files.write(java.nio.file.Path.of("/g/alcina/tmp/t0.html"), token.localMarkup.replace("&nbsp;","\u00A0").getBytes());
			 java.nio.file.Files.write(java.nio.file.Path.of("/g/alcina/tmp/t1.html"), token.remoteMarkup.replace("&nbsp;","\u00A0").getBytes());
			
			 java.nio.file.Files.write(java.nio.file.Path.of("/g/alcina/tmp/t2.txt"), e.toString().getBytes());
			 java.nio.file.Files.write(java.nio.file.Path.of("/g/alcina/tmp/t3.txt"), token.container.implAccess().toLocalAttachIdString(false).getBytes());
			 @formatter:on
			 */
			throw e;
		}
		long end = System.currentTimeMillis();
		// FIXME - logging
		// LocalDom.consoleLog(Ax.format("traverse-and-mark :: %s nodes - %sms",
		// attachIds.size(), end - start), false);
		if (!token.ok) {
			LocalDom.consoleLog("MarkupJso :: !!success", true);
		}
	}

	/*
	 * See the IdProtocolList protocol. id#0 is a 'special' marker code
	 */
	final native JavaScriptObjectList traverseAndMark(ElementJso container,
			JavaScriptObjectList createdJsos, JavaScriptIntList attachIdList) /*-{
	
//traverse the node tree depth first, maintaining an array of cursors to track node position
//traverse the node tree depth first, maintaining an array of cursors to track node position
var resultJsos = createdJsos.@com.google.gwt.core.client.impl.JavaScriptObjectList::ensureJsArray()();
var ids = attachIdList.@com.google.gwt.core.client.impl.JavaScriptIntList::ensureJsArray()();
var idsIdx = 0;
var itr = document.createNodeIterator(container);
var incorrect = false;
var cursor = container;
var idJso = [];
for (; ;) {
	if (idsIdx == ids.length) {
		cursor = itr.nextNode();
		incorrect = cursor != null;
		resultJsos.push(cursor);
		break;
	}
	var int0 = ids[idsIdx++];
	if (int0 == 0) {
		//special - PROTOCOL_0_SPECIAL
		var int1 = ids[idsIdx++];
		switch (int1) {
			//PROTOCOL_1_TEXT_BLANK_NON_SEQUENCE
			case 0: {
				var attachId = ids[idsIdx++];
				var parentAttachId = ids[idsIdx++];
				var previousSiblingAttachId = ids[idsIdx++];
				var parentNode = idJso[parentAttachId];
				var previousSiblingNode = idJso[previousSiblingAttachId];
				previousSiblingNode = !previousSiblingNode ? null : previousSiblingNode;
				if(previousSiblingNode && previousSiblingNode.parentNode != parentNode){
					debugger;
					throw "incorrect previousSiblingNode parenting";
				}
				var nextSiblingNode = previousSiblingNode == null ? parentNode.firstChild : previousSiblingNode.nextSibling;
				var text = cursor.ownerDocument.createTextNode("");
				resultJsos.push(text);
				text.__attachId = attachId;
				idJso[text.__attachId] = text;
				parentNode.insertBefore(text, nextSiblingNode);
				itr.nextNode();
				break;
			}
			//PROTOCOL_1_TEXT_NODE_SEQUENCE
			case 1: {
			//unused
				var attachId = ids[idsIdx++];
				var nodeCount = ids[idsIdx++];
				var attachIds = [];
				var lengths = [];
				var strings = [];
				for (var idx1 = 0; idx1 < nodeCount; idx1++) {
					attachIds.push(ids[idsIdx++]);
				}
				var partLengthSum = 0;
				for (var idx1 = 0; idx1 < nodeCount; idx1++) {
					var length = ids[idsIdx++];
					lengths.push(length);
					partLengthSum += length;
				}
				var content = cursor.nodeValue;
				var totalLength = content.length;
				var offset = totalLength - partLengthSum;
				cursor.nodeValue = content.substring(0, offset);
				var appendCursor = cursor;
				var parentNode = appendCursor.parentNode;
				for (var idx1 = 0; idx1 < nodeCount; idx1++) {
					var attachId = attachIds[idx1];
					var length = lengths[idx1];
					var nodeContent = content.substring(offset, length);
					offset += length;
					var text = cursor.ownerDocument.createTextNode(nodeContent);
					resultJsos.push(text);
					text.__attachId = attachId;
					idJso[text.__attachId] = text;
					parentNode.insertBefore(text,appendCursor.nextSibling);
					itr.nextNode()
					appendCursor = text;
				}
				break;
			}
			default:
				debugger;
				throw "Unsupported protocol token: " + int1;
		}

	} else {

		//common case
		cursor = itr.nextNode();
		if (cursor == null) {
			incorrect = true;
			break;
		}
		if (cursor.nodeType == Node.TEXT_NODE) {
			
			//Coalesce sequential text nodes produced by browser parseing of a DOM string (KHTML/WebKit legacy behavior)
			
			var peek = cursor.nextSibling;
			while (peek != null && peek.nodeType == Node.TEXT_NODE) {
				cursor.nodeValue = cursor.nodeValue + peek.nodeValue;
				peek = peek.nextSibling;
				itr.nextNode()
			}
		}
		resultJsos.push(cursor);
		cursor.__attachId = int0;
		idJso[cursor.__attachId] = cursor;
	}
}

if (idsIdx != ids.length || incorrect) {
	var buffer = "";
	debugger;
	if (!@com.google.gwt.core.client.GWT::isScript()()) {
		for (var idx = 0; idx < resultJsos.length; idx++) {
			var jso = resultJsos[idx];
			if (jso != null) {
				buffer += "" + jso.__attachId + " - " + jso.nodeName.toLowerCase() + "\n";
			} else {
				buffer += "[null jso] - idx " + idx + "\n";
			}
		}
			buffer+="\n\n----------\n\n";

		for (var idx = 0; idx < resultJsos.length; idx++) {
			var jso = resultJsos[idx];
			
			if (jso != null) {
				var cursor = jso;
				while(cursor!=null){
					buffer+=" ";
					cursor=cursor.parentNode;
				}
				buffer += "" + jso.nodeName.toLowerCase() + " - " + jso.__attachId;
				if(jso.nodeType == Node.TEXT_NODE){
					buffer += "[" + jso.nodeValue.length + "] ";
					var str= jso.nodeValue;
					str=str.substring(0,Math.min(20,str.length));
					str=str.replaceAll("\n","\\n").replaceAll("\r","\\r").replaceAll("\t","\\t");
					buffer+= str;
				}
				buffer+="\n";
			} else {
				buffer += "[null jso] - idx " + idx + "\n";
			}
		}
	}
	throw "incorrect-idlist-length\n" + buffer;
}
// magic - return the createdJsos object to avoid round-tripping (devmode)
return idsIdx == ids.length ? createdJsos : null;
	}-*/;
}
