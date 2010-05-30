package cc.alcina.template.client.widgets;

import cc.alcina.framework.gwt.client.widget.APanel.AAnchor;
import cc.alcina.template.client.logic.AlcinaTemplateContentProvider;
import cc.alcina.template.cs.AlcinaTemplateHistory;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.InlineHTML;
import com.google.gwt.user.client.ui.InlineLabel;

@SuppressWarnings("unused")
public class FooterCmp extends Composite {
	private AAnchor contactButton;


	public FooterCmp() {
		FlowPanel panel = new FlowPanel();
		this.contactButton = new AAnchor("alcina-FooterLink", AlcinaTemplateHistory.get()
				.getContentLink(AlcinaTemplateContentProvider.CONTACT_US));
		
		InlineHTML blurb = new InlineHTML(
				"&copy; Copyright statement");
//		panel.add(contactButton);
//		panel.add(createSeparator());
		
		panel.add(blurb);
		panel.setStyleName("alcina-Footer");
		initWidget(panel);
	}

	private InlineLabel createSeparator() {
		return new InlineLabel(" | ");
	}
}
