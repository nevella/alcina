let idx = 0;
let lastIdx = -1;
function view(linkId) {
	if(window.event && (window.event.metaKey || window.event.ctrlKey)){
		return true;
	}
	show(parseInt(linkId.replace("link_", "")));
	return false;
}
function next() {
	show(idx + 1);
}
function previous() {
	show(idx - 1);
}
function toggleFull(){
	document.body.className=(document.body.className)?"":"full";	
}
function show(linkIdx) {
	if (linkIdx < 0) {
		linkIdx = __viewer_data.length - 1;
	}
	if (linkIdx >= __viewer_data.length) {
		linkIdx = 0;
	}
	if (lastIdx != -1) {
		document.getElementById('link_' + lastIdx).className = '';
	}
	idx = linkIdx;
	lastIdx = idx;
	document.getElementById('link_' + idx).className = 'selected';
	document.getElementById('img__').className="loading";
	document.getElementById('img__').src = __viewer_data[idx].url;
	window.event.preventDefault();
}
window.onkeydown = function(e) {
	switch (e.keyCode) {
		case 37:
			previous();
			break;
		case 39:
			next();
			break;
	}
};
window.onload = function() { show(0); };