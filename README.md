# yudao-ureport2

yudao-ureport2 是 [UReport2](https://github.com/youseries/ureport) 2.2.9 的社区维护版本，不是官方发布。它让 UReport2 可以直接运行在新版 Spring Boot 上，同时保留原有的 XML 报表模板格式、Web 设计器和扩展接口签名（`ReportProvider`、`BuildinDatasource`、`Function` 等），已有模板和扩展可以继续使用。自定义扩展如果依赖旧版 Servlet、Spring 或 POI API，可能需要按目标分支做适配。

Java 包名仍是 `com.bstek.ureport.*`，以便兼容原模板和扩展实现。

## 仓库与分支

| 平台 | 仓库地址 |
| --- | --- |
| Gitee（主仓库） | <https://gitee.com/yudaocode/yudao-ureport2> |
| GitHub | <https://github.com/yudaocode/yudao-ureport2> |

两个仓库内容相同。源码按 Spring Boot 大版本分成三个分支：

| 分支 | 适用宿主 | 维护版本 | Gitee | GitHub |
| --- | --- | --- | --- | --- |
| `master` | JDK 8 + Spring Boot 2.7 | `2.0.0-SNAPSHOT` | [`master`](https://gitee.com/yudaocode/yudao-ureport2/tree/master/) | [`master`](https://github.com/yudaocode/yudao-ureport2/tree/master) |
| `master-jdk17` | JDK 17 + Spring Boot 3.5 | `3.0.0-SNAPSHOT` | [`master-jdk17`](https://gitee.com/yudaocode/yudao-ureport2/tree/master-jdk17/) | [`master-jdk17`](https://github.com/yudaocode/yudao-ureport2/tree/master-jdk17) |
| `master-jdk25` | JDK 25 + Spring Boot 4.1 | `4.0.0-SNAPSHOT` | [`master-jdk25`](https://gitee.com/yudaocode/yudao-ureport2/tree/master-jdk25/) | [`master-jdk25`](https://github.com/yudaocode/yudao-ureport2/tree/master-jdk25) |

各分支已验证的精确版本见下面的“版本线”。

## 关于 UReport2

UReport2 是一款架构在 Spring 之上的高性能纯 Java 报表引擎，通过迭代单元格可以实现任意复杂的中国式报表。它提供基于网页的报表设计器，可以在 Chrome、Firefox、Edge 等主流浏览器中运行（IE 除外），打开浏览器即可完成复杂报表的设计制作。UReport2 是第一款基于 Apache-2.0 协议开源的中式报表引擎。

主要功能：

- Web 报表设计器，在浏览器中设计中国式复杂报表，模板保存为 XML。
- 数据集支持 SQL（JDBC 内置数据源）和 Spring Bean 方法。
- 表达式与函数、条件属性、参数、行类型、补空白行与分页、图片、套打等报表能力。
- HTML 在线预览、分页和打印，导出 Excel（XLSX）、PDF 和 Word（DOCX）。
- 从 Excel 导入为报表模板。

## 版本线

源码按 Spring Boot 大版本分成三个分支。三条线的 groupId 都是 `cn.iocoder.ureport2`，artifactId 也相同，用版本号区分：维护版本的主号与宿主的 Spring Boot 大版本一致（`2.x` 对应 Boot 2，`3.x` 对应 Boot 3，`4.x` 对应 Boot 4）。宿主只引入与自己 Boot 版本对应的一条线，不要同时引入多条线。

| 分支 | 维护版本 | 已验证的宿主版本 | Spring | JDK | Servlet | 主要依赖 | Starter artifactId |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `master` | `2.0.0-SNAPSHOT` | Spring Boot 2.7.18 | 5.3.31 | JDK 8 | javax.servlet 4.0.1 | POI 4.1.2、Velocity 2.3 | `ureport2-spring-boot-starter` |
| `master-jdk17` | `3.0.0-SNAPSHOT` | Spring Boot 3.5.15 | 6.2.19 | JDK 17 | jakarta.servlet 6.0.0 | POI 5.5.1、Velocity 2.4.1 | `ureport2-spring-boot-starter` |
| `master-jdk25` | `4.0.0-SNAPSHOT` | Spring Boot 4.1.2 | 7.0.10 | JDK 25（库字节码为 Java 17） | jakarta.servlet 6.1.0 | POI 5.5.1、Velocity 2.4.1 | `ureport2-spring-boot-starter` |

表中是实际验证过的精确版本，同一大版本的其他小版本没有逐一验证，不承诺全部可用。

每条线的产物都是 `ureport2-parent`、`ureport2-bom`、`ureport2-core`、`ureport2-font`、`ureport2-console`、`ureport2-spring-boot-starter`，只有版本不同。同一个宿主中，所有 `cn.iocoder.ureport2` 模块必须使用同一个维护版本。

**当前分支：`master`**（Spring Boot 2.7.18，JDK 8，javax.servlet 4.0.1）。

## 相对官方 UReport2 的扩展与兼容改造

下表只列本项目在官方 2.2.9 基础上新增或修改的部分。报表模板格式、设计器和报表功能、扩展接口（`ReportProvider`、`BuildinDatasource`、`Function`、`FontRegister`、`ImageProvider` 等）以及字体模块都沿用官方，不在表中。

| 改造 | 对使用者的意义 |
| --- | --- |
| 按 Spring Boot 2 / 3 / 4 分成三条线，各线的 parent、BOM、core、font、console 和 starter 使用相同 artifactId，以版本主号区分，每条线只包含一套引擎类 | 按宿主的 Boot 大版本选对应主号的版本即可，不需要自己做 javax 到 jakarta 等迁移 |
| Starter 自动注册 `UReportServlet` 到 `/ureport/*`，并导入原有的 `ureport-console-context.xml`，原 Bean 名称不变 | 不需要手写 `web.xml` 或 Servlet 注册代码 |
| `ureport.*` 配置属性绑定到原 XML 占位符；默认 `debug=false`、`disable-file-provider=true`，启用文件存储时必须配置绝对路径 | 在 `application.yml` 中配置；默认不往服务器磁盘写模板，也不会把模板写到相对路径或 `/WEB-INF` 下 |
| 宿主通过 Starter 启动时检查 classpath；本项目自身的 Maven 构建另用 Enforcer 规则禁止 `com.bstek.ureport` 依赖 | 宿主混入官方引擎 jar 或其他线的 core、console 时，应用在启动阶段直接失败，不会在运行时出现同包名类冲突。Enforcer 只作用于本项目的构建，不会带到宿主构建中 |
| Excel 导入改用 Servlet 原生 `Part`，去掉 commons-fileupload；上限由 `ureport.import-max-file-size`（默认 10MB）和 `ureport.import-max-request-size`（默认 11MB）控制 | 少一个旧依赖，上传大小可以按需调整。这两个配置只限制大小，不检查文件类型或内容 |
| 按各线的依赖版本适配 POI、Velocity、dom4j、commons-io、commons-lang3 以及 Spring、Servlet API 的调用（Boot 3 / 4 线同时迁移到 jakarta.servlet） | 在“版本线”表中列出的宿主版本上可以直接运行。这不代表覆盖所有版本，也不等于做过完整的依赖安全审计 |
| 静态资源接口只读取打包的 `ureport-asserts` 目录，拒绝路径穿越和其他 classpath 资源 | 不能再通过 `/ureport/res/` 读到配置文件或类文件。这只是资源访问边界，登录和权限仍由宿主负责，见“安全与部署” |
| 新增 `token` / `tenantId` 透明传递：入口 URL 带上这两个 query 参数后，设计器、预览页和查询表单设计器发往 UReport 自身同源路径的后续请求会自动带上；三个页面加 `no-referrer`；`token` 不进入报表参数，也不回显到预览 HTML | 宿主可以用自己的过滤器通过 `getParameter` 校验每个请求，不需要改 UReport 前端或增加登录接口，见“token / tenantId 传递” |

## 快速开始

### 1. 构建并安装到本地仓库

当前分支的版本 `2.0.0-SNAPSHOT` **还没有发布到 Maven Central**，需要先从源码构建并安装到本地 Maven 仓库。在发布之前，下面的坐标只能在执行过 `mvn install` 的机器上解析。

```bash
git clone https://gitee.com/yudaocode/yudao-ureport2.git
cd yudao-ureport2
git checkout master
# 使用 JDK 8
mvn clean install
```

### 2. 引入 Starter

```xml
<properties>
    <!-- 主号与宿主的 Spring Boot 大版本一致 -->
    <ureport2.version>2.0.0-SNAPSHOT</ureport2.version>
</properties>

<dependency>
    <groupId>cn.iocoder.ureport2</groupId>
    <artifactId>ureport2-spring-boot-starter</artifactId>
    <version>${ureport2.version}</version>
</dependency>
```

同时引用 core、console 时，可以用 BOM 统一版本：

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>cn.iocoder.ureport2</groupId>
            <artifactId>ureport2-bom</artifactId>
            <version>${ureport2.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

不要同时引入官方的 `com.bstek.ureport:*`，也不要让 `cn.iocoder.ureport2` 的各模块混用不同主号的版本（例如 starter 用 `2.x`、core 用 `3.x`）。Starter 启动时会检查 classpath，发现重复引擎或混用版本线时直接启动失败。

### 3. 配置（可选）

所有配置都有默认值，不配置也能启动：

```yaml
ureport:
  enabled: true                              # 是否启用自动配置
  debug: false                               # 是否输出调试信息
  disable-file-provider: true                # 是否禁用服务器文件存储
  # file-store-dir: /data/ureport-files      # 启用文件存储时必填，必须是绝对路径
  disable-http-session-report-cache: false   # 是否禁用报表缓存
  import-max-file-size: 10MB                 # Excel 导入单文件上限
  import-max-request-size: 11MB              # Excel 导入请求总大小上限
```

| 配置 | 默认值 | 说明 |
| --- | --- | --- |
| `ureport.enabled` | `true` | 关闭后不加载自动配置 |
| `ureport.debug` | `false` | 对应原 `ureport.debug` |
| `ureport.disable-file-provider` | `true` | 默认禁用服务器文件存储，不写 `/WEB-INF` |
| `ureport.file-store-dir` | 无 | 启用文件存储时必须配置绝对路径；禁用时不创建任何目录 |
| `ureport.disable-http-session-report-cache` | `false` | 对应原同名配置 |
| `ureport.import-max-file-size` | `10MB` | `/ureport/import` 单个文件上限 |
| `ureport.import-max-request-size` | `11MB` | `/ureport/import` 整个请求上限 |

文件存储默认关闭。把 `disable-file-provider` 设为 `false` 时必须同时配置 `file-store-dir`，而且只能是绝对路径；未配置、相对路径或原来的 `/WEB-INF/...` 写法都会在启动时报错。

使用 Starter 时以 Spring Boot 中的 `ureport.*` 配置为准，未配置时生效的是上表中的 Starter 默认值，它们也高于原来 classpath 下 `ureport.properties` 中的值。从旧项目迁移时，需要把原 `ureport.properties` 中的配置改写到 `application.yml`。宿主直接配置的原驼峰键（例如 `ureport.disableFileProvider`）仍然优先。

### 4. 访问

Servlet 注册在 `/ureport/*`，宿主配置了 `server.servlet.context-path` 时需要加上该前缀。

| 地址 | 说明 |
| --- | --- |
| `/ureport/designer` | 报表设计器 |
| `/ureport/preview?_u=<报表文件>` | HTML 预览 |
| `/ureport/excel?_u=<报表文件>` | 导出 Excel |
| `/ureport/pdf?_u=<报表文件>` | 导出 PDF |
| `/ureport/word?_u=<报表文件>` | 导出 Word |

`<报表文件>` 是带存储前缀的文件名，前缀由对应的 `ReportProvider` 决定，例如 `file:demo.ureport.xml`。

## 安全与部署

- **本项目不提供登录和权限控制。** 宿主需要用自己的 Spring Security 或其他认证机制保护 `/ureport/**`。设计、保存、删除、数据源管理等操作和预览、导出的风险不同，建议分开授权；特别是数据源相关接口可以浏览表结构和测试 JDBC 连接，只应开放给设计管理员。
- 只有 `/ureport/res/ureport-asserts/**` 是设计器需要的静态资源，可以按需放行；不要放行整个 `/ureport/**`。
- 草稿、预览分页和图表数据缓存在当前进程的内存中，以 HttpSession 的 sessionId（即 `JSESSIONID`）为 key。推荐与宿主前端同源部署；多实例部署需要会话粘滞，或自定义共享报表缓存实现。只共享 HttpSession 并不会让这些缓存的报表对象在实例间共享。
- 同一用户在多个标签页打开草稿时可能互相覆盖。
- 模板中的 SQL 由报表直接执行，不会自动附加租户或数据权限条件。多租户场景下，数据范围需要在 SQL、Bean 数据集或数据源层面由宿主自行控制。

### token / tenantId 传递

宿主在 iframe 或新窗口中打开 UReport 页面时，把 `token`（必须小写）和可选的 `tenantId`（驼峰）放在 URL query 中。设计器、预览页以及设计器内嵌的查询表单设计器会通过 `ureport-token.js` 把这两个值自动附加到 UReport 自身的后续请求上，宿主用自己的过滤器调用 `request.getParameter("token")` 读取并校验即可。

```javascript
// 值对库是不透明的，必须用 encodeURIComponent 编码（token 常含 + / = 等字符）
const q = 'token=' + encodeURIComponent(token) + '&tenantId=' + encodeURIComponent(tenantId);
iframe.src = '/ureport/designer?' + q;
// 预览：_u 等原有参数照旧
window.open('/ureport/preview?_u=' + encodeURIComponent('file:demo.ureport.xml') + '&' + q);
// 配置了 context-path 时加前缀，例如 /admin-api/ureport/designer?...
```

- 两个参数都为空时脚本什么都不做。`tenantId` 没传就不追加，库不会生成默认 token 或租户。
- 附加范围：只附加到与当前页同源、路径等于 `{context-path}/ureport` 或在其下的请求，包括 XHR（加载、保存、数据源、预览数据、分页、定时刷新、图表数据）、`window.open`（预览、打开报表、导出、翻页）、iframe（查询表单设计器、PDF 打印）、提交到这些路径的 GET 和 POST 表单（包括导入 Excel），以及页面中指向这些路径的链接（包括拖到新标签页打开）。
- 不附加：外站、其他协议、宿主的其他接口、越出 context-path 的路径，以及 `/ureport/res/**` 静态资源。宿主需要匿名放行 `/ureport/res/ureport-asserts/**`。报表中指向外部地址的链接和图片也不会带上凭据。
- 目标 URL 已经有同名参数时保留原值，不重复追加。
- `token` 是保留参数名，不会进入报表参数，表达式 `param("token")`、SQL 参数、Bean 数据集都拿不到它，预览 HTML 中也不会回显它。不要把报表参数或查询表单元素命名为 `token`。
- `tenantId` 由入口 URL 传入，仍是普通报表参数，模板可以通过 `param("tenantId")` 读取，是否使用由宿主决定。库不会根据它自动过滤数据，也不改写 SQL。
- 查询表单组件不要命名为 `tenantId` 或 `token`，查询条件请使用其他字段名。`token` 不会进入报表参数；`tenantId` 的单次解码只针对入口 URL，而查询表单提交时会对字段值再编码，同名字段会覆盖入口传入的值，中文或含 `%` 的值还会残留编码。
- 传递不等于授权。客户端传来的 `tenantId` 不可信，宿主应从校验过的 token 推导租户，并负责校验用户、租户和权限。
- token 对库是不透明的。库不实现 OAuth 刷新，不签发新凭据，也不新增登录接口、票据或请求头通道。
- `token` 可以承载 accessToken，也可以承载 refreshToken，库只原样透传，不区分类型。宿主的鉴权逻辑决定接受哪一种：有的宿主只校验 accessToken，有的会在 accessToken 无效时改用 refreshToken。因此传入 refreshToken 不保证能通过校验，需要以宿主自身实现为准。
- 仍保留 `JSESSIONID` 和现有的报表缓存，不做无 Session 改造。
- 三个页面都加了 `<meta name="referrer" content="no-referrer">`，防止带 token 的页面地址作为 Referer 发出。token 放在 URL 里仍可能进入浏览器历史和访问日志，宿主需要使用 HTTPS，并对日志中的 `token` 参数打码。

## 扩展

宿主把以下接口的实现注册为 Spring Bean 即可被自动发现：

| 接口 | 用途 |
| --- | --- |
| `com.bstek.ureport.provider.report.ReportProvider` | 报表模板的存储，例如存入数据库 |
| `com.bstek.ureport.definition.datasource.BuildinDatasource` | 内置 JDBC 数据源，可复用宿主的 `DataSource` |
| `com.bstek.ureport.expression.function.Function` | 自定义表达式函数 |
| `com.bstek.ureport.export.pdf.font.FontRegister` | 注册 PDF 导出字体 |
| `com.bstek.ureport.provider.image.ImageProvider` | 自定义图片来源 |

Spring Bean 数据集在模板中使用 `type="spring"` 的数据源，指定 Bean 名称和方法。

例如，复用宿主的 `DataSource` 作为内置数据源：

```java
/**
 * 报表内置数据源，复用宿主的 DataSource
 *
 * @author 芋道源码
 */
@Component
public class HostBuildinDatasource implements BuildinDatasource {

    private final DataSource dataSource;

    public HostBuildinDatasource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public String name() {
        return "host-datasource"; // 设计器中显示的数据源名称
    }

    @Override
    public Connection getConnection() {
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

}
```

## 字体与中文 PDF

字体与官方 UReport2 一致。引入 starter 后会自动加载官方字体（包括宋体、黑体），模板中使用这些字体名即可导出中文 PDF。

需要其他字体时，宿主可以额外注册 `FontRegister` Bean：`getFontName()` 与模板中的 `font-family` 一致，`getFontPath()` 返回字体在 classpath 中的路径。

## 文档

仓库内文档（沿用上游内容）：

- [报表存储与数据源配置](docs/STORAGE-DATASOURCE.md)
- [报表计算模型](docs/REPORT-MODEL.md)
- [表达式](docs/EXPRESSION.md)

上游学习入口：

- 官方 UReport2 仓库：<https://github.com/youseries/ureport>
- BSDN WIKI：<http://wiki.bsdn.org/display/UR/ureport2+Home>
- w3cschool：<https://www.w3cschool.cn/ureport>

上游文档按官方原版的 `web.xml` 方式介绍安装和配置；在本项目中由 Starter 自动完成，按上面的“快速开始”即可。

发布到 Maven Central 的维护者说明见 [publishing/PUBLISHING.md](publishing/PUBLISHING.md)。

## 许可证

本项目源码沿用 UReport2 的 [Apache License 2.0](LICENSE.txt)，保留原作者 Bstek（Jacky.gao）的版权和署名，归属说明见 [NOTICE](NOTICE)。

第三方依赖保留各自的许可证，并不全是 Apache 许可。其中 `com.itextpdf:itextpdf` 5.x（当前为 5.5.13）为 AGPL / 商业双许可，使用前请自行确认是否符合你的分发方式。
