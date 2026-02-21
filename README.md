桌面常驻小挂件（悬浮在屏幕右下角，始终置顶），点击后弹出笔记主窗口
笔记主窗口分三栏：左侧文件列表、中间编辑区、右侧 Markdown 实时预览
支持打开本地 Markdown 文件（.md 格式）
支持临时随手记，内容可保存到本地
Markdown 实时预览（编辑时右侧同步渲染）。

鼠标移到挂件上即显示主窗口。
鼠标离开后若无后续操作会自动隐藏。
一旦在主窗口有操作（点击/键盘），窗口会保留，不自动收起。

项目结构：
desktop-note-widget/
├─ pom.xml
└─ src/
   └─ main/
      ├─ java/
      │  └─ com/example/notewidget/
      │     ├─ NoteWidgetApp.java
      │     ├─ controller/
      │     │  ├─ MainController.java
      │     │  └─ WidgetController.java
      │     ├─ model/
      │     │  └─ NoteDocument.java
      │     ├─ service/
      │     │  ├─ MarkdownService.java
      │     │  └─ NoteStorageService.java
      │     └─ util/
      │        └─ AppPaths.java
      └─ resources/
         ├─ css/
         │  ├─ app.css
         │  └─ widget.css
         └─ fxml/
            ├─ main-view.fxml
            └─ widget-view.fxml
