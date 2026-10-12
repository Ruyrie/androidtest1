package com.heban.app.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;
import java.util.UUID;

/**
 * 图片工具（技术准备文档 9.5）：相册图片用 ImageDecoder（ALLOCATOR_SOFTWARE）解码；
 * 发给大模型前缩放到最长边 1280px、JPEG 质量 80（约 200–400KB）并转成 data URL；
 * 照片保存在 App 私有目录 filesDir 下，以 file:// 地址记录（与 ImagePaths.toUri 兼容）。
 */
public final class ImageUtils {
    public static final int MAX_EDGE = 1280;
    public static final int QUALITY = 80;
    /** 拍照识别的照片目录 */
    public static final String DIR_SCANS = "scans";
    /** AI 对话中附带的图片目录 */
    public static final String DIR_CHAT = "chat";

    private static final String FILE_SCHEME = "file://";
    /** 解码时先把超大图片降采样到不超过此边长，避免占用过多内存 */
    private static final int DECODE_MAX_EDGE = 2 * MAX_EDGE;

    private ImageUtils() {
    }

    /** 相册返回的 Uri（content:// 或 file://）解码为软件 Bitmap（可直接 compress / createBitmap） */
    @NonNull
    public static Bitmap decode(@NonNull Context context, @NonNull Uri uri) throws IOException {
        return decode(ImageDecoder.createSource(context.getContentResolver(), uri));
    }

    /** 本地文件解码为软件 Bitmap */
    @NonNull
    public static Bitmap decode(@NonNull File file) throws IOException {
        return decode(ImageDecoder.createSource(file));
    }

    private static Bitmap decode(ImageDecoder.Source source) throws IOException {
        return ImageDecoder.decodeBitmap(source, (decoder, info, src) -> {
            decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
            int w = info.getSize().getWidth();
            int h = info.getSize().getHeight();
            int edge = Math.max(w, h);
            if (edge > DECODE_MAX_EDGE) {
                float scale = DECODE_MAX_EDGE / (float) edge;
                decoder.setTargetSize(Math.max(1, Math.round(w * scale)), Math.max(1, Math.round(h * scale)));
            }
        });
    }

    /** 最长边超过 maxEdge 时等比缩小 */
    @NonNull
    public static Bitmap scaleDown(@NonNull Bitmap src, int maxEdge) {
        float scale = Math.min(1f, maxEdge / (float) Math.max(src.getWidth(), src.getHeight()));
        return scale < 1f
                ? Bitmap.createScaledBitmap(src, Math.round(src.getWidth() * scale),
                Math.round(src.getHeight() * scale), true)
                : src;
    }

    /** 缩放并压缩为 JPEG 字节 */
    @NonNull
    public static byte[] compress(@NonNull Bitmap src, int maxEdge, int quality) {
        Bitmap bmp = scaleDown(src, maxEdge);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, quality, out);
        return out.toByteArray();
    }

    /** 压缩并转成 data URL（1280px、质量 80） */
    @NonNull
    public static String toDataUrl(@NonNull Bitmap src, int maxEdge, int quality) {
        return toDataUrl(compress(src, maxEdge, quality));
    }

    /** JPEG 字节转 data URL（Base64 不换行） */
    @NonNull
    public static String toDataUrl(@NonNull byte[] jpeg) {
        return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(jpeg);
    }

    /** 把 JPEG 字节保存到 filesDir/dir/ 下的新文件 */
    @NonNull
    public static File saveJpeg(@NonNull Context context, @NonNull byte[] jpeg, @NonNull String dir)
            throws IOException {
        File folder = new File(context.getFilesDir(), dir);
        if (!folder.isDirectory() && !folder.mkdirs()) throw new IOException("无法创建目录 " + folder);
        File file = new File(folder, dir + "_" + System.currentTimeMillis() + "_"
                + UUID.randomUUID().toString().substring(0, 8) + ".jpg");
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(jpeg);
        }
        return file;
    }

    /** 压缩（1280px、质量 80）后保存到 filesDir/dir/ */
    @NonNull
    public static File saveJpeg(@NonNull Context context, @NonNull Bitmap bitmap, @NonNull String dir)
            throws IOException {
        return saveJpeg(context, compress(bitmap, MAX_EDGE, QUALITY), dir);
    }

    @NonNull
    public static byte[] readBytes(@NonNull File file) throws IOException {
        return Files.readAllBytes(file.toPath());
    }

    /** 文件的 file:// 地址（App 私有目录路径不含需要转义的字符，直接拼接） */
    @NonNull
    public static String toFileUri(@NonNull File file) {
        return FILE_SCHEME + file.getAbsolutePath();
    }

    /** file:// 地址或绝对路径转为 File；其他地址（content:// 等）返回 null */
    @Nullable
    public static File toFile(@Nullable String pathOrUri) {
        if (pathOrUri == null || pathOrUri.isEmpty()) return null;
        if (pathOrUri.startsWith(FILE_SCHEME)) {
            String p = pathOrUri.substring(FILE_SCHEME.length());
            if (p.startsWith("/android_asset/")) return null;
            return new File(p);
        }
        if (pathOrUri.startsWith("/")) return new File(pathOrUri);
        return null;
    }

    /** file 是否位于 filesDir/dir/ 中（只删除 App 自己保存的照片） */
    public static boolean isInDir(@NonNull Context context, @Nullable File file, @NonNull String dir) {
        if (file == null) return false;
        try {
            String folder = new File(context.getFilesDir(), dir).getCanonicalPath() + File.separator;
            return file.getCanonicalPath().startsWith(folder);
        } catch (IOException e) {
            return false;
        }
    }

    /** 删除 filesDir/dir/ 中的照片（其他位置的文件不删除） */
    public static void deleteInDir(@NonNull Context context, @Nullable String pathOrUri, @NonNull String dir) {
        File f = toFile(pathOrUri);
        if (isInDir(context, f, dir)) {
            //noinspection ResultOfMethodCallIgnored
            f.delete();
        }
    }
}
