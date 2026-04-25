package com.odiousapps.weewxweather;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;

import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Broadcasts every item to all currently active observers on the main thread.
 */

@DontObfuscate
class EventBroadcaster<T>
{
	private final CopyOnWriteArraySet<ObserverEntry<T>> observers = new CopyOnWriteArraySet<>();
	private final Handler mainHandler = new Handler(Looper.getMainLooper());
	private record ObserverEntry<T>(LifecycleOwner ownerRef, Observer<T> delegate) {}

	void observe(@NonNull LifecycleOwner owner, @NonNull Observer<T> observer)
	{
		ObserverEntry<T> entry = new ObserverEntry<>(owner, observer);
		observers.add(entry);

		// remove when lifecycle destroyed
		owner.getLifecycle().addObserver((LifecycleEventObserver) (source, event) ->
		{
			if(event == Lifecycle.Event.ON_DESTROY)
				removeObserver(observer);
		});
	}

	void removeObserver(Observer<T> observer)
	{
		observers.removeIf(e -> e.delegate == observer);
	}

	void broadcast(final T item)
	{
		if (observers.isEmpty())
			return;

		// deliver each item to each observer on main thread
		mainHandler.post(() ->
		{
			for (ObserverEntry<T> e : observers)
			{
				// only deliver to observers whose lifecycle is at least STARTED
				LifecycleOwner owner = e.ownerRef;
				if (owner != null && owner.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED))
				{
					try
					{
						e.delegate.onChanged(item);
					} catch (Exception ignored) { }
				}
			}
		});
	}
}