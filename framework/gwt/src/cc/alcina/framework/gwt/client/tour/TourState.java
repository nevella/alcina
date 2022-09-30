package cc.alcina.framework.gwt.client.tour;

import java.util.List;

public class TourState {
	static TourState fromJson(String tourJson) {
		TourState model = new TourState();
		model.tour = TourJso.fromJson(tourJson);
		return model;
	}

	private int stepIdx;

	private Tour tour;

	public TourState() {
	}

	public TourState(Tour tour) {
		this.tour = tour;
	}

	public Tour.Step getCurrentStep() {
		return getSteps().get(stepIdx);
	}

	public int getCurrentStepIndex() {
		return stepIdx;
	}

	public List<? extends Tour.Step> getSteps() {
		return tour.getSteps();
	}

	String getName() {
		return tour.getName();
	}

	void gotoStep(int idx) {
		stepIdx = idx;
	}

	boolean hasNext() {
		return stepIdx < getSteps().size() - 1;
	}

	boolean hasPrevious() {
		return stepIdx > 0;
	}
}
