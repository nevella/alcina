
//traverse the node tree depth first, maintaining an array of cursors to track node position
//traverse the node tree depth first, maintaining an array of cursors to track node position
/*
var resultJsos = createdJsos.@com.google.gwt.core.client.impl.JavaScriptObjectList::ensureJsArray()();
var ids = attachIdList.@com.google.gwt.core.client.impl.JavaScriptIntList::ensureJsArray()();
*/
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
		//special
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
				var text = cursor.ownerDocument.createTextNode("");
				resultJsos.push(text);
				text.__attachId = attachId;
				idJso[text.__attachId] = text;
				parentNode.insertBefore(text, previousSiblingNode);
				break;
			}
			//PROTOCOL_1_TEXT_BLANK_NON_SEQUENCE
			case 1: {
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
				for (var idx1 = 0; idx1 < nodeCount; idx1++) {
					var attachId = attachIds[idx1];
					var length = lengths[idx1];
					var nodeContent = content.substring(offset, length);
					offset += length;
					var text = cursor.ownerDocument.createTextNode(nodeContent);
					resultJsos.push(text);
					text.__attachId = attachId;
					idJso[text.__attachId] = text;
					parentNode.insertBefore(appendCursor.nextSibling);
					appendCursor = text;
				}
				break;
			}
			default:
				throw "Unsupported protocol token: " + int1;
		}

	} else {

		//common case
		cursor = itr.nextNode();
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
	/*
	if (!@com.google.gwt.core.client.GWT::isScript()()) {
		for (var idx = 0; idx < resultJsos.length; idx++) {
			var jso = resultJsos[idx];
			if (jso != null) {
				buffer += "" + jso.__attachId + " - " + jso.nodeName + "\n";
			} else {
				buffer += "[null jso] - idx " + idx + "\n";
			}
		}
	}
		*/
	throw "incorrect-idlist-length\n" + buffer;
}
// magic - return the createdJsos object to avoid round-tripping (devmode)
return idsIdx == ids.length ? createdJsos : null;