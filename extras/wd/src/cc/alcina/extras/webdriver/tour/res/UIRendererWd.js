class UIRendererWd {
	constructor() {

	}

	start() {
		let css = __UIRendererWd_css;
		let style = document.createElement("style");
		style.innerText = css;
		document.head.appendChild(style);
	}
	renderRelative(popupInfoJson, html) {

		let popupInfo = JSON.parse(popupInfoJson);
		let relativeTo = this.evalSelector(popupInfo.relativeTo.element);
		let div = document.createElement("div");
		div.innerHTML = html;
		let content = div.firstElementChild;
		content.addEventListener('click', e => content.remove(), false);
		document.body.appendChild(content);
		this.position(content, relativeTo, popupInfo.relativeTo);
	}

	position(elem, relativeToElem, relativeTo) {
		let rect = relativeToElem.getBoundingClientRect();
		let absRect = this.absRect(rect);
		let clientWidth = document.documentElement.clientWidth;
		let clientHeight = document.documentElement.clientHeight;
		let directions = relativeTo.direction.split("_");
		let distOffset = 14;
		switch (directions[0]) {
			case "TOP":
				elem.style.bottom = (clientHeight - absRect.top + distOffset) + "px";
				break;
			case "BOTTOM":
				elem.style.top = (absRect.bottom + distOffset) + "px";
				break;
			case "LEFT":
				elem.style.right = (clientWidth - absRect.left + distOffset) + "px";
				break;
			default:
				throw `not handled direction (axis 1): ${directions}`;
		}
		let bubbleOffset = 30 + 7 + 2;//offset + 1/2 triangle width + border width (although....it's transfomed??')
		switch (directions[1]) {
			case "LEFT": {
				let fudge = 5;
				elem.style.left = Math.max(0, absRect.left + (absRect.right - absRect.left) / 2 - bubbleOffset + fudge) + "px";
			}
				break;
			case "RIGHT": {
				elem.style.right = (clientWidth - absRect.right + (absRect.right - absRect.left) / 2 - bubbleOffset) + "px";
			}
				break;
			case "TOP": {
				let fudge = 15;
				bubbleOffset = 30 + 7 + 2 - fudge;//offset + 1/2 triangle width + border width (although....it's transfomed??')
				elem.style.top = Math.max(0, absRect.top - bubbleOffset) + "px";
			}
				break;
			default:
				throw `not handled direction (axis 2): ${directions}`;
		}
		if (relativeTo.bubble) {
			let bubbleElem = document.createElement("div");
			bubbleElem.className = `ol-tour-bubble axis-1-${directions[0].toLowerCase()} axis-2-${directions[1].toLowerCase()}`;
			bubbleElem.id = elem.id + "_bubble";
			elem.appendChild(bubbleElem);
			//			switch (directions[0]) {
			//				case "TOP":
			//					bubbleElem.style.bottom = (clientHeight - absRect.top) + "px";
			//					bubbleElem.style.left = (absRect.left + (rect.width / 2) - 5) + "px";
			//					break;
			//				case "BOTTOM":
			//					bubbleElem.style.top = (absRect.bottom + 0) + "px";
			//					bubbleElem.style.left = (absRect.left + (rect.width / 2) - 5) + "px";
			//					break;
			//				default:
			//					throw `not handled direction (bubble): ${directions}`;
			//			}
		}
	}

	absRect(rect) {
		return {
			top: rect.top + window.scrollY,
			bottom: rect.bottom + window.scrollY,
			left: rect.left + window.scrollX,
			right: rect.right + window.scrollX
		};
	}

	evalSelector(selector) {
		if (selector.indexOf("/") == 0) {
			return document.evaluate(selector, document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;
		} else {
			return document.querySelector(selector);
		}
	}
	getForSelectors(selectors) {
		for (const selector of selectors) {
			let relativeTo = this.evalSelector(selector);
			if (relativeTo != null) {
				return relativeTo;
			}
		}
		return null;
	}
	remove(id) {
		let elt = document.getElementById(id);
		if (elt) {
			elt.remove();
		}
		let bubble = document.getElementById(id + "_bubble");
		if (bubble) {
			bubble.remove();
		}
	}
}
