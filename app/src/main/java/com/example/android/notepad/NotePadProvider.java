package com.example.android.notepad;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

public class NotePadProvider extends ContentProvider {
    // 数据库版本（支持分类字段的最新版本）
    private static final int DATABASE_VERSION = 3;
    // 数据库名
    private static final String DATABASE_NAME = "notepad.db";
    // 表名（与契约类一致）
    private static final String TABLE_NOTES = NotePad.Notes.TABLE_NAME;

    // URI匹配器常量
    private static final int NOTES = 1;               // 匹配多条笔记
    private static final int NOTE_ID = 2;             // 匹配单条笔记
    private static final int NOTES_LIVE_FOLDER = 3;   // 匹配动态文件夹

    // 初始化URI匹配器
    private static final UriMatcher sUriMatcher;
    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(NotePad.AUTHORITY, "notes", NOTES);
        sUriMatcher.addURI(NotePad.AUTHORITY, "notes/#", NOTE_ID);
        sUriMatcher.addURI(NotePad.AUTHORITY, "notes/live", NOTES_LIVE_FOLDER);
    }

    // 数据库帮助类实例
    private DatabaseHelper mOpenHelper;

    /**
     * 数据库帮助类：负责数据库创建和升级
     */
    private static class DatabaseHelper extends SQLiteOpenHelper {
        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        /**
         * 创建数据库表（包含所有字段：标题、内容、创建时间、修改时间、分类）
         */
        @Override
        public void onCreate(SQLiteDatabase db) {
            final String SQL_CREATE_NOTES_TABLE = "CREATE TABLE " + TABLE_NOTES + " ("
                    + NotePad.Notes._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + NotePad.Notes.COLUMN_NAME_TITLE + " TEXT NOT NULL DEFAULT '',"
                    + NotePad.Notes.COLUMN_NAME_NOTE + " TEXT NOT NULL DEFAULT '',"
                    + NotePad.Notes.COLUMN_NAME_CREATE_DATE + " INTEGER NOT NULL,"
                    + NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE + " INTEGER NOT NULL,"
                    + NotePad.Notes.COLUMN_NAME_CATEGORY + " TEXT NOT NULL DEFAULT '默认'"
                    + ");";
            db.execSQL(SQL_CREATE_NOTES_TABLE);
        }

        /**
         * 数据库升级逻辑（支持从旧版本平滑升级）
         */
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // 版本1→2：添加修改时间字段
            if (oldVersion < 2) {
                db.execSQL("ALTER TABLE " + TABLE_NOTES + " ADD COLUMN "
                        + NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE + " INTEGER NOT NULL DEFAULT 0");
            }
            // 版本2→3：添加分类字段
            if (oldVersion < 3) {
                db.execSQL("ALTER TABLE " + TABLE_NOTES + " ADD COLUMN "
                        + NotePad.Notes.COLUMN_NAME_CATEGORY + " TEXT NOT NULL DEFAULT '默认'");
            }
        }

        /**
         * 数据库降级逻辑（可选）
         */
        @Override
        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            onUpgrade(db, newVersion, oldVersion);
        }
    }

    /**
     * 初始化Provider，创建数据库帮助类
     */
    @Override
    public boolean onCreate() {
        mOpenHelper = new DatabaseHelper(getContext());
        return true;
    }

    /**
     * 查询数据（支持多条件筛选、排序）
     */
    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        queryBuilder.setTables(TABLE_NOTES);

        // 根据URI设置查询条件
        switch (sUriMatcher.match(uri)) {
            case NOTE_ID:
                // 查询单条笔记：WHERE _id = ?
                queryBuilder.appendWhere(NotePad.Notes._ID + "=" + uri.getLastPathSegment());
                break;
            case NOTES_LIVE_FOLDER:
                // 动态文件夹：只返回标题和ID
                projection = new String[]{
                        NotePad.Notes._ID,
                        NotePad.Notes.COLUMN_NAME_TITLE
                };
                break;
            case NOTES:
                // 查询所有笔记，使用默认条件
                break;
            default:
                throw new IllegalArgumentException("未知URI: " + uri);
        }

        // 设置默认排序方式
        String orderBy = TextUtils.isEmpty(sortOrder) ? NotePad.Notes.DEFAULT_SORT_ORDER : sortOrder;

        // 执行查询并设置通知URI
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        Cursor cursor = queryBuilder.query(db, projection, selection, selectionArgs,
                null, null, orderBy);
        if (getContext() != null) {
            cursor.setNotificationUri(getContext().getContentResolver(), uri);
        }

        return cursor;
    }

    /**
     * 获取数据MIME类型
     */
    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case NOTES:
            case NOTES_LIVE_FOLDER:
                return NotePad.Notes.CONTENT_TYPE;
            case NOTE_ID:
                return NotePad.Notes.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalArgumentException("未知URI: " + uri);
        }
    }

    /**
     * 插入数据（自动补充默认值）
     */
    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        // 验证URI合法性
        if (sUriMatcher.match(uri) != NOTES) {
            throw new IllegalArgumentException("插入操作仅支持notes URI: " + uri);
        }

        ContentValues values = initialValues != null ? new ContentValues(initialValues) : new ContentValues();

        // 补充默认值
        long currentTime = System.currentTimeMillis();
        if (!values.containsKey(NotePad.Notes.COLUMN_NAME_CREATE_DATE)) {
            values.put(NotePad.Notes.COLUMN_NAME_CREATE_DATE, currentTime);
        }
        if (!values.containsKey(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE)) {
            values.put(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, currentTime);
        }
        if (!values.containsKey(NotePad.Notes.COLUMN_NAME_TITLE)) {
            values.put(NotePad.Notes.COLUMN_NAME_TITLE, "未命名笔记");
        }
        if (!values.containsKey(NotePad.Notes.COLUMN_NAME_NOTE)) {
            values.put(NotePad.Notes.COLUMN_NAME_NOTE, "");
        }
        if (!values.containsKey(NotePad.Notes.COLUMN_NAME_CATEGORY)) {
            values.put(NotePad.Notes.COLUMN_NAME_CATEGORY, "默认");
        }

        // 执行插入
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        long rowId = db.insert(TABLE_NOTES, null, values);
        if (rowId <= 0) {
            throw new SQLException("插入失败: " + uri);
        }

        // 生成新笔记URI并通知数据变化
        Uri noteUri = ContentUris.withAppendedId(NotePad.Notes.CONTENT_URI, rowId);
        if (getContext() != null) {
            getContext().getContentResolver().notifyChange(noteUri, null);
        }

        return noteUri;
    }

    /**
     * 删除数据（支持单条/批量删除）
     */
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int affectedRows;

        switch (sUriMatcher.match(uri)) {
            case NOTES:
                // 删除所有符合条件的笔记
                affectedRows = db.delete(TABLE_NOTES, selection, selectionArgs);
                break;
            case NOTE_ID:
                // 删除单条笔记：WHERE _id = ?
                String noteId = uri.getLastPathSegment();
                String whereClause = NotePad.Notes._ID + "=" + noteId
                        + (TextUtils.isEmpty(selection) ? "" : " AND (" + selection + ")");
                affectedRows = db.delete(TABLE_NOTES, whereClause, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("删除操作仅支持notes URI: " + uri);
        }

        // 通知数据变化
        if (affectedRows > 0 && getContext() != null) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return affectedRows;
    }

    /**
     * 更新数据（自动更新修改时间）
     */
    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        if (values == null) {
            throw new IllegalArgumentException("更新数据不能为空");
        }

        // 自动更新修改时间
        values.put(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, System.currentTimeMillis());

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int affectedRows;

        switch (sUriMatcher.match(uri)) {
            case NOTES:
                // 更新所有符合条件的笔记
                affectedRows = db.update(TABLE_NOTES, values, selection, selectionArgs);
                break;
            case NOTE_ID:
                // 更新单条笔记：WHERE _id = ?
                String noteId = uri.getLastPathSegment();
                String whereClause = NotePad.Notes._ID + "=" + noteId
                        + (TextUtils.isEmpty(selection) ? "" : " AND (" + selection + ")");
                affectedRows = db.update(TABLE_NOTES, values, whereClause, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("更新操作仅支持notes URI: " + uri);
        }

        // 通知数据变化
        if (affectedRows > 0 && getContext() != null) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return affectedRows;
    }
}