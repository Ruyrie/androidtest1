package com.heban.app.data.remote.llm.dto;

import com.google.gson.annotations.SerializedName;

/** 多模态消息中的一段内容：文字或图片（Base64 data URL）。 */
public class ContentPart {
    public static final String TYPE_TEXT = "text";
    public static final String TYPE_IMAGE_URL = "image_url";

    /** "text" 或 "image_url" */
    public String type;
    public String text;
    @SerializedName("image_url") public ImageUrl imageUrl;

    public static class ImageUrl {
        public String url;

        public ImageUrl(String url) {
            this.url = url;
        }
    }

    public static ContentPart text(String text) {
        ContentPart p = new ContentPart();
        p.type = TYPE_TEXT;
        p.text = text;
        return p;
    }

    public static ContentPart image(String dataUrl) {
        ContentPart p = new ContentPart();
        p.type = TYPE_IMAGE_URL;
        p.imageUrl = new ImageUrl(dataUrl);
        return p;
    }
}
