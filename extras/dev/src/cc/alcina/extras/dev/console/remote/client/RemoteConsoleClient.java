package cc.alcina.extras.dev.console.remote.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Document;

import cc.alcina.extras.dev.console.remote.client.common.logic.RemoteConsoleInit;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.LiSet;

public class RemoteConsoleClient implements EntryPoint {
    @Override
    public void onModuleLoad() {
        // force init
        LiSet liSet = new LiSet();
        Document.get().getDocumentElement();
        Scheduler.get().scheduleDeferred(() -> init0());
    }

    private void init0() {
        new RemoteConsoleInit().init();
        RemoteConsoleLayout.get().init();
    }
}
