package com.heban.app.data.model;

/** S08 顶部四个入口的未读数与私信未读总数；H01 / S01 铃铛和信封角标用 total()。 */
public final class MessageCounts {
    public int comment;
    public int like;
    public int follow;
    public int system;
    public int directMessages;

    public int total() {
        return comment + like + follow + system + directMessages;
    }
}
