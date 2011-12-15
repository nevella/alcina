package cc.alcina.framework.gwt.client.res;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.TextResource;

public interface AlcinaResources extends ClientBundle {
	public static final AlcinaResources INSTANCE =  GWT.create(AlcinaResources.class);

	  @Source("Alcina.css")
	  public TextResource css();
	  
	
}