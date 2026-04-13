package com.odiousapps.weewxweather;

import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Broadcasts every item to all currently active observers on the main thread.
 */
public class EventBroadcaster<T>
{
    private final CopyOnWriteArraySet<ObserverEntry<T>> observers = new CopyOnWriteArraySet<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public void observe(LifecycleOwner owner, Observer<T> observer)
    {
        Objects.requireNonNull(owner);
        Objects.requireNonNull(observer);

        ObserverEntry<T> entry = new ObserverEntry<>(owner, observer);
        observers.add(entry);

        // remove when lifecycle destroyed
        owner.getLifecycle().addObserver((LifecycleEventObserver) (source, event) ->
        {
            if(event == Lifecycle.Event.ON_DESTROY)
                removeObserver(observer);
        });
    }

    public void removeObserver(Observer<T> observer)
    {
        observers.removeIf(e -> e.delegate == observer);
    }

    public void broadcast(final T item)
    {
        if (observers.isEmpty()) return;
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

    private static final class ObserverEntry<T>
    {
        final LifecycleOwner ownerRef;
        final Observer<T> delegate;

        ObserverEntry(LifecycleOwner owner, Observer<T> delegate)
        {
            this.ownerRef = owner;
            this.delegate = delegate;
        }
    }
}