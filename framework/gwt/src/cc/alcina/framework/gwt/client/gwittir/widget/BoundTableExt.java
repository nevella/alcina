/*
 * BoundTable.java
 *
 * Created on July 24, 2007, 5:30 PM
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */
package cc.alcina.framework.gwt.client.gwittir.widget;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FocusListener;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HTMLTable;
import com.google.gwt.user.client.ui.HTMLTable.Cell;
import com.google.gwt.user.client.ui.HasFocus;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.ScrollListener;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.SourcesClickEvents;
import com.google.gwt.user.client.ui.SourcesTableEvents;
import com.google.gwt.user.client.ui.TableListener;
import com.google.gwt.user.client.ui.Widget;
import com.totsp.gwittir.client.beans.Binding;
import com.totsp.gwittir.client.beans.SourcesPropertyChangeEvents;
import com.totsp.gwittir.client.ui.BoundWidget;
import com.totsp.gwittir.client.ui.Button;
import com.totsp.gwittir.client.ui.Checkbox;
import com.totsp.gwittir.client.ui.Label;
import com.totsp.gwittir.client.ui.table.AbstractTableWidget;
import com.totsp.gwittir.client.ui.table.DataProvider;
import com.totsp.gwittir.client.ui.table.Field;
import com.totsp.gwittir.client.ui.table.HasChunks;
import com.totsp.gwittir.client.ui.table.SortableDataProvider;
import com.totsp.gwittir.client.util.ListSorter;

import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.AlcinaTopics;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.context.LooseContextInstance;
import cc.alcina.framework.gwt.client.dirndl.RenderContext;
import cc.alcina.framework.gwt.client.gwittir.HasBinding;
import cc.alcina.framework.gwt.client.gwittir.provider.CollectionDataProvider;
import cc.alcina.framework.gwt.client.gwittir.widget.EndRowButtonClickedEvent.EndRowButtonClickedHandler;
import cc.alcina.framework.gwt.client.gwittir.widget.EndRowButtonClickedEvent.HasEndRowClickedHandlers;
import cc.alcina.framework.gwt.client.objecttree.HasRenderContext;
import cc.alcina.framework.gwt.client.widget.FlowPanelClickable;
import cc.alcina.framework.gwt.client.widget.SpanPanel;

/**
 * This is an option-rich table for use with objects implementing the
 * SourcesPropertyChangeEvents interfaces.
 *
 * @author <a href="mailto:cooper@screaming-penguin.com">Robert "kebernet"
 *         Cooper</a>
 * @see com.totsp.gwittir.client.beans.SourcesPropertyChangeEvents
 * @author Nick Reddel
 *         <p>
 *         <b>Changes from gwittir.BoundTable</b>
 *         </p>
 *         <ul>
 *         <li>changed to protected: allRowsHandle, columns, masks, rowHandles,
 *         shiftDown, table, widgetCache , addRow(),
 *         <li>add: HANDLES_AS_CHECKBOXES, searchingMessage, noContentMessage,
 *         sortedColumn
 *         <li>method changes:
 *         <ul>
 *         <li>ordering in init() {works better with the logic, i think}
 *         <li>init(int) - support for handlesAsCheckboxes
 *         <li>renderAll() - support for searchingmessage, nocontentmessage,
 *         labels as HTML (see GridForm). <br>
 *         Also show hide navrow if <=1 chunk, and render with display=none
 *         until the end (keep browser from reflowing unnecessarily)
 *         <li>first(), next(), previous(), last() - ignore if this.inChunk ==
 *         true (otherwise we get errors on setchunk when people press things
 *         twice)
 *         <li>Visual hints (up/down arrows) to indicate column sort status
 *         </ul>
 *         </ul>
 */
@SuppressWarnings({ "unchecked", "deprecation" })
public class BoundTableExt extends AbstractTableWidget implements HasChunks,
		HasBinding, HasRenderContext, HasEndRowClickedHandlers {
	static Logger logger = LoggerFactory.getLogger(Binding.class);

	private static BoundTableExt activeTable = null;

	/**
	 * A placholder for no mask options (0)
	 */
	public static final int NONE_MASK = 0;

	/**
	 * Renderer the table inside a scroll panel.
	 *
	 * <p>
	 * If the table has a DataProvider, it will use the "Google Reader"
	 * get-next-chunk-on-max-scroll operation.
	 * </p>
	 */
	public static final int SCROLL_MASK = 1;

	/**
	 * Renderers a heading row on the table using the labels on the Column
	 * objects.
	 */
	public static final int HEADER_MASK = 2;

	/**
	 * Lets the user have multiple rows in the "selected" state at a time.
	 */
	public static final int MULTIROWSELECT_MASK = 4;

	/**
	 * Turns off row selection and styling.
	 */
	public static final int SELECT_ROW_MASK = 8;

	/**
	 * Turns off selected column stying.
	 */
	public static final int NO_SELECT_COL_MASK = 16;

	/**
	 * Turns off cell selection stying.
	 */
	public static final int NO_SELECT_CELL_MASK = 32;

	/**
	 * Tells the table to render a spacing row in between bound rows.
	 */
	public static final int SPACER_ROW_MASK = 64;

	/**
	 * If this table has a DataProvider AND it is not scrolling, this supresses
	 * the first, previous, next and last buttons at the bottom of the table.
	 */
	public static final int NO_NAV_ROW_MASK = 128;

	/**
	 * Enables sorting on the table when a header row is clicked.
	 *
	 * If this table has a DataProvider, it must be a SortableDataProvider for
	 * this to work.
	 */
	public static final int SORT_MASK = 256;

	/**
	 * Enables the click in widget insertion. Note: This will use the default
	 * widget type for the model object from the BoundWidgetTypeFactory
	 *
	 * @see com.totsp.gwittir.client.ui.util.BoundWidgetTypeFactory
	 */
	public static final int INSERT_WIDGET_MASK = 512;

	/**
	 * Determines whether buttons for row handles/selection should be present.
	 */
	public static final int ROW_HANDLE_MASK = 1024;

	public static final int MULTI_REQUIRES_SHIFT = 2048;

	public static final int HANDLES_AS_CHECKBOXES = 4096;

	public static final int END_ROW_BUTTON = 8192;

	private static final String DEFAULT_STYLE = "default";

	private static final String NAV_STYLE = "nav";

	private Binding topBinding;

	protected Button allRowsHandle;

	private Collection value;

	private DataProvider provider;

	protected FlexTable table;

	private HashMap<SourcesClickEvents, ClickListener> clickListeners = new HashMap<SourcesClickEvents, ClickListener>();

	private HashMap<HasFocus, FocusListener> focusListeners = new HashMap<HasFocus, FocusListener>();

	private HashMap<Integer, String> selectedRowStyles;

	protected List rowHandles; /* <Button> */

	private Map bindingCache = new HashMap();

	private Map externalKeyBindings = new HashMap();

	protected Map widgetCache = new HashMap();

	private ScrollPanel scroll;

	private String selectedCellLastStyle;

	private String selectedColLastStyle = BoundTableExt.DEFAULT_STYLE;

	private String selectedRowLastStyle = BoundTableExt.DEFAULT_STYLE;

	private boolean usesTHead;

	private Timer cleanUpCaches = new Timer() {
		@Override
		public void run() {
			if (value != null) {
				for (Iterator it = new ArrayList(widgetCache.keySet())
						.iterator(); it.hasNext();) {
					Object o = it.next();
					if (!value.contains(o)) {
						widgetCache.remove(o);
					}
				}
				for (Iterator it = new ArrayList(bindingCache.keySet())
						.iterator(); it.hasNext();) {
					Object o = it.next();
					if (!value.contains(o)) {
						bindingCache.remove(o);
					}
				}
			}
		}
	};

	private Widget base;

	private boolean[] ascending;

	protected Field[] columns;

	private boolean inChunk = false;

	protected boolean shiftDown = false;

	private int currentChunk = -1;

	private int lastScrollPosition;

	protected int masks;

	private int numberOfChunks;

	private int selectedCellRowLastIndex = -1;

	private int selectedColLastIndex = -1;

	private int selectedRowLastIndex = -1;

	private PropertyChangeListener collectionPropertyChangeListener;

	private List<SourcesPropertyChangeEvents> listenedToByCollectionChangeListener = new ArrayList<SourcesPropertyChangeEvents>();

	private RenderContext renderContext;

	private ClickHandler rowSelectHandler = new ClickHandler() {
		@Override
		public void onClick(ClickEvent event) {
			Object o = getObjectForEvent(event);
			setSelected(Collections.singletonList(o));
		}
	};

	private EventingSimplePanel esp;

	private String searchingMessage = "Searching...";

	private String noContentMessage = "No matching results found";

	private Collection lastRendered;

	private Iterator rowIterator = null;

	private int sortedColumn = -1;

	/**
	 * Creates a new instance of a table using a Collection as a data set.
	 *
	 * @param masks
	 *            int value containing the sum of the *_MASK options for the
	 *            table.
	 * @param cols
	 *            The Column objects for the table.
	 */
	public BoundTableExt(int masks, Field[] cols) {
		super();
		this.setColumns(cols);
		this.init(masks);
	}

	/**
	 * Creates a new instance of a table using a Collection as a data set.
	 *
	 * @param masks
	 *            int value containing the sum of the *_MASK options for the
	 *            table.
	 * @param cols
	 *            The Column objects for the table.
	 * @param value
	 *            A collection containing SourcesPropertyChangeEvents objects to
	 *            render in the table.
	 */
	public BoundTableExt(int masks, Field[] cols, Collection value) {
		super();
		this.setColumns(cols);
		this.value = value;
		this.init(masks);
	}

	/**
	 * Creates a new instance of BoundTable
	 *
	 * @param masks
	 *            int value containing the sum of the *_MASK options for the
	 *            table.
	 * @param cols
	 *            The Column objects for the table.
	 * @param provider
	 *            Instance of DataProvider to get chunked data from.
	 */
	public BoundTableExt(int masks, Field[] cols, DataProvider provider) {
		super();
		this.setColumns(cols);
		this.provider = provider;
		this.init(masks);
	}

	public BoundTableExt(int mask, List<Field> fields, DataProvider provider) {
		this(mask, (Field[]) fields.toArray(new Field[fields.size()]),
				provider);
	}

	/**
	 * Adds a colleciton of Bindables to the table
	 *
	 * @param c
	 *            A collection containing SourcesPropertyChangeEvents objects.
	 */
	public void add(Collection c) {
		for (Iterator it = c.iterator(); it.hasNext();) {
			this.add((SourcesPropertyChangeEvents) it.next());
		}
	}

	/**
	 * Adds a new SourcesPropertyChangeEvents object to the table.
	 *
	 * @param o
	 *            An object of type SourcesPropertyChangeEvents.
	 */
	public void add(SourcesPropertyChangeEvents o) {
		if (this.value.add(o)) {
			this.addRow(o);
		}
	}

	public HandlerRegistration addClickHandler(ClickHandler handler) {
		return this.table.addClickHandler(handler);
	}

	public void addCollectionPropertyChangeListener(
			PropertyChangeListener collectionPropertyChangeListener) {
		this.collectionPropertyChangeListener = collectionPropertyChangeListener;
	}

	@Override
	public HandlerRegistration
			addEndRowClickedHandler(EndRowButtonClickedHandler handler) {
		return addHandler(handler, EndRowButtonClickedEvent.getType());
	}

	protected void addRow(final SourcesPropertyChangeEvents o) {
		int row = table.getRowCount();
		if (((((masks & BoundTableExt.HEADER_MASK) > 0) && (row >= 2))
				|| (((masks & BoundTableExt.HEADER_MASK) == 0) && (row >= 1)))
				&& ((masks & BoundTableExt.SPACER_ROW_MASK) > 0)) {
			table.setWidget(row, 0, new Label(""));
			table.getFlexCellFormatter().setColSpan(row, 0,
					this.columns.length);
			table.getRowFormatter().setStyleName(row, "spacer");
			row++;
		}
		Binding bindingRow = new Binding();
		topBinding.getChildren().add(bindingRow);
		int count = topBinding.getChildren().size();
		final Widget handle;
		int startColumn = 0;
		if ((this.masks & BoundTableExt.ROW_HANDLE_MASK) > 0) {
			if ((this.masks & BoundTableExt.HANDLES_AS_CHECKBOXES) > 0) {
				handle = new Checkbox();
			} else {
				handle = new Button(
						(this.getActive() && (rowHandles.size() < 9))
								? Integer.toString(this.rowHandles.size() + 1)
								: " ");
			}
			handle.setStyleName("rowHandle");
			((HasFocus) handle).addFocusListener(new FocusListener() {
				@Override
				public void onFocus(Widget sender) {
					if (shiftDown) {
						return;
					}
					setActive(true);
					List newSelected = null;
					if ((masks & BoundTableExt.MULTIROWSELECT_MASK) > 0) {
						newSelected = new ArrayList(getSelected());
						if (newSelected.contains(o)) {
							newSelected.remove(o);
						} else {
							newSelected.add(o);
						}
					} else {
						newSelected = new ArrayList();
						newSelected.add(o);
					}
					setSelected(newSelected);
				}

				@Override
				public void onLostFocus(Widget sender) {
				}
			});
			((SourcesClickEvents) handle).addClickListener(new ClickListener() {
				@Override
				public void onClick(Widget sender) {
					setActive(true);
					List newSelected = null;
					if ((masks & BoundTableExt.MULTIROWSELECT_MASK) > 0) {
						newSelected = new ArrayList(getSelected());
						if (newSelected.contains(o)) {
							newSelected.remove(o);
						} else {
							newSelected.add(o);
						}
					} else {
						newSelected = new ArrayList();
						newSelected.add(o);
					}
					if (handle != null) {
						((HasFocus) handle).setFocus(true);
					}
					if (handle != null) {
						((HasFocus) handle).setFocus(true);
					}
					setSelected(newSelected);
				}
			});
			startColumn++;
			this.rowHandles.add(handle);
			this.table.setWidget(row, 0, handle);
		} else {
			handle = null;
		}
		for (int col = 0; col < this.columns.length; col++) {
			Widget widget = (Widget) createCellWidget(bindingRow, col, o);
			try {
				table.setWidget(row, col + startColumn, widget);
				if (widget instanceof HasFocus) {
					addSelectedFocusListener((HasFocus) widget,
							topBinding.getChildren().size() - 1,
							col + startColumn);
				}
				if (widget instanceof SourcesClickEvents) {
					addSelectedClickListener((SourcesClickEvents) widget,
							topBinding.getChildren().size() - 1,
							col + startColumn);
				}
				if (this.columns[col].getWidgetStyleName() != null) {
					widget.addStyleName(this.columns[col].getWidgetStyleName());
				}
				if (this.columns[col].getStyleName() != null) {
					table.getCellFormatter().setStyleName(row,
							col + startColumn,
							this.columns[col].getStyleName());
				}
			} catch (RuntimeException e) {
				logger.warn("Insertion exception", e);
			}
		}
		if ((this.masks & BoundTableExt.END_ROW_BUTTON) > 0) {
			EndRowButton endRowButton = new EndRowButton();
			table.setWidget(row, this.columns.length + startColumn,
					endRowButton);
			int f_row = row;
			endRowButton.addClickHandler(e -> {
				EndRowButtonClickedEvent.fire(BoundTableExt.this, f_row, o);
			});
		}
		if (collectionPropertyChangeListener != null) {
			o.addPropertyChangeListener(collectionPropertyChangeListener);
			listenedToByCollectionChangeListener.add(o);
		}
		boolean odd = (this.calculateRowToObjectOffset(Integer.valueOf(row))
				.intValue() % 2) != 0;
		this.table.getRowFormatter().setStyleName(row, odd ? "odd" : "even");
		bindingRow.setLeft();
	}

	private void addSelectedClickListener(final SourcesClickEvents widget,
			final int objectNumber, final int col) {
		ClickListener l = new ClickListener() {
			@Override
			public void onClick(Widget sender) {
				setActive(true);
				int row = calculateObjectToRowOffset(objectNumber);
				handleSelect(true, row, col);
			}
		};
		widget.addClickListener(l);
		clickListeners.put(widget, l);
	}

	private void addSelectedFocusListener(final HasFocus widget,
			final int objectNumber, final int col) {
		FocusListener l = new FocusListener() {
			@Override
			public void onFocus(Widget sender) {
				setActive(true);
				int row = calculateObjectToRowOffset(objectNumber);
				// GWT.log("Focus row: " + row + " object: " + objectNumber
				// + " col: " + col, null);
				// GWT.log("SelectedRowLastIndex " + selectedRowLastIndex,
				// null);
				handleSelect(row != selectedRowLastIndex, row, col);
			}

			@Override
			public void onLostFocus(Widget sender) {
			}
		};
		widget.addFocusListener(l);
		focusListeners.put(widget, l);
	}

	@Override
	public void addStyleName(String style) {
		this.base.addStyleName(style);
	}

	public void addTableListener(TableListener listener) {
		this.table.addTableListener(listener);
	}

	protected int calculateObjectToRowOffset(int row) {
		if ((masks & BoundTableExt.SPACER_ROW_MASK) > 0) {
			row += row;
		}
		if ((masks & BoundTableExt.HEADER_MASK) > 0) {
			row++;
		}
		// GWT.log( "Row before: "+ row, null);
		if ((masks & BoundTableExt.INSERT_WIDGET_MASK) > 0) {
			// if( (masks & BoundTable.MULTIROWSELECT_MASK) > 0){
			// row += this.selectedRowsBeforeObjectRow(row);
			// } else {
			row += this.selectedRowsBeforeRow(row);
			// }
		}
		// GWT.log( "Row after "+ row, null);
		return row;
	}

	protected Integer calculateRowToObjectOffset(Integer rowNumber) {
		int row = rowNumber.intValue();
		if ((masks & BoundTableExt.HEADER_MASK) > 0) {
			row -= 1;
		}
		if ((masks & BoundTableExt.SPACER_ROW_MASK) > 0) {
			row -= (row / 2);
		}
		// GWT.log( "Selected rows before row "+row+" "+
		// this.selectedRowsBeforeRow( row ), null );
		if (((masks & BoundTableExt.INSERT_WIDGET_MASK) > 0)
				&& ((masks & BoundTableExt.MULTIROWSELECT_MASK) > 0)) {
			// GWT.log( "At"+ row+
			// " Removing: "+this.selectedRowsBeforeRow(row), null );
			row -= this.selectedRowsBeforeRow(row);
		}
		// GWT.log( "Returning object instance index: "+row, null);
		return Integer.valueOf(row);
	}

	/**
	 * Clears the table and cleans up all bindings and listeners.
	 */
	public void clear() {
		for (SourcesPropertyChangeEvents spce : listenedToByCollectionChangeListener) {
			spce.removePropertyChangeListener(collectionPropertyChangeListener);
		}
		listenedToByCollectionChangeListener.clear();
		this.topBinding.unbind();
		this.topBinding.getChildren().clear();
		if (this.rowHandles != null) {
			this.rowHandles.clear();
		}
		for (Iterator it = this.focusListeners.entrySet().iterator(); it
				.hasNext();) {
			Entry entry = (Entry) it.next();
			((HasFocus) entry.getKey())
					.removeFocusListener((FocusListener) entry.getValue());
		}
		for (Iterator it = this.clickListeners.entrySet().iterator(); it
				.hasNext();) {
			Entry entry = (Entry) it.next();
			((SourcesClickEvents) entry.getKey())
					.removeClickListener((ClickListener) entry.getValue());
		}
		lastRendered = null;
		table.removeAllRows();
		// createTable();
		if (this.selectedRowStyles != null) {
			this.selectedRowStyles.clear();
		}
		this.clearSelectedCol();
		this.selectedCellLastStyle = BoundTableExt.DEFAULT_STYLE;
		this.selectedColLastIndex = -1;
		this.selectedColLastStyle = BoundTableExt.DEFAULT_STYLE;
		this.selectedRowLastIndex = -1;
		this.selectedRowLastStyle = BoundTableExt.DEFAULT_STYLE;
		this.selectedCellRowLastIndex = -1;
		this.cleanUpCaches.schedule(50);
	}

	private void clearSelectedCell() {
		if ((this.selectedColLastIndex != -1)
				&& (this.selectedCellRowLastIndex != -1)) {
			this.getCellFormatter().setStyleName(this.selectedCellRowLastIndex,
					this.selectedColLastIndex, this.selectedCellLastStyle);
		}
		this.selectedColLastIndex = -1;
		this.selectedCellRowLastIndex = -1;
	}

	private void clearSelectedCol() {
		if (this.selectedColLastIndex != -1) {
			this.getColumnFormatter().setStyleName(this.selectedColLastIndex,
					this.selectedColLastStyle);
		}
	}

	private void clearSelectedRows() {
		this.clearSelectedCol();
		this.clearSelectedCell();
		List old = this.getSelected();
		if ((this.masks & BoundTableExt.INSERT_WIDGET_MASK) > 0) {
			List removeRows = new ArrayList();
			if (this.selectedRowStyles != null) {
				removeRows.addAll(this.selectedRowStyles.keySet());
			} else {
				removeRows.add(Integer.valueOf(this.selectedRowLastIndex));
			}
			for (int i = removeRows.size() - 1; i >= 0; i--) {
				// GWT.log("Removing nested: " + removeRows.get(i), null);
				this.removeNestedWidget(
						((Integer) removeRows.get(i)).intValue());
			}
		}
		if ((this.masks & BoundTableExt.MULTIROWSELECT_MASK) > 0) {
			for (Iterator it = this.selectedRowStyles.entrySet().iterator(); it
					.hasNext();) {
				Entry entry = (Entry) it.next();
				int row = ((Integer) entry.getKey()).intValue();
				this.table.getRowFormatter().removeStyleName(row, "selected");
			}
			this.selectedRowStyles.clear();
		} else if ((this.masks & BoundTableExt.SELECT_ROW_MASK) != 0) {
			if (this.selectedRowLastIndex != -1) {
				this.table.getRowFormatter()
						.removeStyleName(this.selectedRowLastIndex, "selected");
				this.selectedRowLastStyle = BoundTableExt.DEFAULT_STYLE;
			}
		}
		this.selectedRowLastIndex = -1;
		this.selectedCellRowLastIndex = -1;
		this.changes.firePropertyChange("selected", old, this.getSelected());
	}

	protected BoundWidget createCellWidget(Binding rowBinding, int colIndex,
			SourcesPropertyChangeEvents target) {
		final BoundWidget widget;
		Field col = this.columns[colIndex];
		BoundWidget[] rowWidgets = (BoundWidget[]) widgetCache.get(target);
		if (rowWidgets == null) {
			rowWidgets = new BoundWidget[this.columns.length];
			widgetCache.put(target, rowWidgets);
		}
		if (rowWidgets[colIndex] != null) {
			widget = rowWidgets[colIndex];
			// BoundTable.LOG.log(Level.SPAM,
			// "Using cache widget for " + target + "." +
			// col.getPropertyName(), null);
		} else {
			if (col.getCellProvider() != null) {
				widget = col.getCellProvider().get();
			} else {
				widget = (BoundWidget) this.factory
						.getWidgetProvider(Reflections.at(target)
								.property(col.getPropertyName()).getType())
						.get();
				// TODO Figure out some way to make this read only.
			}
			rowWidgets[colIndex] = widget;
			// BoundTable.LOG.log(Level.SPAM,
			// "Creating widget for " + target + "." + col.getPropertyName(),
			// null);
		}
		Binding[] bindings = (Binding[]) this.bindingCache.get(target);
		if (bindings == null) {
			bindings = new Binding[this.columns.length];
			this.bindingCache.put(target, bindings);
		}
		if (bindings[colIndex] == null) {
			bindings[colIndex] = new Binding(widget, "value",
					col.getValidator(), col.getFeedback(), target,
					col.getPropertyName(), null, null);
			// BoundTable.LOG.log(Level.SPAM,
			// "Created binding " + bindings[colIndex], null);
		}
		widget.setModel(target);
		rowBinding.getChildren().add(bindings[colIndex]);
		return widget;
	}

	private Widget createNavWidget() {
		Grid p = new Grid(1, 5);
		p.setStyleName(BoundTableExt.NAV_STYLE);
		Button b = new Button("<<", new ClickListener() {
			@Override
			public void onClick(Widget sender) {
				first();
			}
		});
		b.setStyleName(BoundTableExt.NAV_STYLE);
		if (this.getCurrentChunk() == 0) {
			b.setEnabled(false);
		}
		p.setWidget(0, 0, b);
		b = new Button("<", new ClickListener() {
			@Override
			public void onClick(Widget sender) {
				previous();
			}
		});
		b.setStyleName(BoundTableExt.NAV_STYLE);
		if (this.getCurrentChunk() == 0) {
			b.setEnabled(false);
		}
		p.setWidget(0, 1, b);
		b = new Button(">", new ClickListener() {
			@Override
			public void onClick(Widget sender) {
				next();
			}
		});
		b.setStyleName(BoundTableExt.NAV_STYLE);
		if (this.getCurrentChunk() == (this.getNumberOfChunks() - 1)) {
			b.setEnabled(false);
		}
		Label l = new Label((this.getCurrentChunk() + 1) + " / "
				+ this.getNumberOfChunks());
		p.setWidget(0, 2, l);
		p.getCellFormatter().setHorizontalAlignment(0, 2,
				HasHorizontalAlignment.ALIGN_CENTER);
		p.setWidget(0, 3, b);
		b = new Button(">>", new ClickListener() {
			@Override
			public void onClick(Widget sender) {
				last();
			}
		});
		b.setStyleName(BoundTableExt.NAV_STYLE);
		if (this.getCurrentChunk() == (this.getNumberOfChunks() - 1)) {
			b.setEnabled(false);
		}
		p.setWidget(0, 4, b);
		return p;
	}

	private void createTable() {
		String oldStyleNames = "";
		if (this.table != null) {
			oldStyleNames = this.table.getStyleName();
		}
		this.table = createTableImpl();
		if ((this.masks & BoundTableExt.SELECT_ROW_MASK) > 0) {
			this.table.addClickHandler(rowSelectHandler);
		}
		this.table.setCellPadding(0);
		this.table.setCellSpacing(0);
		table.addTableListener(new TableListener() {
			@Override
			public void onCellClicked(SourcesTableEvents sender, int row,
					int cell) {
				setActive(true);
				int startColumn = ((masks & BoundTableExt.ROW_HANDLE_MASK) > 0)
						? 1
						: 0;
				if (startColumn == 0) {
					handleSelect(true, row, cell);
				}
				if (((masks & BoundTableExt.SORT_MASK) > 0)
						&& ((masks & BoundTableExt.HEADER_MASK) > 0)
						&& (row == 0) && !(BoundTableExt.this.value == null
								|| BoundTableExt.this.value.isEmpty())) {
					sortColumn(cell - startColumn);
				}
			}
		});
		this.base = this.table;
		this.setStyleName("gwittir-BoundTable", true);
		if (Ax.notBlank(oldStyleNames)) {
			this.setStyleName(oldStyleNames);
		}
		esp.setWidget(this.table);
	}

	protected FlexTable createTableImpl() {
		return new FlexTable();
	}

	private void ensureBound(boolean bound) {
		if (bound) {
			if (!topBinding.isBound()) {
				topBinding.bind();
			}
		} else {
			if (topBinding.isBound()) {
				topBinding.unbind();
			}
		}
	}

	/**
	 * Causes the table to go to the first chunk of data, if a data provider is
	 * used.
	 */
	public void first() {
		if (this.inChunk) {
			return;
		}
		this.currentChunk = 0;
		this.inChunk = true;
		this.provider.getChunk(this, this.getCurrentChunk());
	}

	public boolean getActive() {
		return BoundTableExt.activeTable == this;
	}

	/**
	 * Returns the Binding object used by this table.
	 *
	 * @return The Binding object for this table.
	 */
	@Override
	public Binding getBinding() {
		return this.topBinding;
	}

	public int getCellCount(int row) {
		return this.table.getCellCount(row);
	}

	public HTMLTable.CellFormatter getCellFormatter() {
		return this.table.getCellFormatter();
	}

	public int getCellPadding() {
		return this.table.getCellPadding();
	}

	public int getCellSpacing() {
		return this.table.getCellSpacing();
	}

	public HTMLTable.ColumnFormatter getColumnFormatter() {
		return this.table.getColumnFormatter();
	}

	/**
	 * Returns the Columns used in this table.
	 *
	 * @return Column[] used for rendering this table.
	 */
	public Field[] getColumns() {
		Field[] ret = new Field[this.columns.length];
		for (int i = 0; i < ret.length; i++) {
			ret[i] = this.columns[i];
		}
		return columns;
	}

	/**
	 * Returns the current fetched chunk from the data provider.
	 *
	 * @return int index of the current chunk.
	 */
	public int getCurrentChunk() {
		return currentChunk;
	}

	public FlexTable.FlexCellFormatter getFlexCellFormatter() {
		return this.table.getFlexCellFormatter();
	}

	public String getHTML(int row, int column) {
		return this.table.getHTML(row, column);
	}

	public String getNoContentMessage() {
		return noContentMessage;
	}

	/**
	 * Returns the number of available chunks (passed in from the DataProvider)
	 *
	 * @return int number of chunks available from the DataProvider
	 */
	public int getNumberOfChunks() {
		return numberOfChunks;
	}

	public Object getObjectForEvent(ClickEvent event) {
		Cell cell = table.getCellForEvent(event);
		if (cell != null) {
			Iterator itr = value.iterator();
			for (int i = 1; i < cell.getRowIndex() && itr.hasNext(); i++) {
				itr.next();
			}
			return itr.hasNext() ? itr.next() : null;
		}
		return null;
	}

	@Override
	public LooseContextInstance getRenderContext() {
		return this.renderContext;
	}

	protected native Element getRow(Element elem, int row)/*-{
    return elem.rows[row];
	}-*/;

	public int getRowCount() {
		return this.table.getRowCount();
	}

	public HTMLTable.RowFormatter getRowFormatter() {
		return this.table.getRowFormatter();
	}

	/**
	 * Returns a List containing the current selected row objects.
	 *
	 * @return List of Bindables from the selected rows.
	 */
	public List getSelected() {
		ArrayList selected = new ArrayList();
		HashSet realIndexes = new HashSet();
		if (this.selectedRowStyles != null) {
			for (Iterator it = this.selectedRowStyles.keySet().iterator(); it
					.hasNext();) {
				realIndexes
						.add(calculateRowToObjectOffset((Integer) it.next()));
			}
		} else if (this.selectedRowLastIndex != -1) {
			realIndexes.add(calculateRowToObjectOffset(
					Integer.valueOf(this.selectedRowLastIndex)));
		}
		int i = 0;
		for (Iterator it = this.value.iterator(); it.hasNext(); i++) {
			if (realIndexes.contains(Integer.valueOf(i))) {
				selected.add(it.next());
			} else {
				it.next();
			}
		}
		return selected;
	}

	public int getSelectedRowIndex() {
		return this.selectedRowLastIndex;
	}

	@Override
	public String getStyleName() {
		return this.base.getStyleName();
	}

	@Override
	public String getTitle() {
		return this.table.getTitle();
	}

	@Override
	public Object getValue() {
		return value;
	}

	public BoundWidget getWidget(int row, int col) {
		return (BoundWidget) this.table.getWidget(row, col);
	}

	public void handleKeyupEvent(KeyUpEvent event) {
		int delta = 0;
		if (event.getNativeKeyCode() == KeyCodes.KEY_UP) {
			delta = -1;
		}
		if (event.getNativeKeyCode() == KeyCodes.KEY_DOWN) {
			delta = 1;
		}
		if (delta != 0) {
			boolean hasHeader = (masks & BoundTableExt.HEADER_MASK) > 0;
			int row = selectedRowLastIndex + delta;
			if (row == -2) {
				row = -1;
			}
			row = row % value.size();
			setSelectedRow(row);
			int row2 = Math.min(value.size() - 1, row + 4);
			final Element row3 = getRow(table.getElement(), row2);
			event.stopPropagation();
			event.preventDefault();
		}
	}

	private void handleSelect(boolean toggleRow, int row, int col) {
		int calcRow = row;
		// GWT.log( "Toggle row "+ toggleRow, null );
		// GWT.log( " ON "+row+", "+col, new RuntimeException() );
		if ((this.masks & BoundTableExt.INSERT_WIDGET_MASK) > 0) {
			if (((this.selectedRowStyles == null)
					&& (this.selectedRowLastIndex != -1)
					&& (this.selectedRowLastIndex == (row - 1)))
					|| ((this.selectedRowStyles != null)
							&& this.selectedRowStyles
									.containsKey(Integer.valueOf(row - 1)))) {
				return;
			}
			calcRow = row - this.selectedRowsBeforeRow(row);
		}
		if (calcRow < 0) {
			throw new RuntimeException("Row base is negative!");
		}
		if ((((masks & BoundTableExt.SPACER_ROW_MASK) == 0)
				&& ((((masks & BoundTableExt.HEADER_MASK) > 0) && (calcRow > 0))
						|| ((masks & BoundTableExt.HEADER_MASK) == 0)))
				|| (((masks & BoundTableExt.HEADER_MASK) > 0)
						& ((calcRow % 2) != 0))
				|| (((masks & BoundTableExt.HEADER_MASK) == 0)
						&& ((calcRow % 2) != 1))) {
			// GWT.log( "Inside" , null);
			if ((toggleRow
					&& (((masks & BoundTableExt.MULTIROWSELECT_MASK) == 0)
							&& (row != this.selectedCellRowLastIndex)))
					|| (((masks & BoundTableExt.MULTIROWSELECT_MASK) > 0)
							&& toggleRow)) {
				// if( toggleRow || (masks & BoundTable.MULTIROWSELECT_MASK) ==
				// 0){
				row = setSelectedRow(row);
			}
			setSelectedCell(row, col);
			setSelectedCol(col);
			this.selectedCellRowLastIndex = row;
		}
	}

	/**
	 * Method called by the DataProvider to initialize the first chunk and pass
	 * in the to total number of chunks available.
	 *
	 * @param c
	 *            Data for Chunk index 0
	 * @param numberOfChunks
	 *            The total number of available chunks of data.
	 */
	@Override
	public void init(Collection c, int numberOfChunks) {
		this.numberOfChunks = numberOfChunks;
		this.currentChunk = 0;
		this.inChunk = false;
		this.setValue(c);
	}

	private void init(int masksValue) {
		renderContext = RenderContext.get().snapshot();
		// GWT.log( "Init "+ +masksValue + " :: "+((masksValue &
		// BoundTable.MULTI_REQUIRES_SHIFT) > 0), null);
		final BoundTableExt instance = this;
		this.topBinding = new Binding();
		this.masks = masksValue;
		if (((this.masks & BoundTableExt.SORT_MASK) > 0)
				&& (this.columns != null)) {
			this.ascending = new boolean[this.columns.length];
		}
		if ((this.masks & BoundTableExt.MULTIROWSELECT_MASK) > 0) {
			this.selectedRowStyles = new HashMap();
		}
		if (((this.masks & BoundTableExt.ROW_HANDLE_MASK) > 0)
				&& ((this.masks & BoundTableExt.MULTIROWSELECT_MASK) > 0)) {
			this.allRowsHandle = new Button("  ", new ClickListener() {
				@Override
				public void onClick(Widget sender) {
					if ((getSelected() != null)
							&& (getSelected().size() == 0)) {
						setSelected(new ArrayList((Collection) getValue()));
					} else {
						setSelected(new ArrayList());
					}
				}
			});
			this.allRowsHandle.setStyleName("rowHandle");
			this.allRowsHandle.setHeight("100%");
			this.allRowsHandle.setWidth("100%");
			if ((this.masks & BoundTableExt.MULTIROWSELECT_MASK) == 0) {
				this.allRowsHandle.setEnabled(false);
			}
		}
		if ((this.masks & BoundTableExt.ROW_HANDLE_MASK) > 0) {
			this.rowHandles = new ArrayList();
		}
		esp = new EventingSimplePanel();
		createTable();
		if ((masks & BoundTableExt.SCROLL_MASK) > 0) {
			if ("".isEmpty()) {
				throw new UnsupportedOperationException();
			}
			this.scroll = new ScrollPanel();
			this.scroll.setWidget(table);
			super.initWidget(esp);
			scroll.addScrollListener(new ScrollListener() {
				@Override
				public void onScroll(Widget widget, int scrollLeft,
						int scrollTop) {
					// GWT.log("HasProvider: " + (provider != null), null);
					if ((provider != null) && (inChunk == false)
							&& (scrollTop >= (table.getOffsetHeight()
									- scroll.getOffsetHeight()))) {
						// GWT.log("Scroll Event fired. ", null);
						lastScrollPosition = scrollTop - 1;
						next();
					}
				}
			});
		} else {
			super.initWidget(esp);
		}
		this.value = (this.value == null) ? new ArrayList() : this.value;
		this.columns = (this.columns == null) ? new Field[0] : this.columns;
		this.setStyleName("gwittir-BoundTable");
		if ((masks & BoundTableExt.HANDLES_AS_CHECKBOXES) > 0) {
			this.addStyleName("handles-as-checkboxes");
		}
		if ((this.provider != null) && (this.getCurrentChunk() == -1)) {
			this.inChunk = true;
			this.provider.init(this);
		}
		this.addPropertyChangeListener("selected",
				new PropertyChangeListener() {
					@Override
					public void propertyChange(
							PropertyChangeEvent propertyChangeEvent) {
						if (getAction() != null) {
							getAction().execute(instance);
						}
					}
				});
		this.addPropertyChangeListener("active", new PropertyChangeListener() {
			@Override
			public void
					propertyChange(PropertyChangeEvent propertyChangeEvent) {
				boolean newActive = ((Boolean) propertyChangeEvent
						.getNewValue()).booleanValue();
				if ((masks & BoundTableExt.ROW_HANDLE_MASK) > 0
						&& (masks & BoundTableExt.HANDLES_AS_CHECKBOXES) == 0) {
					for (int i = 0; i < rowHandles.size(); i++) {
						((Button) rowHandles.get(i))
								.setText((newActive && (i <= 8))
										? Integer.toString(i + 1)
										: " ");
					}
				}
			}
		});
	}

	private void insertNestedWidget(int row) {
		// GWT.log( "Inserting nested for row "+row, null);
		Integer realIndex = this
				.calculateRowToObjectOffset(Integer.valueOf(row));
		// GWT.log( "RealIndex: "+ realIndex, null );
		int i = 0;
		SourcesPropertyChangeEvents o = null;
		for (Iterator it = this.topBinding.getChildren().iterator(); it
				.hasNext(); i++) {
			if (realIndex.intValue() == i) {
				o = ((Binding) ((Binding) it.next()).getChildren().get(0))
						.getRight().object;
				break;
			} else {
				it.next();
			}
		}
		BoundWidget widget = (BoundWidget) this.factory
				.getWidgetProvider(o.getClass()).get();
		widget.setModel(o);
		this.table.insertRow(row + 1);
		this.table.setWidget(row + 1, 0, (Widget) widget);
		this.table.getFlexCellFormatter().setColSpan(row + 1, 0,
				this.columns.length + 1);
		this.table.getCellFormatter().setStyleName(row + 1, 0, "expanded");
		this.modifySelectedIndexes(row, +1);
	}

	public boolean isUsesTHead() {
		return this.usesTHead;
	}

	/**
	 * Causes the table to render the last chunk of data.
	 */
	public void last() {
		if (this.inChunk) {
			return;
		}
		if ((this.numberOfChunks - 1) >= 0) {
			this.currentChunk = this.numberOfChunks - 1;
			this.inChunk = true;
			this.provider.getChunk(this, currentChunk);
		}
	}

	private void modifySelectedIndexes(int fromRow, int modifier) {
		if (this.selectedRowLastIndex > fromRow) {
			this.selectedRowLastIndex += modifier;
		}
		if (this.selectedCellRowLastIndex > fromRow) {
			this.selectedCellRowLastIndex += modifier;
		}
		if (this.selectedRowStyles == null) {
			return;
		}
		HashMap newSelectedRowStyles = new HashMap();
		for (Iterator it = this.selectedRowStyles.entrySet().iterator(); it
				.hasNext();) {
			Entry entry = (Entry) it.next();
			Integer entryRow = (Integer) entry.getKey();
			if (entryRow.intValue() > fromRow) {
				newSelectedRowStyles.put(
						Integer.valueOf(entryRow.intValue() + modifier),
						entry.getValue());
			} else {
				newSelectedRowStyles.put(entryRow, entry.getValue());
			}
		}
		this.selectedRowStyles = newSelectedRowStyles;
	}

	/**
	 * Causes the table to render the next chunk of data.
	 */
	public void next() {
		if (this.inChunk) {
			return;
		}
		if ((this.currentChunk + 1) < this.numberOfChunks) {
			this.inChunk = true;
			this.provider.getChunk(this, ++currentChunk);
		}
	}

	@Override
	protected void onAttach() {
		super.onAttach();
		this.renderAll();
		ensureBound(true);
	}

	@Override
	protected void onDetach() {
		ensureBound(false);
		super.onDetach();
		this.setActive(false);
	}

	/**
	 * Causes teh table to render the previous chunk of data.
	 */
	public void previous() {
		if (this.inChunk) {
			return;
		}
		if ((this.getCurrentChunk() - 1) >= 0) {
			inChunk = true;
			this.provider.getChunk(this, --currentChunk);
		}
	}

	private void removeNestedWidget(int row) {
		this.modifySelectedIndexes(row, -1);
		this.table.removeRow(row + 1);
	}

	protected void renderAll() {
		if (value != null && value == lastRendered) {
			return;
		}
		if (columns == null) {
			return;
		}
		if (this.topBinding == null) { // Used to check that init() has fired.
			return;
		}
		if (!renderCheck()) {
			return;
		}
		try {
			RenderContext.get().push();
			RenderContext.get().putSnapshotProperties(renderContext);
			renderTop();
			renderRows(Integer.MAX_VALUE);
			renderBottom();
			lastRendered = value;
		} finally {
			RenderContext.get().pop();
		}
	}

	public void renderBottom() {
		if ((this.provider != null)
				&& ((this.masks & BoundTableExt.SCROLL_MASK) == 0)
				&& ((this.masks & BoundTableExt.NO_NAV_ROW_MASK) == 0)
				&& numberOfChunks > 1) {
			int row = this.table.getRowCount();
			this.table.setWidget(row, 0, this.createNavWidget());
			this.table.getFlexCellFormatter().setColSpan(row, 0,
					this.columns.length);
			table.getCellFormatter().setHorizontalAlignment(row, 0,
					HasHorizontalAlignment.ALIGN_CENTER);
		}
		setVisible(true);
	}

	public boolean renderCheck() {
		try {
			RenderContext.get().push();
			RenderContext.get().putSnapshotProperties(renderContext);
			if (this.value == null || this.value.isEmpty()) {
				this.clear();
				HTML l = new HTML(inChunk
						&& !(this.provider instanceof CollectionDataProvider)
								? searchingMessage
								: noContentMessage);
				l.setStyleName("no-content");
				this.table.setWidget(0, 0, l);
				return false;
			}
			return true;
		} finally {
			RenderContext.get().pop();
		}
	}

	private void renderRows(int numberOfRows) {
		for (; rowIterator != null && rowIterator.hasNext()
				&& --numberOfRows != 0;) {
			this.addRow((SourcesPropertyChangeEvents) rowIterator.next());
		}
	}

	public void renderTop() {
		setVisible(false);
		this.clear();
		int startColumn = 0;
		if ((this.masks & BoundTableExt.ROW_HANDLE_MASK) > 0) {
			this.table.setWidget(0, 0, this.allRowsHandle);
			startColumn = 1;
		}
		if ((this.masks & BoundTableExt.HEADER_MASK) > 0) {
			for (int i = 0; i < this.columns.length; i++) {
				this.table.setWidget(0, i + startColumn,
						new HTML(this.columns[i].getLabel()));
			}
			if (this.provider instanceof SortableDataProvider
					&& (this.masks & BoundTableExt.SORT_MASK) > 0) {
				SortableDataProvider sdp = (SortableDataProvider) this.provider;
				String[] sortableProperties = sdp.getSortableProperties();
				for (int i = 0; (i < sortableProperties.length); i++) {
					for (int index = 0; index < this.columns.length; index++) {
						if (sortableProperties[i].equals(
								this.columns[index].getPropertyName())) {
							this.table.getCellFormatter().addStyleName(0,
									index + startColumn, "sortable");
						}
					}
				}
			}
			this.table.getRowFormatter().setStyleName(0, "header");
		}
		if ((this.masks & BoundTableExt.END_ROW_BUTTON) > 0) {
			this.table.setWidget(0, this.columns.length + startColumn,
					new HTML("\u00A0"));
		}
		if (sortedColumn != -1) {
			if ((this.masks & BoundTableExt.HEADER_MASK) > 0) {
				table.getCellFormatter().addStyleName(0,
						sortedColumn + startColumn,
						this.ascending[sortedColumn] ? "ascending"
								: "descending");
			}
		}
		rowIterator = this.value == null ? null : this.value.iterator();
	}

	private int selectedRowsBeforeRow(int row) {
		// GWT.log( "=======Selected rows before "+row, null);
		// GWT.log( "=======lastRow "+this.selectedRowLastIndex, null );
		if (this.selectedRowStyles == null) {
			return ((this.selectedRowLastIndex == -1)
					|| (this.selectedRowLastIndex >= row)) ? 0 : 1;
		}
		int count = 0;
		for (Iterator it = this.selectedRowStyles.keySet().iterator(); it
				.hasNext();) {
			if (((Integer) it.next()).intValue() < row) {
				count++;
			}
		}
		return count;
	}

	public void setActive(boolean active) {
		if ((BoundTableExt.activeTable == this) & !active) {
			BoundTableExt.activeTable = null;
			this.changes.firePropertyChange("active", true, false);
		} else if (BoundTableExt.activeTable != this) {
			if (BoundTableExt.activeTable != null) {
				BoundTableExt.activeTable.setActive(false);
			}
			BoundTableExt.activeTable = this;
			this.changes.firePropertyChange("active", false, true);
		}
	}

	public void setBorderWidth(int width) {
		this.table.setBorderWidth(width);
	}

	public void setCellPadding(int padding) {
		this.table.setCellPadding(padding);
	}

	public void setCellSpacing(int spacing) {
		this.table.setCellSpacing(spacing);
	}

	/**
	 * Called by the DataProvider to pass in a requested chunk of data. THIS
	 * METHOD MUST BE CALLED ASYNCRONOUSLY.
	 *
	 * @param c
	 *            The next requested chunk of SourcesPropertyChangeEvents
	 *            objects.
	 */
	@Override
	public void setChunk(Collection c) {
		if (!this.inChunk) {
			throw new RuntimeException(
					"This method MUST becalled asyncronously!");
			// edge - if a user presses the 'next' button twice
			// now handled with an inchunk check for all nav actions
		}
		if ((masks & BoundTableExt.SCROLL_MASK) > 0) {
			this.add(c);
		} else {
			this.setValue(c);
		}
		if (((masks & BoundTableExt.SCROLL_MASK) > 0) && (this.scroll
				.getVerticalScrollPosition() >= this.lastScrollPosition)) {
			this.scroll.setVerticalScrollPosition(this.lastScrollPosition);
		}
		this.inChunk = false;
	}

	/**
	 * Sets Column[] object for use on the table. Note, this will foce a re-init
	 * of the table.
	 *
	 * @param columns
	 *            Column[] to use to render the table.
	 */
	public void setColumns(Field[] columns) {
		this.columns = new Field[columns.length];
		for (int i = 0; i < columns.length; i++) {
			this.columns[i] = columns[i];
		}
		if ((this.masks & BoundTableExt.SORT_MASK) > 0) {
			this.ascending = new boolean[this.columns.length];
		}
		this.renderAll();
	}

	public void setDataProvider(DataProvider provider) {
		this.provider = provider;
		this.inChunk = true;
		this.provider.init(this);
	}

	@Override
	public void setHeight(String height) {
		this.base.setHeight(height);
	}

	public void setNoContentMessage(String noContentMessage) {
		this.noContentMessage = noContentMessage;
		// TODO - searching indicator
	}

	@Override
	public void setPixelSize(int width, int height) {
		this.table.setPixelSize(width, height);
	}

	/**
	 * Sets the indicated items in the list to "selected" state.
	 *
	 * @param selected
	 *            A List of Bindables to set as the Selected value.
	 */
	public void setSelected(List selected) {
		int i = 0;
		this.clearSelectedRows();
		for (Iterator it = this.topBinding.getChildren().iterator(); it
				.hasNext(); i++) {
			SourcesPropertyChangeEvents b = ((Binding) ((Binding) it.next())
					.getChildren().get(0)).getRight().object;
			if (selected.contains(b)) {
				this.setSelectedRow(calculateObjectToRowOffset(i));
				if (this.table.getWidget(calculateObjectToRowOffset(i),
						0) instanceof HasFocus) {
					((HasFocus) this.table
							.getWidget(calculateObjectToRowOffset(i), 0))
									.setFocus(true);
				}
			}
		}
	}

	private void setSelectedCell(int row, int col) {
		if ((((this.masks & BoundTableExt.HEADER_MASK) > 0) && (row == 0))
				|| ((this.masks & BoundTableExt.NO_SELECT_CELL_MASK) > 0)) {
			return;
		}
		if ((this.selectedColLastIndex != -1)
				&& (this.selectedCellRowLastIndex != -1)) {
			this.getCellFormatter().setStyleName(this.selectedCellRowLastIndex,
					this.selectedColLastIndex, this.selectedCellLastStyle);
		}
		this.selectedCellLastStyle = table.getCellFormatter().getStyleName(row,
				col);
		if ((this.selectedCellLastStyle == null)
				|| (this.selectedCellLastStyle.length() == 0)) {
			this.selectedCellLastStyle = BoundTableExt.DEFAULT_STYLE;
		}
		table.getCellFormatter().setStyleName(row, col, "selected");
	}

	private void setSelectedCol(int col) {
		clearSelectedCol();
		this.selectedColLastIndex = col;
		if ((this.masks & BoundTableExt.NO_SELECT_COL_MASK) == 0) {
			this.selectedColLastStyle = table.getColumnFormatter()
					.getStyleName(col);
			if ((this.selectedColLastStyle == null)
					|| (this.selectedColLastStyle.length() == 0)) {
				this.selectedColLastStyle = BoundTableExt.DEFAULT_STYLE;
			}
			table.getColumnFormatter().setStyleName(col, "selected");
		}
	}

	private int setSelectedRow(int row) {
		if (((this.masks & BoundTableExt.HEADER_MASK) > 0) && (row == 0)) {
			return row;
		}
		List old = this.getSelected();
		if ((this.masks & BoundTableExt.MULTIROWSELECT_MASK) > 0) {
			if ((((masks
					& BoundTableExt.MULTI_REQUIRES_SHIFT) > 0) == shiftDown)) {
				// TOGGLE ROW.
				if (this.selectedRowStyles.containsKey(Integer.valueOf(row))) {
					// Handle Widget remove on Multirow
					this.getRowFormatter().setStyleName(row,
							(String) this.selectedRowStyles
									.remove(Integer.valueOf(row)));
					if ((this.masks & BoundTableExt.INSERT_WIDGET_MASK) > 0) {
						this.removeNestedWidget(row);
					}
				} else {
					String lastStyle = table.getRowFormatter()
							.getStyleName(row);
					lastStyle = ((lastStyle == null)
							|| (lastStyle.length() == 0))
									? BoundTableExt.DEFAULT_STYLE
									: lastStyle;
					this.selectedRowStyles.put(Integer.valueOf(row), lastStyle);
					this.getRowFormatter().addStyleName(row, "selected");
					if ((this.masks & BoundTableExt.INSERT_WIDGET_MASK) > 0) {
						this.insertNestedWidget(row);
					}
				}
			} else {
				// Set selected and toggle all others
				GWT.log("clearing all rows", null);
				for (Integer i : (Integer[]) this.selectedRowStyles.keySet()
						.toArray(new Integer[this.selectedRowStyles.keySet()
								.size()])) {
					if (i == row) {
						continue;
					}
					GWT.log("Clearing " + i, null);
					// Handle Widget remove on Multirow
					this.getRowFormatter().removeStyleName(i, "selected");
					if ((this.masks & BoundTableExt.INSERT_WIDGET_MASK) > 0) {
						this.removeNestedWidget(row);
					}
				}
				if (!this.selectedRowStyles.containsKey(row)) {
					String lastStyle = table.getRowFormatter()
							.getStyleName(row);
					lastStyle = ((lastStyle == null)
							|| (lastStyle.length() == 0))
									? BoundTableExt.DEFAULT_STYLE
									: lastStyle;
					this.selectedRowStyles.put(row, lastStyle);
					this.getRowFormatter().addStyleName(row, "selected");
					if ((this.masks & BoundTableExt.INSERT_WIDGET_MASK) > 0) {
						this.insertNestedWidget(row);
					}
				}
			}
		} else {
			if ((this.masks & BoundTableExt.SELECT_ROW_MASK) != 0) {
				if (this.selectedRowLastIndex != -1) {
					this.getRowFormatter().removeStyleName(
							this.selectedRowLastIndex, "selected");
					if ((this.masks & BoundTableExt.INSERT_WIDGET_MASK) > 0) {
						this.removeNestedWidget(this.selectedRowLastIndex);
						if (this.selectedRowLastIndex < row) {
							row--;
						}
					}
				}
				String currentStyle = table.getRowFormatter().getStyleName(row);
				if ((currentStyle == null)
						|| !currentStyle.equals("selected")) {
					this.selectedRowLastStyle = currentStyle;
				}
				if ((this.selectedRowLastStyle == null)
						|| (this.selectedRowLastStyle.length() == 0)) {
					this.selectedRowLastStyle = BoundTableExt.DEFAULT_STYLE;
				}
				table.getRowFormatter().addStyleName(row, "selected");
				if ((this.masks & BoundTableExt.INSERT_WIDGET_MASK) > 0) {
					this.insertNestedWidget(row);
				}
			}
		}
		this.selectedRowLastIndex = (this.selectedRowLastIndex == row) ? (-1)
				: row;
		this.changes.firePropertyChange("selected", old, this.getSelected());
		return row;
	}

	@Override
	public void setSize(String width, String height) {
		this.base.setSize(width, height);
	}

	@Override
	public void setStyleName(String style) {
		this.base.setStyleName(style);
	}

	@Override
	public void setStyleName(String style, boolean add) {
		this.base.setStyleName(style, add);
	}

	public void setUsesTHead(boolean usesTHead) {
		this.usesTHead = usesTHead;
		table.setUsesTHead(usesTHead);
	}

	@Override
	public void setValue(Object value) {
		Collection old = this.value;
		this.value = (Collection) value;
		this.changes.firePropertyChange("value", old, this.value);
		boolean active = this.getActive();
		this.setActive(false);
		this.renderAll();
		this.setActive(active);
	}

	@Override
	public void setWidth(String width) {
		this.base.setWidth(width);
	}

	/**
	 * Sorts the table based on the value of the property in the specified
	 * column index.
	 *
	 * If using a SortableDataProvider, this will throw a runtime exception if
	 * the column denoted by the index is not a supported sortable column.
	 *
	 * @param index
	 *            index of the column to sort the table on.
	 */
	public void sortColumn(int index) {
		ascending[index] = !ascending[index];
		if (this.provider == null) {
			ArrayList sort = new ArrayList();
			sort.addAll(value);
			try {
				ListSorter.sortOnProperty(sort,
						columns[index].getPropertyName(), ascending[index]);
			} catch (Exception e) {
				logger.warn("Sort exception", e);
			}
			value.clear();
			for (Iterator it = sort.iterator(); it.hasNext();) {
				value.add(it.next());
			}
			setValue(value);
		} else if (this.provider instanceof SortableDataProvider) {
			SortableDataProvider sdp = (SortableDataProvider) this.provider;
			boolean canSort = false;
			String[] sortableProperties = sdp.getSortableProperties();
			for (int i = 0; (i < sortableProperties.length) && !canSort; i++) {
				if (sortableProperties[i]
						.equals(this.columns[index].getPropertyName())) {
					canSort = true;
				}
			}
			if (!canSort) {
				AlcinaTopics.devWarning.publish(new RuntimeException(Ax.format(
						"Field %s is not a"
								+ " sortable field from data provider %s.",
						this.columns[index].getPropertyName(),
						this.provider.getClass().getName())));
				return;
			}
			sortedColumn = index;
			sdp.sortOnProperty(this, this.columns[index].getPropertyName(),
					this.ascending[index]);
		}
		int startColumn = ((masks & BoundTableExt.ROW_HANDLE_MASK) > 0) ? 1 : 0;
		if ((this.masks & BoundTableExt.HEADER_MASK) > 0) {
			table.getCellFormatter().setStyleName(0, index + startColumn,
					this.ascending[index] ? "ascending" : "descending");
		}
	}

	protected static class EndRowButton extends Composite
			implements HasClickHandlers {
		private FlowPanelClickable fpc;

		public EndRowButton() {
			fpc = new FlowPanelClickable();
			initWidget(fpc);
			SpanPanel inner = new SpanPanel();
			fpc.add(inner);
			setStyleName("end-row-button");
		}

		@Override
		public HandlerRegistration addClickHandler(ClickHandler handler) {
			return fpc.addClickHandler(handler);
		}
	}

	private class EventingSimplePanel extends SimplePanel {
		EventingSimplePanel() {
			super();
			sinkEvents(Event.MOUSEEVENTS);
			sinkEvents(Event.FOCUSEVENTS);
			sinkEvents(Event.KEYEVENTS);
		}

		@Override
		public void onBrowserEvent(Event evt) {
			if (DOM.eventGetShiftKey(evt)) {
				shiftDown = true;
			} else {
				shiftDown = false;
			}
			super.onBrowserEvent(evt);
		}
	}
}
