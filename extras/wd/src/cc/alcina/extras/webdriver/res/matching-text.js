var re = /##regex##/;
var walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT,
	null, false);

var node;
while (node = walker.nextNode()) {
	var parent = node.parentNode;
	var skip = false;
	switch (parent.tagName.toLowerCase()) {
		case "script":
		case "style":
			skip = true;
			break
	}
	if (!skip && re.test(node.textContent)) {
		return parent;
	}
}
return null;