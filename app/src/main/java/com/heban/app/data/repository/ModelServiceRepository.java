package com.heban.app.data.repository;

import com.heban.app.data.model.ModelServiceConfig;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

/** M11「模型服务」（技术准备文档 9.6）：用户自己的 OpenAI 兼容配置，Key 经 SecureStore 加密保存。 */
public interface ModelServiceRepository {
    /** 当前保存的用户配置（apiKey 为空，只给 apiKeyLast4）；没有保存过时 enabled = false */
    Flowable<ModelServiceConfig> observeUserConfig();

    /** 保存并启用；apiKey 为空时保留原来的 Key */
    Completable save(ModelServiceConfig config);

    /** 「使用内置配置」：清除用户配置 */
    Completable clear();

    /** 「测试连接」：发送「你好」（max_tokens 8，非流式）；成功返回模型名，失败以 AppError 可转换的异常结束 */
    Single<String> test(ModelServiceConfig config);

    /** 内置配置是否可用（local.properties 中填写了 DeepSeek Key） */
    boolean hasBuiltInKey();
}
