package com.heban.app.data.remote.llm;

import com.heban.app.data.remote.llm.dto.ChatChunk;
import com.heban.app.data.remote.llm.dto.ChatRequest;

import io.reactivex.rxjava3.core.Single;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Url;

/** 非流式 Chat Completions（识图、测试连接、一次性回答），URL 由 LlmConfig 给出。 */
public interface LlmApi {
    @POST
    Single<ChatChunk> complete(@Url String url, @Header("Authorization") String auth, @Body ChatRequest body);
}
