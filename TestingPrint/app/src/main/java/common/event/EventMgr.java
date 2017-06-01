package common.event;

import java.util.ArrayList;
import java.util.List;

public class EventMgr {
	private List<EventListener> listEventListener = new ArrayList<EventListener>();
	public void add(EventListener eventListener) {
		synchronized(this) {
			listEventListener.add(eventListener);
		}
	}
	public void remove(EventListener eventListener) {
		synchronized(this) {
			for(int i=0;i<listEventListener.size();i++) {
				if(listEventListener.get(i).equals(eventListener)) {
					listEventListener.remove(i);
					return;
				}
			}
		}
	}
	public void clear() {
		synchronized(this) {
			listEventListener.clear();
		}
	}
	public void addEvent(Object event) {
		synchronized(this) {
			for(int i=0;i<listEventListener.size();i++) {
				listEventListener.get(i).apply(event);
			}
		}
	}
}
