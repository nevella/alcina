{
	var elem = arguments[0];
	elem.setAttribute("story-annotate-highlight", "");
	document.body.setAttribute("story-annotate-highlight-has", "");
	var style = document.createElement("style");
	style.innerText = "*[story-annotate-highlight]{opacity: 1.5 !important; outline: solid 2px blue; outline-offset: 4px} /*body[story-annotate-highlight-has]{opacity: 0.66;}*/";
	document.head.appendChild(style);
}