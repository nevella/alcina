package cc.alcina.extras.webdriver.tour;

import java.util.List;

import cc.alcina.framework.gwt.client.tour.Tour.Step;
import cc.alcina.framework.gwt.client.tour.TourManager;
import cc.alcina.framework.gwt.client.tour.TourManager.UIRenderer;

public class UiRendererWd extends UIRenderer {
	@Override
	protected void afterStepListenerAction() {
		// TODO Auto-generated method stub
	}

	@Override
	protected void clearPopups() {
		// TODO Auto-generated method stub
	}

	@Override
	protected void exitTour(String message) {
		// TODO Auto-generated method stub
	}

	@Override
	protected boolean hasElement(List<String> selectors) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected boolean performAction(Step step) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected void publishNext() {
		// TODO Auto-generated method stub
	}

	@Override
	protected void render(Step step) {
		// TODO Auto-generated method stub
	}

	@Override
	protected boolean showStepPopups() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected void startTour(TourManager tourManager) {
		int debug = 3;
	}
}
