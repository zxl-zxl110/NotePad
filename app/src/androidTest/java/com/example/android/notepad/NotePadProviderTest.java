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
    // 数据库版本（已升级到3，支持分类字段）
    private static final int DATABASE_VERSION = 3;
    // 数据库名
    private static final String DATABASE_NAME = "notepad.db";
    // 表名（和契约类一致）
    private static final String TABLE_NOTES = NotePad.Notes.TABLE_NAME;

    // URI匹配器：区分不同的URI请求（多条笔记/单条笔记）
    private static final UriMatcher sUriMatcher;
    private static final int NOTES = 1; // 匹配多条笔记
    private static final int NOTE_ID = 2; // 匹配单条笔记
    private static final int NOTES_LIVE_FOLDER = 3; // 匹配动态文件夹

    // 初始化URI匹配器
    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(NotePad.AUTHORITY, "notes", NOTES);
        sUriMatcher.addURI(NotePad.AUTHORITY, "notes/#", NOTE_ID);
        sUriMatcher.addURI(NotePad.AUTHORITY, "notes/live", NOTES_LIVE_FOLDER);
    }

    // 数据库帮助类实例（移到这里，让整个Provider都能访问）
    private DatabaseHelper mOpenHelper;

    // 数据库帮助类：创建和升级数据库（内部类，仅负责数据库管理）
    private static class DatabaseHelper extends SQLiteOpenHelper {
        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        // 创建表（包含新增的category字段）
        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + TABLE_NOTES + " ("
                    + NotePad.Notes._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + NotePad.Notes.COLUMN_NAME_TITLE + " TEXT,"
                    + NotePad.Notes.COLUMN_NAME_NOTE + " TEXT,"
                    + NotePad.Notes.COLUMN_NAME_CREATE_DATE + " INTEGER,"
                    + NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE + " INTEGER,"
                    + NotePad.Notes.COLUMN_NAME_CATEGORY + " TEXT DEFAULT '默认'" // 分类字段，默认"默认"
                    + ");");
        }

        // 升级数据库（添加category字段）
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // 仅当旧版本 < 3 时执行升级（从版本2升到3）
            if (oldVersion < 3) {
                db.execSQL("ALTER TABLE " + TABLE_NOTES + " ADD COLUMN "
                        + NotePad.Notes.COLUMN_NAME_CATEGORY + " TEXT DEFAULT '默认'");
            }
        }
    }

    // 以下方法移到NotePadProvider类的直接作用域下（不再嵌套在DatabaseHelper中）

    // 初始化Provider，创建数据库帮助类
    @Override
    public boolean onCreate() {
        mOpenHelper = new DatabaseHelper(getContext());
        return true;
    }

    // 查询数据
    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(TABLE_NOTES);

        // 根据URI匹配结果设置查询条件
        switch (sUriMatcher.match(uri)) {
            case NOTE_ID:
                // 单条笔记：WHERE _id = ?
                qb.appendWhere(NotePad.Notes._ID + "=" + uri.getPathSegments().get(1));
                break;
            case NOTES_LIVE_FOLDER:
                // 动态文件夹：只查标题和ID
                projection = new String[]{
                        NotePad.Notes._ID,
                        NotePad.Notes.COLUMN_NAME_TITLE
                };
                break;
            default:
                break;
        }

        // 设置默认排序
        String orderBy;
        if (TextUtils.isEmpty(sortOrder)) {
            orderBy = NotePad.Notes.DEFAULT_SORT_ORDER;
        } else {
            orderBy = sortOrder;
        }

        // 执行查询
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, orderBy);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    // 获取MIME类型
    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case NOTES:
            case NOTES_LIVE_FOLDER:
                return NotePad.Notes.CONTENT_TYPE;
            case NOTE_ID:
                return NotePad.Notes.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    // 插入数据（新建笔记）
    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        // 只允许插入到笔记表
        if (sUriMatcher.match(uri) != NOTES) {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        ContentValues values = (initialValues != null) ? new ContentValues(initialValues) : new ContentValues();

        // 设置默认值（创建时间、修改时间、标题、内容、分类）
        Long now = System.currentTimeMillis();
        if (!values.containsKey(NotePad.Notes.COLUMN_NAME_CREATE_DATE)) {
            values.put(NotePad.Notes.COLUMN_NAME_CREATE_DATE, now);
        }
        if (!values.containsKey(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE)) {
            values.put(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, now);
        }
        if (!values.containsKey(NotePad.Notes.COLUMN_NAME_TITLE)) {
            values.put(NotePad.Notes.COLUMN_NAME_TITLE, "");
        }
        if (!values.containsKey(NotePad.Notes.COLUMN_NAME_NOTE)) {
            values.put(NotePad.Notes.COLUMN_NAME_NOTE, "");
        }
        // 确保分类有默认值（如果未设置）
        if (!values.containsKey(NotePad.Notes.COLUMN_NAME_CATEGORY)) {
            values.put(NotePad.Notes.COLUMN_NAME_CATEGORY, "默认");
        }

        // 执行插入
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        long rowId = db.insert(TABLE_NOTES, NotePad.Notes.COLUMN_NAME_TITLE, values);
        if (rowId > 0) {
            Uri noteUri = ContentUris.withAppendedId(NotePad.Notes.CONTENT_URI, rowId);
            getContext().getContentResolver().notifyChange(noteUri, null);
            return noteUri;
        }

        throw new SQLException("Failed to insert row into " + uri);
    }

    // 删除数据
    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;

        switch (sUriMatcher.match(uri)) {
            case NOTES:
                // 删除所有笔记
                count = db.delete(TABLE_NOTES, where, whereArgs);
                break;
            case NOTE_ID:
                // 删除单条笔记：WHERE _id = ?
                String noteId = uri.getPathSegments().get(1);
                count = db.delete(TABLE_NOTES, NotePad.Notes._ID + "=" + noteId
                        + (!TextUtils.isEmpty(where) ? " AND (" + where + ")" : ""), whereArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    // 更新数据
    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;

        // 更新时自动更新修改时间
        if (values != null) {
            values.put(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, System.currentTimeMillis());
        }

        switch (sUriMatcher.match(uri)) {
            case NOTES:
                // 更新所有笔记
                count = db.update(TABLE_NOTES, values, where, whereArgs);
                break;
            case NOTE_ID:
                // 更新单条笔记：WHERE _id = ?
                String noteId = uri.getPathSegments().get(1);
                count = db.update(TABLE_NOTES, values, NotePad.Notes._ID + "=" + noteId
                        + (!TextUtils.isEmpty(where) ? " AND (" + where + ")" : ""), whereArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }
}