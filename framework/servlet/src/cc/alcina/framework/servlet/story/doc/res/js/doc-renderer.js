if (location.pathname.indexOf("document_scrollToEnd.html") != -1) {
	//non-lambda due to html escaping
	window.setTimeout(function () { window.scrollTo(0, document.body.getBoundingClientRect().height); }, 100);
}
