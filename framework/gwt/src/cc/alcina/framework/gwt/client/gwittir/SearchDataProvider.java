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
package cc.alcina.framework.gwt.client.gwittir;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.totsp.gwittir.client.beans.BeanDescriptor;
import com.totsp.gwittir.client.beans.Converter;
import com.totsp.gwittir.client.beans.Property;
import com.totsp.gwittir.client.ui.table.HasChunks;
import com.totsp.gwittir.client.ui.table.SortableDataProvider;

import cc.alcina.framework.common.client.collections.CollectionFilters;
import cc.alcina.framework.common.client.csobjects.SearchResultsBase;
import cc.alcina.framework.common.client.search.SearchCriterion.Direction;
import cc.alcina.framework.common.client.search.SingleTableSearchDefinition;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.gwt.client.logic.CancellableAsyncCallback;

/**
 * 
 * @author Nick Reddel
 */
public abstract class SearchDataProvider implements SortableDataProvider {
	protected SingleTableSearchDefinition def;

	private final AsyncCallback completionCallback;

	private final Converter converter;

	private SearchCallback runningCallback;

	public SearchDataProvider(SingleTableSearchDefinition def,
			AsyncCallback completionCallback, Converter converter) {
		this.def = def;
		this.completionCallback = completionCallback;
		this.converter = converter;
	}

	@Override
	public void getChunk(HasChunks table, int chunkNumber) {
		runSort(false, chunkNumber, table);
	}

	@Override
	public String[] getSortableProperties() {
		BeanDescriptor descriptor = GwittirBridge.get()
				.getDescriptorForClass(def.getResultClass());
		List<String> pNames = new ArrayList<String>();
		for (Property p : descriptor.getProperties()) {
			if (p.getType().isPrimitive() || p.getType().isEnum()
					|| CommonUtils.isStandardJavaClass(p.getType())) {
				pNames.add(p.getName());
			}
		}
		return (String[]) pNames.toArray(new String[pNames.size()]);
	}

	@Override
	public void init(HasChunks table) {
		runSort(true, 0, table);
	}

	@Override
	public void sortOnProperty(HasChunks table, String propertyName,
			boolean ascending) {
		def.setOrderDirection(
				ascending ? Direction.ASCENDING : Direction.DESCENDING);
		def.setOrderPropertyName(propertyName);
		runSort(true, 0, table);
	}

	protected void runSort(final boolean callBackInit, int pageNumber,
			final HasChunks table) {
		if (runningCallback != null) {
			if (runningCallback.isCallBackInit()) {
				return;
			} else {
				runningCallback.setCancelled(true);
			}
		}
		SearchCallback callback = new SearchCallback(callBackInit) {
			@Override
			public void onFailure(Throwable caught) {
				if (isCancelled()) {
					return;
				}
				completionCallback.onFailure(caught);
				runningCallback = null;
			}

			@Override
			public void onSuccess(SearchResultsBase result) {
				if (isCancelled()) {
					return;
				}
				List results = result.getResults();
				if (converter != null) {
					results = CollectionFilters.convert(results, converter);
				}
				if (callBackInit) {
					table.init(results, result.pageCount());
				} else {
					table.setChunk(results);
				}
				completionCallback.onSuccess(result);
				runningCallback = null;
			}
		};
		search(pageNumber, callback);
	}

	protected abstract void search(int pageNumber, SearchCallback callback);

	public abstract static class SearchCallback
			extends CancellableAsyncCallback<SearchResultsBase> {
		private final boolean callBackInit;

		public SearchCallback(boolean callBackInit) {
			this.callBackInit = callBackInit;
		}

		public boolean isCallBackInit() {
			return callBackInit;
		}
	}

	public static class SearchDataProviderCommon extends SearchDataProvider {
		public SearchDataProviderCommon(SingleTableSearchDefinition def,
				AsyncCallback completionCallback, Converter converter) {
			super(def, completionCallback, converter);
		}

		@Override
		protected void search(int pageNumber, SearchCallback callback) {
			Client.commonRemoteService().search(def,
					pageNumber, callback);
		}
	}
}
