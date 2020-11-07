package cc.alcina.framework.gwt.client.cell.tree;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.Cell.Context;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.OutlineStyle;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.safecss.shared.SafeStyles;
import com.google.gwt.safecss.shared.SafeStylesBuilder;
import com.google.gwt.safecss.shared.SafeStylesUtils;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.client.SafeHtmlTemplates.Template;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.CellList;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionModel;
import com.google.gwt.view.client.TreeViewModel;
import com.google.gwt.view.client.TreeViewModel.DefaultNodeInfo;

import cc.alcina.framework.gwt.client.cell.tree.res.RoCellTreeRes;

public class RoCellTree<T> extends Composite {
	private static final Template TEMPLATE2 = GWT.create(Template.class);

	private TreeViewModel model;

	private CellList<T> cellList;

	protected Map<T, Integer> depthMap = new LinkedHashMap<T, Integer>();

	public RoCellTree(TreeViewModel model, T root, Cell<T> cell,
			RoCellTreeRes resources) {
		this.model = model;
		cellList = new CellListWithStyles(cell, resources);
		cellList.setPageSize(Integer.MAX_VALUE);
		final ListDataProvider<T> dataProvider = new ListDataProvider<T>();
		makeDepthMap(root, -1);
		ArrayList<T> list = new ArrayList<T>(depthMap.keySet());
		maybeSort(list);
		dataProvider.getList().addAll(list);
		dataProvider.addDataDisplay(cellList);
		initWidget(cellList);
	}

	protected void makeDepthMap(T node, int depth) {
		if (depth >= 0) {
			depthMap.put(node, depth);
		}
		List<T> list = ((RoCellTree.VisibleNodeInfo<T>) model
				.getNodeInfo(node)).dataProviderPkg.getList();
		for (T t : list) {
			makeDepthMap(t, depth + 1);
		}
	}

	protected void maybeSort(ArrayList<T> list) {
	}

	public static class VisibleNodeInfo<T> extends DefaultNodeInfo<T> {
		ListDataProvider<T> dataProviderPkg;

		public VisibleNodeInfo(ListDataProvider<T> dataProvider, Cell<T> cell,
				SelectionModel<? super T> selectionModel,
				ValueUpdater<T> valueUpdater) {
			super(dataProvider, cell, selectionModel, valueUpdater);
			this.dataProviderPkg = dataProvider;
		}
	}

	private final class CellListWithStyles extends CellList<T> {
		private RoCellTreeRes.ListStyle style2;

		private Cell<T> cell2;

		private CellListWithStyles(Cell<T> cell, RoCellTreeRes resources) {
			super(cell, resources);
			this.cell2 = cell;
			this.style2 = resources.cellListStyle();
			this.style2.ensureInjected();
		}

		@Override
		protected boolean isKeyboardNavigationSuppressed() {
			return true;
		}

		@Override
		protected void onFocus() {
			// noop }
		}

		@Override
		protected void renderRowValues(SafeHtmlBuilder sb, List<T> values,
				int start, SelectionModel<? super T> selectionModel) {
			String evenItem = style2.cellListEvenItem();
			String oddItem = style2.cellListOddItem();
			int length = values.size();
			int end = start + length;
			for (int i = start; i < end; i++) {
				T value = values.get(i - start);
				StringBuilder classesBuilder = new StringBuilder();
				classesBuilder.append(i % 2 == 0 ? evenItem : oddItem);
				Integer depth = depthMap.get(value);
				if (depth == 0) {
					classesBuilder.append(" ");
					classesBuilder.append(style2.cellListDepth0());
				}
				SafeStylesBuilder stylesBuilder = new SafeStylesBuilder();
				stylesBuilder.append(
						SafeStylesUtils.forOutlineStyle(OutlineStyle.NONE));
				stylesBuilder
						.append(SafeStylesUtils.forPaddingLeft(depth, Unit.EM));
				SafeHtmlBuilder cellBuilder = new SafeHtmlBuilder();
				Context context = new Context(i, 0, getValueKey(value));
				cell2.render(context, value, cellBuilder);
				sb.append(TEMPLATE2.div(i, classesBuilder.toString(),
						stylesBuilder.toSafeStyles(),
						cellBuilder.toSafeHtml()));
			}
		}

		@Override
		protected void setKeyboardSelected(int index, boolean selected,
				boolean stealFocus) {
			// ignorea
		}
	}

	interface Template extends SafeHtmlTemplates {
		@Template("<div onclick=\"\" __idx=\"{0}\" class=\"{1}\" style=\"{2}\" >{3}</div>")
		SafeHtml div(int idx, String classes, SafeStyles paddingLeft,
				SafeHtml cellContents);
	}
}