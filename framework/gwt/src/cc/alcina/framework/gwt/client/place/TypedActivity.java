package cc.alcina.framework.gwt.client.place;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.place.shared.Place;

public abstract class TypedActivity<P extends Place> extends AbstractActivity {
    protected P place;

    public TypedActivity(P place) {
        this.place = place;
    }

    public P getPlace() {
        return this.place;
    }
}
