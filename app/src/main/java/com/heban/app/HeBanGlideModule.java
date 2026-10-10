package com.heban.app;

import android.content.Context;

import androidx.annotation.NonNull;

import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory;
import com.bumptech.glide.load.engine.cache.LruResourceCache;
import com.bumptech.glide.module.AppGlideModule;
import com.bumptech.glide.request.RequestOptions;

/** Glide 缓存配置：内存 20MB，磁盘 250MB（技术准备文档 10.1）。 */
@GlideModule
public final class HeBanGlideModule extends AppGlideModule {
    @Override
    public void applyOptions(@NonNull Context context, @NonNull GlideBuilder builder) {
        builder.setMemoryCache(new LruResourceCache(20L * 1024 * 1024));
        builder.setDiskCache(new InternalCacheDiskCacheFactory(context, 250L * 1024 * 1024));
        builder.setDefaultRequestOptions(new RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .placeholder(R.drawable.ph_image)
                .error(R.drawable.ph_image));
    }

    @Override
    public boolean isManifestParsingEnabled() {
        return false;
    }
}
