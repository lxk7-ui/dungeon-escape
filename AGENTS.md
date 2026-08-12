# Dungeon Escape 项目协作约定

## 自动发布到 GitHub

- 用户要求修改、修复、增加或制作项目功能时，完成实现后必须执行与改动风险相称的构建和检查。
- 检查通过后，自动将本次任务相关文件提交并推送到当前 GitHub 远程仓库；无需等待用户再次提醒“上传”。
- 提交前先检查 `git status` 和 diff。只提交本次任务相关改动，不得静默提交用户的无关修改。
- 如果发现无法明确归属的本地修改，先保留它们；只有发生文件重叠且无法安全拆分时才询问用户。
- 默认直接提交到当前 `main` 分支并推送 `origin/main`，除非用户明确要求分支或 PR 流程。
- Android 功能版本以 `android-app/app/build.gradle` 为准。需要发布新安装包时，递增 `versionCode`，并按语义使用 `versionName`。
- Git 提交名称统一使用：`v{versionName} {简短中文修改名称}`，例如 `v2.1.1 优化按键音效设置`。
- 如果改动只涉及文档、仓库配置或其他不影响应用安装包的内容，可以不升级应用版本；提交名使用清晰中文说明。
- 推送前至少运行 Android `assembleDebug` 和 `lintDebug`；改动桌面 Java 或共享核心逻辑时，还应运行 Maven 测试。
- 构建或检查失败时不得推送，并向用户说明具体阻塞。
- 最终回复必须列出：应用版本（如适用）、提交名称、短提交哈希、检查结果和 GitHub 仓库链接。
- APK、Gradle/Maven 构建缓存、`local.properties`、IDE 临时文件及本机配置始终遵循 `.gitignore`，不得提交。

## 安全

- 不索取或保存 GitHub 密码、令牌及其他凭据。
- 使用本机现有的 GitHub CLI 登录状态和 `origin` 远程仓库。
