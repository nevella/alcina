this.innerHTML = html || '';

//traverse the node tree depth first, maintaining an array of cursors to track node position
var attachIdArrayJson = '';
var ids = JSON.parse(attachIdArrayJson);
var idsIdx = 0;
var itr = document.createNodeIterator(container);
for (; ;) {
	var node = itr.nextNode();
	if (node == null) {
		break;
	}
	if (idsIdx == ids.length) {
		return false;
	}
	node.__refid = ids[idsIdx++];
	if (node.nodeType == Node.TEXT_NODE) {
		for (; ;) {
			var next = node.nextSibling;
			if (next != null && next.nodeType == Node.TEXT_NODE) {
				//coalesce
				node.textContent = node.textContent + next.textContent;
				next.remove();
			} else {
				break;
			}
		}
	}
}
return idsIdx == ids.length;