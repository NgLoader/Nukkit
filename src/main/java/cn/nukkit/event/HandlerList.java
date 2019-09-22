package cn.nukkit.event;

import cn.nukkit.plugin.Plugin;
import cn.nukkit.plugin.RegisteredListener;

import java.util.*;

/**
 * Created by Nukkit Team.
 */
public class HandlerList {

	private volatile RegisteredListener[] handlers = null;

	private final EnumMap<EventPriority, ArrayList<RegisteredListener>> handlerslots;

	private static final ArrayList<HandlerList> allLists = new ArrayList<>();

	public static void bakeAll() {
		synchronized (HandlerList.allLists) {
			for (HandlerList hander : HandlerList.allLists) {
				hander.bake();
			}
		}
	}

	public static void unregisterAll() {
		synchronized (HandlerList.allLists) {
			for (HandlerList handler : HandlerList.allLists) {
				synchronized (handler) {
					for (List<RegisteredListener> list : handler.handlerslots.values()) {
						list.clear();
					}
				}
			}
		}
	}

	public static void unregisterAll(Plugin plugin) {
		synchronized (HandlerList.allLists) {
			for (HandlerList h : HandlerList.allLists) {
				h.unregister(plugin);
			}
		}
	}

	public static void unregisterAll(Listener listener) {
		synchronized (HandlerList.allLists) {
			for (HandlerList h : HandlerList.allLists) {
				h.unregister(listener);
			}
		}
	}

	public static ArrayList<RegisteredListener> getRegisteredListeners(Plugin plugin) {
		ArrayList<RegisteredListener> listeners = new ArrayList<>();
		synchronized (allLists) {
			for (HandlerList handler : allLists) {
				synchronized (handler) {
					for (List<RegisteredListener> list : handler.handlerslots.values()) {
						for (RegisteredListener listener : list) {
							if (listener.getPlugin().equals(plugin)) {
								listeners.add(listener);
							}
						}
					}
				}
			}
		}
		return listeners;
	}

	public HandlerList() {
		this.handlerslots = new EnumMap<>(EventPriority.class);

		for (EventPriority priority : EventPriority.values()) {
			this.handlerslots.put(priority, new ArrayList<RegisteredListener>());
		}

		synchronized (HandlerList.allLists) {
			HandlerList.allLists.add(this);
		}
	}

	public synchronized void register(RegisteredListener listener) {
		if (this.handlerslots.get(listener.getPriority()).contains(listener))
			throw new IllegalStateException("This listener is already registered to priority " + listener.getPriority().toString());

		this.handlers = null;
		this.handlerslots.get(listener.getPriority()).add(listener);
	}

	public void registerAll(Collection<RegisteredListener> listeners) {
		for (RegisteredListener listener : listeners) {
			this.register(listener);
		}
	}

	public synchronized void unregister(RegisteredListener listener) {
		this.handlerslots.get(listener.getPriority()).remove(listener);
	}

	public synchronized void unregister(Plugin plugin) {
		for (List<RegisteredListener> list : this.handlerslots.values()) {
			for (ListIterator<RegisteredListener> listener = list.listIterator(); listener.hasNext();) {
				if (listener.next().getPlugin().equals(plugin)) {
					listener.remove();
				}
			}
		}
	}

	public synchronized void unregister(Listener remove) {
		for (List<RegisteredListener> list : this.handlerslots.values()) {
			for (ListIterator<RegisteredListener> listener = list.listIterator(); listener.hasNext();) {
				if (listener.next().getListener().equals(remove)) {
					listener.remove();
				}
			}
		}
	}

	public RegisteredListener[] getRegisteredListeners() {
		RegisteredListener[] handlers;

		while ((handlers = this.handlers) == null) {
			bake();
		} // This prevents fringe cases of returning null

		return handlers;
	}

	public synchronized void bake() {
		if (handlers != null)
			return; // don't re-bake when still valid

		List<RegisteredListener> entries = new ArrayList<RegisteredListener>();
		for (Map.Entry<EventPriority, ArrayList<RegisteredListener>> entry : this.handlerslots.entrySet()) {
			entries.addAll(entry.getValue());
		}

		handlers = entries.toArray(new RegisteredListener[0]);
	}

	public static List<HandlerList> getHandlerLists() {
		synchronized (HandlerList.allLists) {
			return Collections.unmodifiableList(HandlerList.allLists);
		}
	}

}
