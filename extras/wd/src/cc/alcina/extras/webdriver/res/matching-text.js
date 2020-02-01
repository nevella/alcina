var re = /##regex##/;
var walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT,
        null, false);

var node;
while (node = walker.nextNode()) {
    if (re.test(node.textContent)) {
        return node.parentNode;//assume element
    }
}
return null;