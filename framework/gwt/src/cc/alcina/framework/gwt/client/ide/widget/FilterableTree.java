/* 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package cc.alcina.framework.gwt.client.ide.widget;

import java.util.Stack;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;

import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.util.Callback;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.ide.node.TreeOrItem;
import cc.alcina.framework.gwt.client.ide.node.TreeOrItemTree;
import cc.alcina.framework.gwt.client.widget.TreeNodeWalker;
import cc.alcina.framework.gwt.client.widget.VisualFilterable;
import cc.alcina.framework.gwt.client.widget.VisualFilterable.VisualFilterableWithFirst;

/**
 * @author nick@alcina.cc
 * 
 */
public class FilterableTree extends Tree
        implements SelectionHandler<TreeItem>, VisualFilterableWithFirst {
    private static final int OTHER_KEY_DOWN = 63233;

    private static final int OTHER_KEY_LEFT = 63234;

    private static final int OTHER_KEY_RIGHT = 63235;

    private static final int OTHER_KEY_UP = 63232;

    private static int standardizeKeycode(int code) {
        switch (code) {
        case OTHER_KEY_DOWN:
            code = KeyCodes.KEY_DOWN;
            break;
        case OTHER_KEY_RIGHT:
            code = KeyCodes.KEY_RIGHT;
            break;
        case OTHER_KEY_UP:
            code = KeyCodes.KEY_UP;
            break;
        case OTHER_KEY_LEFT:
            code = KeyCodes.KEY_LEFT;
            break;
        }
        if (LocaleInfo.getCurrentLocale().isRTL()) {
            if (code == KeyCodes.KEY_RIGHT) {
                code = KeyCodes.KEY_LEFT;
            } else if (code == KeyCodes.KEY_LEFT) {
                code = KeyCodes.KEY_RIGHT;
            }
        }
        return code;
    }

    private boolean lastWasKeyDown;

    private TreeItem lastSelected;

    boolean lastKeyWasUp = false;

    boolean lastKeyWasDown = false;

    private CollectionFilter shouldExpandCallback;

    private String lastFilteredText = "";

    public FilterableTree() {
        super();
        addSelectionHandler(this);
    }

    public void collapseToFirstLevel() {
        TreeItem item = getSelectedItem();
        new TreeNodeWalker().walk(this, new Callback<TreeItem>() {
            public void apply(TreeItem target) {
                boolean open = target.getParentItem() == null;
                if (shouldExpandCallback != null
                        && !shouldExpandCallback.allow(target)) {
                    open = false;
                }
                target.setState(open);
            }
        });
        if (item != null) {
            setSelectedItem(item, false);
            ensureSelectedItemVisible();
        }
    }

    public void expandAll() {
        expandAll(99);
    }

    public void expandAll(int depth) {
        for (int i = 0; i < getItemCount(); i++) {
            expandAll(getItem(i), depth - 1);
        }
    }

    public void expandAllAsync(Callback<Void> callback, int depth) {
        Scheduler.get().scheduleIncremental(new ExpandCommand(callback, depth));
    }

    public boolean filter(String filterText) {
        this.lastFilteredText = filterText;
        boolean b = false;
        filterText = filterText == null ? null : filterText.toLowerCase();
        for (int i = 0; i < getItemCount(); i++) {
            TreeItem child = getItem(i);
            child.getElement().resolvedToPending();
        }
        for (int i = 0; i < getItemCount(); i++) {
            TreeItem child = getItem(i);
            if (child instanceof VisualFilterable) {
                VisualFilterable vf = (VisualFilterable) child;
                boolean match = vf.filter(filterText);
                b |= match;
                if (match && CommonUtils.isNotNullOrEmpty(filterText)) {
                    // allow for lazy rendering fuzziness
                    vf.filter(filterText);
                }
            }
        }
        if (getSelectedItem() != null && !getSelectedItem().isVisible()) {
            lastSelected = null;
            setSelectedItem(null);
        }
        resetKeyMemory();
        if (filterText.length() == 0) {
            collapseToFirstLevel();
        }
        if (getParent() instanceof ScrollPanel) {
            ((ScrollPanel) getParent()).scrollToTop();
            ((ScrollPanel) getParent()).scrollToLeft();
        }
        return b;
    }

    public String getLastFilteredText() {
        return this.lastFilteredText;
    }

    public TreeItem getNextNode(TreeItem item, boolean ignoreChildAxis,
            int direction) {
        if (item == null) {
            return null;
        }
        TreeOrItem parent = TreeOrItemTree.create(item).getParent();
        if (direction == 1) {
            if (!ignoreChildAxis && item.getState()
                    && item.getChildCount() > 0) {
                return item.getChild(0);
            }
            int childIndex = parent.getChildIndex(item);
            if (childIndex < parent.getChildCount() - 1) {
                return parent.getChild(childIndex + 1);
            }
            if (item.getParentItem() == null) {
                return null;
            }
            return getNextNode(item.getParentItem(), true, direction);
        } else {
            int childIndex = parent.getChildIndex(item);
            if (childIndex > 0) {
                return findDeepestOpenChild(parent.getChild(childIndex - 1));
            }
            return item.getParentItem();
        }
    }

    public CollectionFilter getShouldExpandCallback() {
        return this.shouldExpandCallback;
    }

    public void moveToFirst() {
        int visibleCount = 0;
        TreeItem visibleChild = null;
        for (int i = 0; i < getItemCount(); i++) {
            TreeItem child = getItem(i);
            if (child.isVisible()) {
                visibleCount++;
                if (visibleChild == null) {
                    visibleChild = child;
                }
            }
        }
        if (visibleChild != null) {
            if (visibleCount == 1) {
                selectVisibleChild(visibleChild);
            } else {
                setSelectedItem(visibleChild);
                setFocus(true);
            }
        }
    }

    @Override
    public void onBrowserEvent(Event event) {
        int eventType = DOM.eventGetType(event);
        switch (eventType) {
        case Event.ONKEYDOWN: {
            keyboardNavigation(event);
            lastWasKeyDown = true;
            break;
        }
        case Event.ONKEYPRESS: {
            if (!lastWasKeyDown) {
                keyboardNavigation(event);
            }
            lastWasKeyDown = false;
            break;
        }
        }
        super.onBrowserEvent(event);
    }

    public void onSelection(SelectionEvent<TreeItem> event) {
        TreeItem item = event.getSelectedItem();
        TreeItem lastSelCopy = lastSelected;
        lastSelected = item;
        if (item.isVisible() == false) {
            if (lastSelCopy != null) {
                if (lastKeyWasDown) {
                    lastKeyWasDown = false;
                    TreeItem visibleChild = findVisibleChild(lastSelCopy, 1);
                    if (visibleChild != null) {
                        setSelectedItem(visibleChild);
                    } else {
                        setSelectedItem(lastSelCopy);
                    }
                }
                if (lastKeyWasUp) {
                    lastKeyWasUp = false;
                    TreeItem visibleChild = findVisibleChild(lastSelCopy, -1);
                    if (visibleChild != null) {
                        setSelectedItem(visibleChild);
                    } else {
                        setSelectedItem(lastSelCopy);
                    }
                }
            }
        }
    }

    public void setShouldExpandCallback(CollectionFilter shouldExpandCallback) {
        this.shouldExpandCallback = shouldExpandCallback;
    }

    private void expandAll(TreeItem ti, int depth) {
        if (shouldExpandCallback != null && !shouldExpandCallback.allow(ti)) {
            return;
        }
        ti.setState(true);
        if (depth > 0) {
            for (int i = 0; i < ti.getChildCount(); i++) {
                expandAll(ti.getChild(i), depth - 1);
            }
        }
    }

    private TreeItem findDeepestOpenChild(TreeItem item) {
        if (!item.getState() || item.getChildCount() == 0) {
            return item;
        }
        return findDeepestOpenChild(item.getChild(item.getChildCount() - 1));
    }

    private TreeItem findVisibleChild(TreeItem item, int direction) {
        TreeItem parent = item.getParentItem();
        if (parent == null) {
            return null;
        }
        while (true) {
            item = getNextNode(item, false, direction);
            if (item == null) {
                return null;
            }
            if (item.isVisible()) {
                return item;
            }
        }
    }

    private void keyboardNavigation(Event event) {
        // Handle keyboard events if keyboard navigation is enabled
        resetKeyMemory();
        TreeItem curSelection = getSelectedItem();
        if (isKeyboardNavigationEnabled(curSelection)) {
            int code = DOM.eventGetKeyCode(event);
            switch (standardizeKeycode(code)) {
            case KeyCodes.KEY_UP: {
                lastKeyWasUp = true;
                break;
            }
            case KeyCodes.KEY_DOWN: {
                lastKeyWasDown = true;
                break;
            }
            default: {
                return;
            }
            }
        }
    }

    private void resetKeyMemory() {
        lastKeyWasUp = false;
        lastKeyWasDown = false;
    }

    private boolean selectVisibleChild(TreeItem item) {
        for (int i = 0; i < item.getChildCount(); i++) {
            TreeItem child = item.getChild(i);
            if (child.isVisible()) {
                return selectVisibleChild(child);
            }
        }
        setSelectedItem(item);
        setFocus(true);
        return true;
    }

    class ExpandCommand implements RepeatingCommand {
        private final Callback<Void> callback;

        private final int depth;

        Stack<TreeItem> items = new Stack<TreeItem>();

        int counter = 0;

        public ExpandCommand(Callback<Void> callback, int depth) {
            this.callback = callback;
            this.depth = depth;
            for (int i = 0; i < getItemCount(); i++) {
                items.push(getItem(i));
            }
        }

        @Override
        public boolean execute() {
            counter = 200;
            walk();
            if (counter > 0) {
                callback.apply(null);
                return false;
            }
            return true;
        }

        public void walk() {
            while (!items.isEmpty()) {
                TreeItem pop = items.pop();
                pop.setState(true);
                int pDepth = getDepth(pop);
                if (pDepth <= depth) {
                    for (int i = 0; i < pop.getChildCount(); i++) {
                        items.push(pop.getChild(i));
                    }
                }
                if (counter-- < 0) {
                    break;
                }
            }
        }

        private int getDepth(TreeItem item) {
            int pDepth = 1;
            while (((item = item.getParentItem())) != null) {
                pDepth++;
            }
            return pDepth;
        }
    }
}
