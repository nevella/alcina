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
package cc.alcina.framework.common.client.remote;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.SuggestOracle.Response;

import cc.alcina.framework.common.client.domain.search.BindableSearchDefinition;
import cc.alcina.framework.common.client.domain.search.ModelSearchResults;
import cc.alcina.framework.gwt.client.gwittir.widget.BoundSuggestBox.BoundSuggestOracleRequest;

/**
 * 
 * @author Nick Reddel
 */
public interface SearchRemoteServiceAsync {
	void getForClass(String className, long objectId,
			AsyncCallback<ModelSearchResults> callback);

	void searchModel(BindableSearchDefinition def,
			AsyncCallback<ModelSearchResults> callback);

	public void suggest(BoundSuggestOracleRequest request,
			AsyncCallback<Response> asyncCallback);
}
