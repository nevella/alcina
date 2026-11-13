
//normalise browser markup text nodes
//
//traverse the node tree depth first, maintaining an array of nodes to remove or collate
//
//empty nodes will be removed, non-exmpty collated
//
var itr = document.createNodeIterator(container);
var incorrect = false;
var cursor = container;
//will process in reverse order once initial traversal complete
var toProcess = [];
for (; ;) {
	cursor = itr.nextNode();
	if (cursor == null) {
		break;
	}
	var testTextNode = cursor.nodeType == Node.TEXT_NODE;
	if (testTextNode) {
		var parentTag = cursor.parentNode.nodeName.toLowerCase();
		if (parentTag == 'style' || parentTag == 'script') {
			testTextNode = false;
		}
	}
	if (testTextNode) {
		if (cursor.nodeValue.length == 0) {
			toProcess.push(cursor);
		} else {
			var previous = cursor.previousSibling;
			if (previous != null && previous.nodeType == Node.TEXT_NODE) {
				toProcess.push(cursor);
			}
		}
	}
}
for (var idx = toProcess.length - 1; idx >= 0; idx--) {
	cursor = toProcess[idx];
	if (cursor.length == 0) {
		cursor.remove();
	} else {
		var previous = cursor.previousSibling;
		if (previous != null && previous.nodeType == Node.TEXT_NODE) {
			previous.nodeValue = previous.nodeValue + cursor.nodeValue;
			cursor.remove();
		}
	}
}