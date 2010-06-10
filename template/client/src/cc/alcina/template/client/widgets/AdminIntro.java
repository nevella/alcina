package cc.alcina.template.client.widgets;




import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;

public class AdminIntro extends Composite {
	public AdminIntro(){
		HorizontalPanel panel = new HorizontalPanel();
		HTML h = new HTML("<div align='left' style='margin-left:100px'><div>\n" +
				"<br /><h3>Admin Intro</h3>\n" +
			
				"1. Please be careful with the 'enums' section...basically, just for developers.\n" +
				"</div></div>");
		panel.add(h);
		panel.setWidth("400px");
		initWidget(panel);
	}
}
