package com.heban.app.ui.common;

import androidx.lifecycle.ViewModel;

import com.heban.app.di.AppContainer;

import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;

/** 所有 ViewModel 的基类：订阅统一放入 CompositeDisposable，onCleared 时清除（技术准备文档 5.5）。 */
public abstract class BaseViewModel extends ViewModel {
    protected final AppContainer c;
    protected final CompositeDisposable disposables = new CompositeDisposable();
    /** 一次性提示（Snackbar / Toast） */
    public final SingleLiveEvent<String> toast = new SingleLiveEvent<>();

    protected BaseViewModel(AppContainer c) {
        this.c = c;
    }

    protected void add(Disposable d) {
        disposables.add(d);
    }

    @Override
    protected void onCleared() {
        disposables.clear();
    }
}
