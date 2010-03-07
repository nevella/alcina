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

import com.totsp.gwittir.client.ui.TextBox;

/**
 *
 * @author <a href="mailto:nick@alcina.cc">Nick Reddel</a>
 */

 public class PatchedTextBox<B> extends TextBox<B> {
	public void setValue(B value) {
        String old = this.getValue();
        this.setText( this.getRenderer() != null ? this.getRenderer().render(value) : ""+value);
        if( this.getValue() != old && (this.getValue() == null||
        		(this.getValue() != null && !this.getValue().equals( old ) ))){
            this.changes.firePropertyChange("value", old, this.getValue());
        }
    }
}
