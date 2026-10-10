package com.heban.app.di;

/**
 * mock 渠道（现阶段）：由后端提供的数据先用本地模拟实现（技术准备文档 5.3）。
 * 各 Repository 的工厂方法在对应开发阶段加入。
 */
public final class RepositoryModule {
    public static final String DATA_SOURCE = "mock";

    private RepositoryModule() {
    }
}
