package com.example.android.notepad;

import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.content.ContentUris;
import android.net.Uri;
import android.app.AlertDialog;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

public class NotesList extends ListActivity {
    // 数据库查询的字段（标题、时间戳、ID）
    private final String[] PROJECTION = new String[]{
            NotePad.Notes._ID,
            NotePad.Notes.COLUMN_NAME_TITLE,
            NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE
    };

    private SimpleCursorAdapter mAdapter;
    private String mSelectedCategory = "全部";
    private String mSearchKeyword;
    private EditText mEtSearch;
    private Button mBtnSearch;

    // 背景色相关
    private SharedPreferences mPrefs;
    private int[] mBgColors = {
            Color.WHITE,                  // 0
            Color.parseColor("#FFF8E1"),  // 1
            Color.parseColor("#E8F5E8"),  // 2
            Color.parseColor("#E1F5FE"),  // 3
            Color.parseColor("#F3E5F5"),  // 4
            Color.parseColor("#FFEBEE")   // 5
    };
    private String[] mColorNames = {"白色", "浅橙色", "浅绿色", "浅蓝色", "浅紫色", "浅红色"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.notes_list);

        // 1. 延迟初始化所有UI操作（关键：等待窗口完全注册）
        getWindow().getDecorView().post(() -> {
            initViews();
            applyBgColor();
            loadNotes();
        });

        // 2. 提前注册长按菜单（无需等待窗口）
        registerForContextMenu(getListView());
    }

    // 初始化所有控件（延迟执行）
    private void initViews() {
        // 初始化SharedPreferences
        mPrefs = getSharedPreferences("NotePrefs", MODE_PRIVATE);

        // 绑定分类筛选按钮
        Button btnCategory = findViewById(R.id.btn_filter_category);
        if (btnCategory != null) {
            btnCategory.setText("分类：" + mSelectedCategory);
            btnCategory.setOnClickListener(v -> showCategoryFilterDialog());
        }

        // 绑定背景色按钮
        Button btnChangeBg = findViewById(R.id.btn_change_bg);
        if (btnChangeBg != null) {
            btnChangeBg.setOnClickListener(v -> showBgColorDialog());
        }

        // 绑定搜索控件
        mEtSearch = findViewById(R.id.et_search);
        mBtnSearch = findViewById(R.id.btn_search);
        if (mBtnSearch != null) {
            mBtnSearch.setOnClickListener(v -> {
                mSearchKeyword = mEtSearch.getText().toString().trim();
                loadNotes();
                Toast.makeText(this, "搜索：" + mSearchKeyword, Toast.LENGTH_SHORT).show();
            });
        }

        // 绑定清空搜索按钮
        Button btnClearSearch = findViewById(R.id.btn_clear_search);
        if (btnClearSearch != null) {
            btnClearSearch.setOnClickListener(v -> {
                mEtSearch.setText("");
                mSearchKeyword = "";
                loadNotes();
            });
        }

        // 绑定新建笔记按钮
        Button btnAdd = findViewById(R.id.btn_add_note);
        if (btnAdd != null) {
            btnAdd.setOnClickListener(v -> {
                Intent intent = new Intent(NotesList.this, NoteEditor.class);
                intent.putExtra("isNewNote", true);
                startActivity(intent);
            });
        }
    }

    // 显示背景色选择弹窗
    private void showBgColorDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("选择背景色");
        builder.setItems(mColorNames, (dialog, which) -> {
            mPrefs.edit().putInt("bg_color_index", which).apply();
            applyBgColor();
            Toast.makeText(this, "背景色已更换为：" + mColorNames[which], Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("恢复默认", (dialog, which) -> {
            mPrefs.edit().putInt("bg_color_index", 0).apply();
            applyBgColor();
            Toast.makeText(this, "背景色已恢复默认", Toast.LENGTH_SHORT).show();
        });
        builder.show();
    }

    // 安全应用背景色（避免操作未注册的Window）
    private void applyBgColor() {
        try {
            int colorIndex = mPrefs.getInt("bg_color_index", 0);
            // 索引越界保护
            if (colorIndex < 0 || colorIndex >= mBgColors.length) colorIndex = 0;
            int selectedColor = mBgColors[colorIndex];

            // 方案1：优先设置根布局背景（推荐）
            View rootView = findViewById(R.id.root_layout);
            if (rootView != null) {
                rootView.setBackgroundColor(selectedColor);
            }
            // 方案2：降级设置Window背景（避免直接操作contentView）
            else {
                getWindow().setBackgroundDrawable(new ColorDrawable(selectedColor));
            }

            // 列表背景单独设置（避免内容混淆）
            ListView listView = getListView();
            if (listView != null) {
                listView.setBackgroundColor(Color.WHITE);
                listView.setCacheColorHint(Color.WHITE);
            }
        } catch (Exception e) {
            // 捕获异常，防止崩溃
            e.printStackTrace();
        }
    }

    // 加载笔记数据
    private void loadNotes() {
        String selection = null;
        String[] selectionArgs = null;

        // 分类筛选
        if (!mSelectedCategory.equals("全部")) {
            selection = NotePad.Notes.COLUMN_NAME_CATEGORY + " = ?";
            selectionArgs = new String[]{mSelectedCategory};
        }

        // 叠加搜索筛选
        if (mSearchKeyword != null && !mSearchKeyword.isEmpty()) {
            String searchSelection = NotePad.Notes.COLUMN_NAME_TITLE + " LIKE ? OR "
                    + NotePad.Notes.COLUMN_NAME_NOTE + " LIKE ?";
            String[] searchArgs = new String[]{"%" + mSearchKeyword + "%", "%" + mSearchKeyword + "%"};

            if (selection == null) {
                selection = searchSelection;
                selectionArgs = searchArgs;
            } else {
                selection += " AND (" + searchSelection + ")";
                List<String> argsList = new ArrayList<>(Arrays.asList(selectionArgs));
                argsList.addAll(Arrays.asList(searchArgs));
                selectionArgs = argsList.toArray(new String[0]);
            }
        }

        // 执行查询
        Cursor cursor = getContentResolver().query(
                NotePad.Notes.CONTENT_URI,
                PROJECTION,
                selection,
                selectionArgs,
                NotePad.Notes.DEFAULT_SORT_ORDER
        );

        // 设置适配器
        mAdapter = new SimpleCursorAdapter(
                this,
                R.layout.noteslist_item,
                cursor,
                new String[]{NotePad.Notes.COLUMN_NAME_TITLE, NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE},
                new int[]{R.id.note_title, R.id.note_date},
                0
        ) {
            @Override
            public void setViewText(TextView v, String text) {
                if (v.getId() == R.id.note_date) {
                    try {
                        long time = Long.parseLong(text);
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                        text = sdf.format(new Date(time));
                    } catch (NumberFormatException e) {
                        text = "未知时间";
                    }
                }
                super.setViewText(v, text);
            }
        };

        setListAdapter(mAdapter);
    }

    // 点击列表项跳转编辑页面
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Uri noteUri = ContentUris.withAppendedId(NotePad.Notes.CONTENT_URI, id);
        Intent intent = new Intent(NotesList.this, NoteEditor.class);
        intent.setData(noteUri);
        intent.putExtra("isEditMode", true);
        startActivity(intent);
    }

    // 创建长按菜单
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(0, 1, 0, "删除笔记");
        menu.add(0, 2, 0, "编辑笔记");
    }

    // 处理长按菜单点击
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        if (info == null) return super.onContextItemSelected(item);

        long noteId = info.id;
        Uri noteUri = ContentUris.withAppendedId(NotePad.Notes.CONTENT_URI, noteId);

        switch (item.getItemId()) {
            case 1: // 删除笔记
                int rowsDeleted = getContentResolver().delete(noteUri, null, null);
                if (rowsDeleted > 0) {
                    Toast.makeText(this, "笔记已删除", Toast.LENGTH_SHORT).show();
                    loadNotes();
                }
                return true;
            case 2: // 编辑笔记
                Intent intent = new Intent(NotesList.this, NoteEditor.class);
                intent.setData(noteUri);
                startActivity(intent);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    // 回到页面时刷新数据和背景色
    @Override
    protected void onResume() {
        super.onResume();
        // 延迟刷新，避免窗口未就绪
        getWindow().getDecorView().post(() -> {
            loadNotes();
            applyBgColor();
        });
    }

    // 分类筛选弹窗
    private void showCategoryFilterDialog() {
        Cursor categoryCursor = getContentResolver().query(
                NotePad.Notes.CONTENT_URI,
                new String[]{NotePad.Notes.COLUMN_NAME_CATEGORY},
                null,
                null,
                NotePad.Notes.COLUMN_NAME_CATEGORY + " ASC"
        );

        List<String> categories = new ArrayList<>();
        categories.add("全部");
        if (categoryCursor != null) {
            while (categoryCursor.moveToNext()) {
                String category = categoryCursor.getString(0);
                if (!categories.contains(category)) {
                    categories.add(category);
                }
            }
            categoryCursor.close();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("选择分类");
        builder.setItems(categories.toArray(new String[0]), (dialog, which) -> {
            mSelectedCategory = categories.get(which);
            Button btnCategory = findViewById(R.id.btn_filter_category);
            if (btnCategory != null) {
                btnCategory.setText("分类：" + mSelectedCategory);
            }
            loadNotes();
        });
        builder.show();
    }
}