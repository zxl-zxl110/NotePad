package com.example.android.notepad;

import android.net.Uri;
import android.provider.BaseColumns;

// 记事本数据的契约类，定义表结构和URI
public final class NotePad {
    // 禁止实例化（构造方法私有化）
    private NotePad() {}

    // 内容提供者的Authority（类似身份证号，唯一标识）
    public static final String AUTHORITY = "com.example.android.notepad";

    // 定义笔记表的结构
    public static final class Notes implements BaseColumns {
        // 禁止实例化
        private Notes() {}

        // 表名
        public static final String TABLE_NAME = "notes";

        // 内容URI：访问笔记表的总入口
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/notes");

        // 动态文件夹的URI
        public static final Uri LIVE_FOLDER_URI = Uri.parse("content://" + AUTHORITY + "/notes/live");

        // 单条笔记的URI模式（带ID）
        public static final Uri CONTENT_ID_URI_PATTERN = Uri.parse("content://" + AUTHORITY + "/notes/#");

        // MIME类型：多条笔记（目录）
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.example.note";

        // MIME类型：单条笔记（ item）
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.example.note";

        // 默认排序：按修改时间倒序
        public static final String DEFAULT_SORT_ORDER = "modified DESC";

        // 表字段定义
        public static final String COLUMN_NAME_TITLE = "title"; // 标题
        public static final String COLUMN_NAME_NOTE = "note";   // 内容
        public static final String COLUMN_NAME_CATEGORY = "category";//分类
        public static final String COLUMN_NAME_CREATE_DATE = "created"; // 创建时间
        public static final String COLUMN_NAME_MODIFICATION_DATE = "modified"; // 修改时间
    }
}