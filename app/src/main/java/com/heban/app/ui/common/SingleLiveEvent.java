package com.heban.app.ui.common;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import java.util.concurrent.atomic.AtomicBoolean;

/** 只分发一次的 LiveData，用于 Snackbar、导航等一次性事件。 */
public class SingleLiveEvent<T> extends MutableLiveData<T> {
    private final AtomicBoolean pending = new AtomicBoolean(false);

    @MainThread
    @Override
    public void observe(@NonNull LifecycleOwner owner, @NonNull Observer<? super T> observer) {
        super.observe(owner, t -> {
            if (pending.compareAndSet(true, false)) {
                observer.onChanged(t);
            }
        });
    }

    @MainThread
    @Override
    public void setValue(@Nullable T t) {
        pending.set(true);
        super.setValue(t);
    }
}
