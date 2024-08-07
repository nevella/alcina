var elem = arguments[0];
//var fromTop = %s;
elem.scrollIntoView();
var rect = elem.getBoundingClientRect();
var delta = rect.y - fromTop;
window.scrollTo(window.scrollX, window.scrollY + delta);