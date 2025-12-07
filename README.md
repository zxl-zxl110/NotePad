NotePad 应用功能介绍
该应用基于 Android 官方早期数据库操作教程扩展开发，主要实现了笔记的基本管理功能，并新增了多项实用特性。以下重点介绍时间戳、搜索查询、分类、背景色更换及 UI 界面的实现方式与效果。

1. 时间戳功能
实现方式
数据库设计：在 NotePad.Notes 表中定义了 COLUMN_NAME_CREATE_DATE（创建时间）和 COLUMN_NAME_MODIFICATION_DATE（修改时间）两个字段，均存储长整型时间戳
数据处理：
新建笔记时自动写入当前时间戳
编辑笔记时更新修改时间戳为当前时间
在列表展示时通过 SimpleCursorAdapter 的 setViewText 方法将时间戳转换为 "yyyy-MM-dd HH:mm" 格式的可读时间
效果
每条笔记在列表中都会显示最后修改时间
时间格式统一，清晰展示笔记的更新记录
列表默认按修改时间倒序排列，最新笔记显示在最上方
https://raw.githubusercontent.com/zxl-zxl110/NotePad/master/image/3b748447-9f21-4b99-915b-7c003366edda.png

2. 搜索查询功能
实现方式
UI 组件：在布局文件中通过 EditText 输入搜索关键词，配合 "搜索" 和 "清空" 两个按钮组成搜索栏
功能逻辑：
在 NotesList 中通过 mSearchKeyword 存储搜索关键词
构建 SQL 查询条件，同时匹配笔记标题（COLUMN_NAME_TITLE）和内容（COLUMN_NAME_NOTE）
使用 LIKE ? 实现模糊查询，关键词前后添加 % 符号
搜索结果实时更新列表展示
效果
支持通过关键词快速定位相关笔记
点击 "清空" 按钮可一键清除搜索条件，恢复显示所有笔记
搜索操作有 Toast 提示当前搜索关键词
https://raw.githubusercontent.com/zxl-zxl110/NotePad/master/image/b5d1f56a-5819-4af2-9759-070e1181e145.png
https://raw.githubusercontent.com/zxl-zxl110/NotePad/master/image/be5382c7-6d02-4cec-87d9-9d74314378de.png

3. 分类功能
实现方式
数据库设计：在笔记表中添加 COLUMN_NAME_CATEGORY 字段存储分类信息
分类选项：预设 "默认"、"工作"、"生活"、"学习"、"其他" 五个分类
功能逻辑：
在 NoteEditor 中通过弹窗选择笔记分类
在 NotesList 中通过 "分类：全部" 按钮打开分类筛选弹窗
筛选时通过 selection 条件过滤对应分类的笔记
效果
新建和编辑笔记时可指定分类
列表页可按分类筛选显示内容
分类按钮实时显示当前筛选状态（如 "分类：工作"）
https://raw.githubusercontent.com/zxl-zxl110/NotePad/master
https://raw.githubusercontent.com/zxl-zxl110/NotePad/master
https://raw.githubusercontent.com/zxl-zxl110/NotePad/master

4. 更换背景色功能
实现方式
颜色定义：在 NotesList 和 NoteEditor 中统一定义了 6 种背景色数组（白色、浅橙色、浅绿色、浅蓝色、浅紫色、浅红色）
偏好存储：使用 SharedPreferences 保存用户选择的背景色索引
功能逻辑：
通过 "更换背景色" 按钮打开颜色选择弹窗
选择后更新偏好设置并调用 applyBgColor() 方法应用颜色
同时在 NotesList 和 NoteEditor 中保持背景色一致
效果
支持一键切换应用背景色，提升使用体验
列表内容区域保持白色背景，保证文字可读性
切换时有 Toast 提示当前选择的背景色
支持恢复默认背景色（白色）
https://raw.githubusercontent.com/zxl-zxl110/NotePad/master
https://raw.githubusercontent.com/zxl-zxl110/NotePad/master
https://raw.githubusercontent.com/zxl-zxl110/NotePad/master

5. UI 界面实现
布局结构
主布局（notes_list.xml）采用垂直 LinearLayout 结构，包含：
搜索栏：输入框 + 搜索按钮 + 清空按钮
功能按钮栏：分类筛选按钮 + 更换背景色按钮
新建笔记按钮：全屏宽度的突出显示按钮
笔记列表：使用 ListView 展示笔记条目
列表项布局（noteslist_item.xml）采用垂直布局，包含：
笔记标题（加粗显示）
笔记修改时间（灰色小字）
界面特点
色彩搭配：使用不同颜色区分功能按钮（搜索：紫色、分类：蓝色、背景色：绿色、新建：橙色）
响应式设计：按钮使用 layout_weight 实现自适应布局
交互反馈：所有按钮操作均有明确的视觉或 Toast 提示
一致性：列表页和编辑页保持相同的背景色设置，保证视觉统一
通过以上功能的实现，NotePad 应用提供了简洁易用的笔记管理体验，同时支持个性化定制和高效的内容检索。

（备注，更改分类及删除功能通过左键长按记事本条目出现'删除'及'分类'按钮实现。本项目运行使用虚拟机Pixel 3）
