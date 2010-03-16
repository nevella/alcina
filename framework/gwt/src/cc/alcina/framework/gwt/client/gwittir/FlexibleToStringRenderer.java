package cc.alcina.framework.gwt.client.gwittir;

import com.totsp.gwittir.client.ui.Renderer;

/**
 * To allow subclassing
 * @author nick@alcina.cc
 *
 * @param <T>
 */
public class FlexibleToStringRenderer<T> implements Renderer<T,String> {

    public static final FlexibleToStringRenderer<Object> INSTANCE = new FlexibleToStringRenderer<Object>();

    public String render(T o) {
        return (o == null) ? "" : o.toString();
    }
}