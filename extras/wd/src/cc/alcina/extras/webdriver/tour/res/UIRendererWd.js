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
        let rect = relativeTo.getBoundingClientRect();
        let absRect = this.absRect(rect);
        let div = document.createElement("div");
        div.innerHTML = html;
        let content = div.firstElementChild;
        document.body.appendChild(content);
        let clientWidth = document.documentElement.clientWidth;
        let clientHeight = document.documentElement.clientHeight;
        content.style.top = absRect.top + "px";
        content.style.right = (clientWidth - absRect.left + popupInfo.relativeTo.offsetHorizontal) + "px";
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
            return document.evaluate(selector);
        } else {
            return document.querySelector(selector);
        }
    }

    remove(id) {
        document.getElementById(id).remove();
    }
}
