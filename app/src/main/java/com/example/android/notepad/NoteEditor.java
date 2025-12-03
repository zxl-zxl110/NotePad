package com.example.android.notepad;

import android.app.Activity;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;

public class NoteEditor extends Activity {
    private Uri mNoteUri; // 当前编辑的笔记URI
    private EditText mContentEditText; // 内容输入框
    private String mSelectedCategory = "默认";
    private TextView mCategoryTextView; // 显示当前分类

    // 背景色数组（与NotesList保持一致，避免越界）
    private int[] mBgColors = {
            Color.WHITE,                  // 索引0
            Color.parseColor("#FFF8E1"),  // 索引1
            Color.parseColor("#E8F5E8"),  // 索引2
            Color.parseColor("#E1F5FE"),  // 索引3
            Color.parseColor("#F3E5F5"),  // 索引4
            Color.parseColor("#FFEBEE")   // 索引5
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.note_editor);

        // 应用背景色（带索引越界保护）
        applyBgColor();

        // 1. 初始化控件
        mContentEditText = findViewById(R.id.note_content);
        mCategoryTextView = findViewById(R.id.tv_category);
        Button btnSave = findViewById(R.id.btn_save);

        // 2. 获取Intent传递的Uri（编辑已有笔记时）
        mNoteUri = getIntent().getData();

        // 3. 初始化分类显示
        mCategoryTextView.setText("分类：" + mSelectedCategory);
        mCategoryTextView.setOnClickListener(v -> showCategoryDialog());

        // 4. 加载已有笔记数据（编辑模式）
        if (mNoteUri != null) {
            loadNoteContent();
            loadNoteCategory();
        }

        // 5. 保存按钮点击事件
        btnSave.setOnClickListener(v -> saveNote());
    }

    // 应用背景色（带索引越界保护，避免崩溃）
    private void applyBgColor() {
        SharedPreferences prefs = getSharedPreferences("NotePrefs", MODE_PRIVATE);
        int colorIndex = prefs.getInt("bg_color_index", 0);

        // 关键：索引越界保护，防止数组访问错误
        if (colorIndex < 0 || colorIndex >= mBgColors.length) {
            colorIndex = 0;
            prefs.edit().putInt("bg_color_index", 0).apply();
        }

        // 设置根布局背景色
        View rootView = findViewById(android.R.id.content);
        if (rootView != null) {
            rootView.setBackgroundColor(mBgColors[colorIndex]);
        }

        // 输入框背景设为白色，提升内容可读性
        if (mContentEditText != null) {
            mContentEditText.setBackgroundColor(Color.WHITE);
        }
    }

    // 加载笔记内容到输入框
    private void loadNoteContent() {
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(
                    mNoteUri,
                    new String[]{NotePad.Notes.COLUMN_NAME_NOTE},
                    null,
                    null,
                    null
            );
            if (cursor != null && cursor.moveToFirst()) {
                String content = cursor.getString(0);
                mContentEditText.setText(content);
            }
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    // 保存笔记（新建或更新）
    private void saveNote() {
        String content = mContentEditText.getText().toString().trim();
        if (content.isEmpty()) {
            Toast.makeText(this, "笔记内容不能为空！", Toast.LENGTH_SHORT).show();
            return;
        }

        ContentValues values = new ContentValues();
        values.put(NotePad.Notes.COLUMN_NAME_NOTE, content);
        values.put(NotePad.Notes.COLUMN_NAME_CATEGORY, mSelectedCategory);
        // 补充标题字段（截取前20字）
        values.put(NotePad.Notes.COLUMN_NAME_TITLE, content.substring(0, Math.min(content.length(), 20)));
        // 更新修改时间戳
        values.put(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, System.currentTimeMillis());

        if (mNoteUri == null) {
            // 新建笔记
            Uri newUri = getContentResolver().insert(NotePad.Notes.CONTENT_URI, values);
            if (newUri != null) {
                Toast.makeText(this, "笔记已保存", Toast.LENGTH_SHORT).show();
            }
        } else {
            // 编辑已有笔记
            int rowsUpdated = getContentResolver().update(mNoteUri, values, null, null);
            if (rowsUpdated > 0) {
                Toast.makeText(this, "笔记已更新", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "更新失败，笔记可能已被删除", Toast.LENGTH_SHORT).show();
            }
        }
        finish(); // 返回列表页
    }

    // 分类选择弹窗
    private void showCategoryDialog() {
        String[] categories = {"默认", "工作", "生活", "学习", "其他"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("选择分类")
                .setItems(categories, (dialog, which) -> {
                    mSelectedCategory = categories[which];
                    mCategoryTextView.setText("分类：" + mSelectedCategory);
                })
                .show();
    }

    // 加载当前笔记的分类
    private void loadNoteCategory() {
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(
                    mNoteUri,
                    new String[]{NotePad.Notes.COLUMN_NAME_CATEGORY},
                    null,
                    null,
                    null
            );
            if (cursor != null && cursor.moveToFirst()) {
                mSelectedCategory = cursor.getString(0);
                mCategoryTextView.setText("分类：" + mSelectedCategory);
            }
        } finally {
            if (cursor != null) cursor.close();
        }
    }
}