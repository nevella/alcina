let lastOpened = null;
const listener = function (evt) {
	let t = evt.target;
	while (t != document.documentElement) {
		if (t.tagName.toLowerCase() == 'a') {
			break;
		}
		if (t.tagName.toLowerCase() == 'epic-issue') {
			if (t.className) {
				t.className = '';
			} else {
				t.className = 'open';
				if (lastOpened != null && lastOpened != t) {
					lastOpened.className = '';
				}
			}
			lastOpened = t;
			break;
		}
		t = t.parentElement;
	}
}
window.addEventListener("click", listener);