/*
 * Copyright 2010 Google Inc.
 * 
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
package com.google.gwt.dev.jjs;

import com.google.gwt.core.ext.BadPropertyValueException;
import com.google.gwt.core.ext.PropertyOracle;
import com.google.gwt.core.ext.SelectionProperty;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.js.ast.JsBlock;
import com.google.gwt.dev.js.ast.JsCatch;
import com.google.gwt.dev.js.ast.JsContext;
import com.google.gwt.dev.js.ast.JsFunction;
import com.google.gwt.dev.js.ast.JsModVisitor;
import com.google.gwt.dev.js.ast.JsNameRef;
import com.google.gwt.dev.js.ast.JsParameter;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsThrow;
import com.google.gwt.dev.js.ast.JsTry;

/**
 */
public class JsWrapIos6Body {

  /**
   * wrap method body in try/catch
   */
  private static class MyVisitor extends JsModVisitor {

	  @Override
	  public void endVisit(final JsFunction x, final JsContext ctx) {

	    SourceInfo sourceInfo = x.getSourceInfo();

	    //catch block
	    JsBlock catchBody = new JsBlock(sourceInfo);
	    JsCatch catchNode = new JsCatch(sourceInfo, x.getScope(), "e");
	    catchNode.setBody(catchBody);

	    //statement: throw e
	    JsParameter catchParam = catchNode.getParameter();
	    JsNameRef catchParamNameRef = new JsNameRef(sourceInfo, catchParam.getName());
	    JsThrow throwNode = new JsThrow(sourceInfo, catchParamNameRef);
	    catchBody.getStatements().add(throwNode);

	    //try block with original method body and with catch added.
	    JsTry tryNode = new JsTry(sourceInfo);
	    tryNode.getCatches().add(catchNode);
	    tryNode.setTryBlock(x.getBody());

	    //new function body with try block added.
	    JsBlock functionBody = new JsBlock(sourceInfo);
	    functionBody.getStatements().add(tryNode);
	    x.setBody(functionBody);

	  }
  }

  /**
   * If this permutation may be executed on WebKit, rewrite a >> b as ~~a >> b.
   * 
   * @param program
   * @param logger
   * @param propertyOracles
   * @return true if any changes were made
   */
  public static boolean exec(JsProgram program, TreeLogger logger,
      PropertyOracle[] propertyOracles) {
    boolean seenWebKit = false;
    for (PropertyOracle oracle : propertyOracles) {
      try {
        SelectionProperty prop = oracle.getSelectionProperty(logger,
            "mobile.user.agent");
        System.out.println("prop value:"+prop.getCurrentValue()); 
        // TODO(jat): more checks if we split up the safari permutation
        if ("ios6".equals(prop.getCurrentValue())) {
          seenWebKit = true;
          break;
        }
      } catch (BadPropertyValueException e) {
        // if we couldn't get the property, assume this might be used on WebKit.
        seenWebKit = true;
        break;
      }
    }
    if (!seenWebKit) {
      return false;
    }
    MyVisitor v = new MyVisitor();
    v.accept(program);
    return v.didChange();
  }
}
