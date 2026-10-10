package com.heban.app.di;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

/** 所有 ViewModel 都通过构造函数 (AppContainer) 创建（技术准备文档 5.2）。 */
public final class AppViewModelFactory implements ViewModelProvider.Factory {
    private final AppContainer container;

    public AppViewModelFactory(AppContainer container) {
        this.container = container;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        try {
            return modelClass.getConstructor(AppContainer.class).newInstance(container);
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException(
                    "ViewModel 需要 (AppContainer) 构造函数: " + modelClass, e);
        }
    }
}
