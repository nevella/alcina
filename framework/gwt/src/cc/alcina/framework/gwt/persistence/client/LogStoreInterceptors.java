package cc.alcina.framework.gwt.persistence.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.DomState;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.Text;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Event.NativePreviewHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;

import cc.alcina.framework.common.client.entity.ClientLogRecord;
import cc.alcina.framework.common.client.entity.ReplayInstruction;
import cc.alcina.framework.common.client.util.AlcinaTopics;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.StringPair;
import cc.alcina.framework.common.client.util.TextUtils;
import cc.alcina.framework.common.client.util.TopicListener;
import cc.alcina.framework.gwt.client.ClientNotifications;
import cc.alcina.framework.gwt.client.util.ClientNodeIterator;

public class LogStoreInterceptors {
	private ValueChangeHandler<String> historyListener = new ValueChangeHandler<String>() {
		@Override
		public void onValueChange(ValueChangeEvent<String> event) {
			AlcinaTopics.categorisedLogMessage.publish(new StringPair(
					AlcinaTopics.LOG_CATEGORY_HISTORY, event.getValue()));
		}
	};

	private int statsMuteCounter = 0;

	private TopicListener<Boolean> muteListener = message -> statsMuteCounter += message
			? 1
			: -1;

	private HandlerRegistration historyHandlerRegistration;

	private HandlerRegistration nativePreviewHandlerRegistration;

	List<String> stats = new ArrayList<>();

	private boolean logStatPaused = false;

	private String lastFocussedValueMessage;

	private boolean numberedElements;

	private HandlerRegistration windowClosingHandlerRegistration;

	public void handleNativeEvent(Event nativeEvent, boolean click,
			boolean blur, boolean focus) {
		EventTarget eTarget = nativeEvent.getEventTarget();
		if (Element.is(eTarget)) {
			Element e = null;
			try {
				e = Element.as(eTarget);
			} catch (Exception e1) {
				// FIXME - dirndl.1 - some invalid tables happening?
				Ax.simpleExceptionOut(e1);
				return;
			}
			if (blur || focus) {
				String tag = e.getTagName().toLowerCase();
				if (tag.equals("input")
						&& e.getAttribute("type").equals("button")) {
					return;
				}
				if (!(tag.equals("input") || tag.equals("select")
						|| tag.equals("textarea"))) {
					return;
				}
			}
			List<String> tags = new ArrayList<String>();
			String text = "";
			ClientNodeIterator itr = new ClientNodeIterator(e,
					ClientNodeIterator.SHOW_TEXT);
			itr.nextNode();
			while (text.length() < 50 && itr.getCurrentNode() != null) {
				Text t = (Text) itr.getCurrentNode();
				text += TextUtils.normalizeWhitespaceAndTrim(t.getData());
				itr.nextNode();
			}
			while (e != null) {
				List<String> parts = new ArrayList<String>();
				parts.add(e.getTagName());
				if (numberedElements) {
					int sameTagCount = 0;
					NodeList<Node> kids = e.getParentNode().getChildNodes();
					for (int idn = 0; idn < kids.getLength(); idn++) {
						Node node = kids.getItem(idn);
						if (node == e) {
							parts.add(Ax.format("[%s]", sameTagCount));
							break;
						}
						if (node.getNodeType() == Node.ELEMENT_NODE
								&& ((Element) node).getTagName()
										.equals(e.getTagName())) {
							sameTagCount++;
						}
					}
				} else {
					if (!e.getId().isEmpty()) {
						parts.add("#" + e.getId());
					}
					try {
						DomState.domResolveSvgStyles = true;
						String cn = e.getClassName();
						if (!cn.isEmpty()) {
							parts.add("." + cn);
						}
					} finally {
						DomState.domResolveSvgStyles = false;
					}
				}
				tags.add(CommonUtils.join(parts, ""));
				if (e.getParentElement() == null
						&& !e.getTagName().equals("HTML")) {
					// probably doing something drastic in a previous
					// native handler - try to defer
				}
				e = e.getParentElement();
			}
			Collections.reverse(tags);
			String path = CommonUtils.join(tags, "/");
			String valueMessage = "";
			if (blur || focus) {
				String value = Element.as(eTarget).getPropertyString("value");
				String ih = Element.as(eTarget).getInnerHTML();
				valueMessage = Ax.format("%s%s",
						ClientLogRecord.VALUE_SEPARATOR, value);
				if (focus) {
					lastFocussedValueMessage = valueMessage;
					return;
				} else {
					if (valueMessage.equals(lastFocussedValueMessage)) {
						lastFocussedValueMessage = null;
						return;// no change
					}
				}
			}
			AlcinaTopics.categorisedLogMessage.publish(new StringPair(
					click ? AlcinaTopics.LOG_CATEGORY_CLICK
							: AlcinaTopics.LOG_CATEGORY_CHANGE,
					ReplayInstruction.createReplayBody(path, text,
							valueMessage)));
		}
	}

	public void installStats() {
		AlcinaTopics.muteStatisticsLogging.add(muteListener);
		installStats0();
	}

	public void interceptClientLog() {
		AlcinaTopics.categorisedLogMessage
				.add(LogStore.get().getStringPairListener());
	}

	public boolean isLogStatPaused() {
		return this.logStatPaused;
	}

	public boolean isNumberedElements() {
		return this.numberedElements;
	}

	public void logClicksAndChanges() {
		nativePreviewHandlerRegistration = Event
				.addNativePreviewHandler(new NativePreviewHandler() {
					@Override
					public void onPreviewNativeEvent(NativePreviewEvent event) {
						previewNativeEvent(event);
					}
				});
	}

	public void logHistoryEvents() {
		this.historyHandlerRegistration = History
				.addValueChangeHandler(historyListener);
		windowClosingHandlerRegistration = Window.addWindowClosingHandler(
				evt -> AlcinaTopics.categorisedLogMessage.publish(
						new StringPair(AlcinaTopics.LOG_CATEGORY_HISTORY,
								"window closing")));
	}

	public void logStat(String stat) {
		if (logStatPaused) {
			stats.add(stat);
			return;
		}
		ClientNotifications.get().log(stat);
		AlcinaTopics.categorisedLogMessage
				.publish(new StringPair(AlcinaTopics.LOG_CATEGORY_STAT, stat));
	}

	public void setLogStatPaused(boolean logStatPaused) {
		this.logStatPaused = logStatPaused;
		if (!logStatPaused) {
			stats.forEach(s -> logStat(s));
			stats.clear();
		}
	}

	public void setNumberedElements(boolean numberedElements) {
		this.numberedElements = numberedElements;
	}

	public void unload() {
		AlcinaTopics.categorisedLogMessage
				.remove(LogStore.get().getStringPairListener());
		AlcinaTopics.muteStatisticsLogging.remove(muteListener);
		if (historyHandlerRegistration != null) {
			historyHandlerRegistration.removeHandler();
		}
		if (nativePreviewHandlerRegistration != null) {
			nativePreviewHandlerRegistration.removeHandler();
		}
		if (windowClosingHandlerRegistration != null) {
			windowClosingHandlerRegistration.removeHandler();
		}
	}

	protected void previewNativeEvent(NativePreviewEvent event) {
		Event nativeEvent = Event.as(event.getNativeEvent());
		String type = null;
		try {
			type = nativeEvent.getType();
		} catch (Exception e1) {
			// FF22 throwing some permissions exceptions, gawd knows why
			return;
		}
		boolean click = BrowserEvents.CLICK.equals(type);
		boolean blur = BrowserEvents.BLUR.equals(type)
				|| BrowserEvents.FOCUSOUT.equals(type);
		boolean focus = BrowserEvents.FOCUS.equals(type)
				|| BrowserEvents.FOCUSIN.equals(type);
		if (click || blur || focus) {
			handleNativeEvent(nativeEvent, click, blur, focus);
		}
	}

	boolean areStatsMuted() {
		return statsMuteCounter > 0;
	}

	native void installStats0()/*-{
    function format(out) {
      var idx = 0;
      var j = 1;

      while (true) {
        idx = out.indexOf("%s", idx);
        if (idx == -1) {
          break;
        }
        var ins = arguments[j++];
        if (ins === null) {
          ins = "null";
        } else if (ins === undefined) {
          ins = "undefined";
        } else {
          ins = ins.toString();
        }
        out = out.substring(0, idx) + ins + out.substring(idx + 2);
        idx += ins.length;
      }
      return out;
    }
    function pad0(s, len) {
      return pad(s, "0", len);
    }
    function pad(s, sup, len) {
      s = "" + s;
      while (s.length < len) {
        s = sup + s;
      }
      return s;
    }
    var lsi = this;
    var running = [];
    function eventToString(event) {
      // return some string representation of this event
      var d = new Date(event.millis);
      var timeStr = format("%s:%s:%s,%s", pad0(d.getHours(), 2), pad0(d
          .getMinutes(), 2), pad0(d.getSeconds(), 2), pad0(d.getMilliseconds(),
          3));
      return event.evtGroup + " | " + event.moduleName + " | "
          + event.subSystem + " | " + event.method + " | "
          + pad(event.type, " ", 25) + " | " + timeStr;
    }
    window.$stats = function(evt) {
      var muted = lsi.@cc.alcina.framework.gwt.persistence.client.LogStoreInterceptors::areStatsMuted()();
      if (!muted) {
        var e2s = eventToString(evt);
        lsi.@cc.alcina.framework.gwt.persistence.client.LogStoreInterceptors::logStat(Ljava/lang/String;)(e2s);
      }
      return true;
    };
    //if there were stats collected prior to this install, flush 'em
    if (window["stats_pre"]) {
      for ( var k in window.stats_pre) {
        var pre = window.stats_pre[k];
        lsi.@cc.alcina.framework.gwt.persistence.client.LogStoreInterceptors::logStat(Ljava/lang/String;)(pre);
      }
      window.$stats_pre = [];
    }
    if ($wnd["stats_pre"]) {
      for ( var k in $wnd.stats_pre) {
        var pre = $wnd.stats_pre[k];
        lsi.@cc.alcina.framework.gwt.persistence.client.LogStoreInterceptors::logStat(Ljava/lang/String;)(pre);
      }
      $wnd.$stats_pre = [];
    }

	}-*/;
}