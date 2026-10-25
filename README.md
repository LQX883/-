# LibraryWeb2026 智慧图书馆管理系统

基于 **Servlet + JDBC** 的一体化图书馆管理系统，前端使用 **HTML + CSS + jQuery + Axios + Vue 3**（不使用 JSP），采用「静态页面 + Ajax 调用 Servlet」的非前后端分离方式，数据库使用 **MySQL 8**，数据访问复用作业1的 **JDBCTool**。

---

## 一、技术栈

| 层次 | 技术 |
| --- | --- |
| 前端 | HTML5、CSS3、jQuery 3.7、Axios 1.7、Vue 3（本地引入，无 CDN 依赖） |
| 后端 | Servlet 4.0（javax.servlet）、JDBC、反射 ORM（JDBCTool） |
| 数据库 | MySQL 8.4 |
| 构建 | Maven（war 打包） |
| 服务器 | Apache Tomcat 9.0.123 |
| JDK | JDK 21（编译目标 17） |

> 说明：项目严格采用 Servlet + JDBC，未使用 Spring Boot、MyBatis 等框架。

---

## 二、功能清单

### 1. 用户功能（必做）

- **用户注册**：输入姓名、密码、手机号（11 位校验）、专业、角色；选择角色后动态显示不同信息项：
  - 管理员：权限等级、入职时间
  - 操作员：负责区域、职务
  - 学生：年级、所属学院
  - 教师：所属学院、职称
  - 同名账号不能重复注册，手机号不能重复。
- **用户登录**：用户名 + 密码 + 图形验证码；支持重置；失败显示异常信息。
- **首页**：显示当前用户姓名、角色、专业；根据角色显示不同功能卡片与菜单。
- **用户信息查询**（管理员）：关键字模糊查询（姓名/角色/手机号/专业），按创建时间倒序；默认不显示数据，查询后显示；每行「查看」弹窗展示详情。

### 2. 图书功能（必做）

- **添加图书**：保存后按 `stock` 自动生成副本，新书显示在列表第一行，可反复添加。
- **查询图书**：关键字模糊查询；关键字为空查询全部。
- **废弃图书**：副本全部归还后才允许废弃；废弃后状态改为「已废弃」，保留借阅记录。
- **编辑图书**：仅更新书名、作者、类型、出版社、还书期限、价格（不改库存、副本、状态）。
- **图书副本编辑**：新增副本库存 +1；删除副本库存 -1；借出状态的副本不能删除。
- **借书**：查看可借图书与个人借阅记录，按图书选择借阅，借阅后立即出现在借阅记录。
- **还书**：查看未还记录，按借阅记录归还，归还后从待还列表消失。

### 3. 查询与统计（选做，已实现）

- **借阅记录查询**（管理员）：关键字查询、超期查询、日期范围查询。
- **借阅数据统计**（管理员）：按图书类型、按用户专业统计借阅次数（全部类型均显示）。
- **个人借阅查询**：全部借阅、未还借阅、日期范围查询。
- **推荐图书**（教师）：若馆藏已有同名书籍则提示；否则提交审核。
- **审阅推荐**（管理员）：通过则加入图书列表；驳回必须填写理由。

---

## 三、目录结构

```
LibraryWeb2026
├── pom.xml
├── README.md
├── sql/
│   └── libraryadmin.sql              # 由数据库备份还原脚本（便于复现）
└── src/main
    ├── java/edu/chenjin
    │   ├── jdbc/                     # 作业1 JDBCTool（增强继承字段支持）
    │   │   ├── annotation/           # @Table @Column @Id @Transient
    │   │   └── util/                 # DBUtil、JDBCTool
    │   └── library
    │       ├── entity/               # Base Book BookCopy User Admin Operator Student Teacher Loan BookRecommend
    │       ├── dao/                  # UserDao BookDao BookCopyDao LoanDao RecommendDao Sql
    │       ├── util/                 # Result JsonUtil DtoUtil CaptchaUtil IdUtil SessionUser
    │       └── web/                  # Servlet 与 EncodingFilter、BaseServlet
    ├── resources/
    │   └── db.properties             # 数据库连接配置
    └── webapp/
        ├── index.html                # 跳转登录
        ├── login.html                # 登录页（Vue + Axios + jQuery）
        ├── register.html             # 注册页（角色动态表单）
        ├── home.html                 # 首页（工作台 + 全部功能）
        ├── css/style.css
        ├── js/                       # api.js common.js home.js
        ├── lib/                      # vue / axios / jquery 本地文件
        └── WEB-INF/web.xml
```

---

## 四、环境变量配置（JDK + Tomcat + Maven）

本项目在本机使用的安装路径（可按实际修改）：

| 变量 | 值 |
| --- | --- |
| `JAVA_HOME` | `C:\Program Files\Java\jdk-21` |
| `CATALINA_HOME` | `E:\java\tomcat\apache-tomcat-9.0.123` |
| `MAVEN_HOME` | `E:\java\maven` |
| `Path` 追加 | `%JAVA_HOME%\bin`、`%CATALINA_HOME%\bin`、`%MAVEN_HOME%\bin` |

图形界面设置：`此电脑 → 属性 → 高级系统设置 → 环境变量`，在「用户变量」中新建上述变量，并编辑 `Path` 追加三条路径。

命令行设置（管理员或当前用户，重新打开终端生效）：

```bat
setx JAVA_HOME "C:\Program Files\Java\jdk-21"
setx CATALINA_HOME "E:\java\tomcat\apache-tomcat-9.0.123"
setx MAVEN_HOME "E:\java\maven"
```

验证：

```bat
java -version
mvn -version
catalina.bat version
```

---

## 五、数据库还原

作业提供的备份文件为 Navicat 备份 `LibraryDB.nb3`（本质是 tar 包，内含各表的 gzip 压缩 SQL）。本仓库已将备份还原为可直接导入的 `sql/libraryadmin.sql`。

### 方式一：使用 Navicat 还原（推荐，符合作业要求）

1. 打开 Navicat，连接到本机 MySQL。
2. 右键连接 →「还原备份」→ 选择 `LibraryDB.nb3` → 还原，得到数据库 `libraryadmin`。

### 方式二：使用 SQL 脚本导入

```bat
mysql -uroot -p --default-character-set=utf8mb4 < sql\libraryadmin.sql
```

还原后数据库包含：`user`（17 条）、`book`（54 条）、`bookcopy`（167 条）、`loan`（15 条）等表。

### 修改数据库连接

编辑 `src/main/resources/db.properties`，改成你自己的 MySQL 账号密码：

```properties
jdbc.driver=com.mysql.cj.jdbc.Driver
jdbc.url=jdbc:mysql://localhost:3306/libraryadmin?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&characterEncoding=utf8&useUnicode=true
jdbc.user=root
jdbc.password=你的密码
```

> 注意：`db.properties` 中包含本机数据库密码，上传到公开仓库前建议修改密码或改用专用数据库账号。

---

## 六、构建与部署

```bat
cd /d "E:\java web\LibraryWeb2026"
mvn clean package
```

生成 `target\LibraryWeb2026.war`，复制到 Tomcat：

```bat
copy target\LibraryWeb2026.war "%CATALINA_HOME%\webapps\"
"%CATALINA_HOME%\bin\startup.bat"
```

浏览器访问：

```
http://localhost:8080/LibraryWeb2026/
```

停止 Tomcat：

```bat
"%CATALINA_HOME%\bin\shutdown.bat"
```

---

## 七、局域网访问（让自己的电脑成为 Web 服务器）

1. 查看本机局域网 IP：

```bat
ipconfig
```

找到「无线局域网适配器 / 以太网」下的 IPv4 地址，例如 `192.168.1.100`。

2. 确保 Tomcat 已启动，Windows 防火墙放行 8080 端口（管理员 PowerShell）：

```powershell
New-NetFirewallRule -DisplayName "Tomcat 8080" -Direction Inbound -Protocol TCP -LocalPort 8080 -Action Allow
```

3. 同一局域网内的手机 / 电脑访问：

```
http://192.168.1.100:8080/LibraryWeb2026/
```

> 若仍无法访问，检查是否处于同一网段、是否开启了网络隔离（部分校园网/公共 WiFi 会隔离设备）。

---

## 八、默认账号（来自备份数据）

| 角色 | 用户名 | 密码 |
| --- | --- | --- |
| 管理员 | `admin` | `admin123` |
| 管理员 | `admin2` | `admin456` |
| 操作员 | `operator` | `operator123` |
| 学生 | `student1` | `student123` |
| 学生 | `student2` | `student123` |
| 教师 | `teacher` | `teacher123` |

> 其他账号（李四、王勇、陈赫等）密码多为 `123` 或 `123456`，可在「用户信息查询」中查看基本信息。

---

## 九、接口一览

| 方法 | 路径 | 说明 | 角色 |
| --- | --- | --- | --- |
| GET | `/api/captcha` | 图形验证码 | 匿名 |
| POST | `/api/login` | 登录 | 匿名 |
| POST | `/api/register` | 注册 | 匿名 |
| POST | `/api/logout` | 退出 | 登录 |
| GET | `/api/me` | 当前用户与权限 | 登录 |
| GET | `/api/users/search` | 用户模糊查询 | 管理员 |
| GET | `/api/users/detail` | 用户详情 | 管理员 |
| GET | `/api/books` | 图书列表/查询 | 登录 |
| GET | `/api/books/borrowable` | 可借图书 | 登录 |
| POST | `/api/books/save` | 新增图书 | 操作员 |
| PUT | `/api/books/save` | 编辑图书 | 操作员 |
| POST | `/api/books/discard` | 废弃图书 | 操作员 |
| GET/POST/DELETE | `/api/bookcopies` | 副本查看/新增/删除 | 操作员 |
| POST | `/api/borrow` | 借书 | 登录 |
| POST | `/api/return` | 还书 | 登录 |
| GET | `/api/loans/my` | 个人借阅 | 登录 |
| GET | `/api/loans/search` | 借阅记录查询 | 管理员 |
| GET | `/api/loans/stats` | 借阅统计 | 管理员 |
| GET/POST | `/api/recommends` | 推荐列表/提交 | 登录/教师 |
| POST | `/api/recommends/review` | 审阅推荐 | 管理员 |

统一响应格式：

```json
{ "code": 200, "message": "操作成功", "data": {} }
```

`code`：200 成功；400 参数/业务错误；401 未登录；403 无权限；500 服务器异常。

---

## 十、上传到 Gitee

1. 在 Gitee 新建空仓库（不要勾选初始化 README）。
2. 在项目根目录执行：

```bat
git init
git add .
git commit -m "feat: 完成图书馆管理系统用户、图书、借阅、推荐与统计功能"
git branch -M master
git remote add origin https://gitee.com/你的用户名/你的仓库.git
git push -u origin master
```

3. 把仓库地址填入作业答案，例如：

```
https://gitee.com/你的用户名/LibraryWeb2026
```

> 若希望体现「每完成一个功能就 push」，可按功能拆分多次提交：
> `feat: 用户注册登录与用户查询` → `feat: 图书增删改查与副本管理` → `feat: 借还书与借阅查询统计` → `feat: 图书推荐与审阅`。

---

## 十一、录屏建议（作业要求）

建议按以下顺序录制，覆盖全部必做与选做功能：

1. 展示环境变量（`java -version`、`mvn -version`、`catalina.bat version`）。
2. 启动 Tomcat，浏览器访问本机地址登录。
3. 注册一个新用户（演示角色动态字段、手机号校验、同名/重复手机号提示）。
4. 登录后展示首页角色功能卡片。
5. 管理员：用户信息查询、借阅记录查询（关键字/超期/日期）、借阅统计、审阅推荐。
6. 操作员：添加图书（自动生成副本）、查询、编辑、废弃、副本新增/删除。
7. 学生/教师：借书、还书、个人借阅查询；教师推荐图书。
8. 用局域网 IP 在另一台设备访问，展示「本机作为 Web 服务器」。
9. 打开 Gitee 仓库展示提交记录。

---

## 十二、说明

- 项目未使用 JSP，页面均为静态 HTML，通过 Axios 调用 Servlet。
- 借阅状态「超期」由后端在查询时根据 `dueDate` 动态计算并返回。
- `JDBCTool` 在作业1基础上增强了继承字段收集与 `@Transient` 支持，以适配 `Base → User → 子类` 的实体继承结构。
