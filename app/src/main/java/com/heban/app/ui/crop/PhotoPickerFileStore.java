package com.heban.app.ui.crop;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * {@link PhotoPickerStore} 的文件实现：照片选择器返回的 content:// 图片复制到 filesDir/&lt;dir&gt;/，
 * 文件名随机生成，扩展名按 MIME 类型。相机拍照先写入 MediaStore 中的一条临时记录
 * （App 自己创建的记录不需要存储权限，也不需要 FileProvider），复制完成后删除。
 */
public final class PhotoPickerFileStore implements PhotoPickerStore {
    private static final String TAG = "PhotoPicker";

    private final Context app;

    public PhotoPickerFileStore(@NonNull Context context) {
        app = context.getApplicationContext();
    }

    @NonNull
    @Override
    public Single<String> copy(@NonNull Uri source, @NonNull String dir) {
        return Single.fromCallable(() -> copySync(source, dir)).subscribeOn(Schedulers.io());
    }

    @NonNull
    @Override
    public Single<List<String>> copyAll(@NonNull List<Uri> sources, @NonNull String dir) {
        List<Uri> list = new ArrayList<>(sources);
        return Single.fromCallable(() -> {
            List<String> out = new ArrayList<>();
            try {
                for (Uri uri : list) out.add(copySync(uri, dir));
            } catch (IOException | RuntimeException e) {
                deleteSync(out);
                throw e;
            }
            return out;
        }).subscribeOn(Schedulers.io());
    }

    @SuppressLint("CheckResult")   // 后台删除，不关心结果
    @Override
    public void delete(@NonNull Collection<String> paths) {
        List<String> list = new ArrayList<>(paths);
        if (list.isEmpty()) return;
        Completable.fromAction(() -> deleteSync(list))
                .subscribeOn(Schedulers.io())
                .subscribe(() -> { }, e -> Log.w(TAG, "delete failed", e));
    }

    @Nullable
    @Override
    public Uri createCameraTarget() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, "heban_" + System.currentTimeMillis() + ".jpg");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);
        try {
            return app.getContentResolver().insert(
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values);
        } catch (RuntimeException e) {
            Log.w(TAG, "camera target", e);
            return null;
        }
    }

    @SuppressLint("CheckResult")
    @Override
    public void discardCameraTarget(@NonNull Uri target) {
        Completable.fromAction(() -> app.getContentResolver().delete(target, null, null))
                .subscribeOn(Schedulers.io())
                .subscribe(() -> { }, e -> Log.w(TAG, "discard camera target failed", e));
    }

    private String copySync(Uri source, String dir) throws IOException {
        File folder = new File(app.getFilesDir(), dir);
        if (!folder.isDirectory() && !folder.mkdirs()) throw new IOException("cannot create " + folder);
        ContentResolver resolver = app.getContentResolver();
        File target = new File(folder, UUID.randomUUID().toString().replace("-", "")
                + extension(resolver.getType(source)));
        try (InputStream in = resolver.openInputStream(source);
             OutputStream out = new FileOutputStream(target)) {
            if (in == null) throw new FileNotFoundException(source.toString());
            byte[] buf = new byte[16 * 1024];
            int n;
            long total = 0;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
                total += n;
            }
            if (total == 0) throw new IOException("empty image " + source);
        } catch (IOException | RuntimeException e) {
            //noinspection ResultOfMethodCallIgnored
            target.delete();
            throw e;
        }
        return Uri.fromFile(target).toString();
    }

    private void deleteSync(Collection<String> paths) throws IOException {
        String base = app.getFilesDir().getCanonicalPath() + File.separator;
        for (String p : paths) {
            if (p == null) continue;
            Uri uri = Uri.parse(p);
            if (!"file".equals(uri.getScheme()) || uri.getPath() == null) continue;
            File f = new File(uri.getPath());
            if (f.getCanonicalPath().startsWith(base)) {
                //noinspection ResultOfMethodCallIgnored
                f.delete();
            }
        }
    }

    private static String extension(@Nullable String mime) {
        String ext = mime == null ? null : MimeTypeMap.getSingleton().getExtensionFromMimeType(mime);
        return "." + (ext == null || ext.isEmpty() ? "jpg" : ext);
    }
}
