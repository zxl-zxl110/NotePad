package com.example.android.notepad;

import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.content.Intent;

public class TitleEditor extends Activity {
    private Uri mNoteUri; // 当前笔记的URI
    private EditText mTitleInput; // 标题输入框

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.title_editor);

        // 获取输入框和按钮
        mTitleInput = findViewById(R.id.title_input);
        Button btnSave = findViewById(R.id.btn_save_title);

        // 获取当前笔记的URI（从启动Intent中获取）
        Intent intent = getIntent();
        mNoteUri = intent.getData();
        if (mNoteUri == null) {
            // 如果没有URI，直接关闭（异常情况）
            finish();
            return;
        }

        // 加载当前标题
        loadCurrentTitle();

        // 绑定保存按钮事件
        btnSave.setOnClickListener(v -> saveNewTitle());
    }

    // 加载当前笔记的标题
    private void loadCurrentTitle() {
        Cursor cursor = getContentResolver().query(
                mNoteUri,
                new String[]{NotePad.Notes.COLUMN_NAME_TITLE}, // 只查询标题字段
                null,
                null,
                null
        );

        if (cursor != null && cursor.moveToFirst()) {
            String currentTitle = cursor.getString(0);
            mTitleInput.setText(currentTitle);
            cursor.close();
        }
    }

    // 保存新标题
    private void saveNewTitle() {
        String newTitle = mTitleInput.getText().toString().trim();
        if (newTitle.isEmpty()) {
            Toast.makeText(this, "标题不能为空", Toast.LENGTH_SHORT).show();
            return;
        }

        // 更新标题到数据库
        ContentValues values = new ContentValues();
        values.put(NotePad.Notes.COLUMN_NAME_TITLE, newTitle);

        int rowsUpdated = getContentResolver().update(
                mNoteUri,
                values,
                null,
                null
        );

        if (rowsUpdated > 0) {
            Toast.makeText(this, "标题已更新", Toast.LENGTH_SHORT).show();
            finish(); // 保存后关闭页面
        }
    }
}