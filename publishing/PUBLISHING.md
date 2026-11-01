# 发布到 Maven Central（维护者指南）

本文面向维护者，说明 yudao-ureport2 如何签名、打包并发布到 Maven Central。三条线（`master` / `master-jdk17` / `master-jdk25` 分支）的发布方式相同，本文件在三个分支中内容一致。

源码仓库：Gitee <https://gitee.com/yudaocode/yudao-ureport2>（主仓库，POM 的 `url` / `scm` 指向这里），GitHub <https://github.com/yudaocode/yudao-ureport2>。

## 1 产物、版本与 JDK

| 分支 | 宿主 | 构建 JDK | 库版本主号 | 默认 `revision` |
| --- | --- | --- | --- | --- |
| `master` | Spring Boot 2.7.18 | 8 | `2.x` | `2.0.0-SNAPSHOT` |
| `master-jdk17` | Spring Boot 3.5.15 | 17 | `3.x` | `3.0.0-SNAPSHOT` |
| `master-jdk25` | Spring Boot 4.1.2 | 25（库字节码为 17） | `4.x` | `4.0.0-SNAPSHOT` |

三条线的 groupId 都是 `cn.iocoder.ureport2`，artifactId 相同，与模块目录名一致。每条线发布 6 个产物：

| artifactId | 打包 |
| --- | --- |
| `ureport2-parent` | pom |
| `ureport2-bom` | pom |
| `ureport2-core` | jar |
| `ureport2-font` | jar |
| `ureport2-console` | jar |
| `ureport2-spring-boot-starter` | jar |

根聚合 POM（`ureport2-build`）和 `ureport2-compat-tests` 不发布（`maven.install.skip`、`maven.deploy.skip`、`central.skipPublishing`）。

**同名 artifact、不同版本**：三条线靠版本主号区分，库版本主号等于对应的 Spring Boot 大版本。例如 `ureport2-spring-boot-starter:2.0.0` 对应 Boot 2，`3.0.0` 对应 Boot 3，`4.0.0` 对应 Boot 4。各线只在自己的主号内递增版本（`master-jdk17` 分支发 `3.0.1`、`3.1.0`，不会发 `2.x` 或 `4.x`），所以同一个坐标下三条线的版本不会冲突。三条线分别从各自分支、在各自 JDK 下构建和上传，每次发布只上传本线 6 个产物。

每条线都要在本线对应的 JDK 下构建（设置 `JAVA_HOME`）。

**版本号**：`revision` 在 `ureport2-parent/pom.xml` 和 `ureport2-bom/pom.xml` 两处有默认值，发布时两处必须一致；也可以不改文件，在命令行统一传入 `-Drevision=<版本>`。`flatten-maven-plugin` 会把发布 POM 中的 `${revision}` 替换为实际版本。

Central 上的正式版本**不能覆盖或删除**，出问题只能发新的 patch 版本。

## 2 Maven 配置要点

- `release` profile **默认关闭**。只有显式 `-P release` 才会生成 sources、javadoc 并做 GPG 签名。普通 `mvn install` / `mvn verify` 不签名、不读取发布凭据、不上传。
- 插件版本：`central-publishing-maven-plugin` 0.12.0，`maven-gpg-plugin` 3.2.8，`maven-javadoc-plugin` 3.12.0，`maven-source-plugin` 3.3.1。
- `maven-source-plugin` 生成 `-sources.jar`；`maven-javadoc-plugin` 生成 `-javadoc.jar`，设置了 `doclint=none`，因为原 2.2.9 的注释不符合 doclint 规则，关闭检查后仍生成完整的 javadoc 产物。
- `maven-gpg-plugin` 为每个产物生成 `.asc` 签名，口令只从环境变量 `MAVEN_GPG_PASSPHRASE` 读取（`--pinentry-mode loopback`），不写进 POM 或命令行。
- `central-publishing-maven-plugin`：
  - `publishingServerId=central`，对应 settings.xml 中 `<server><id>central</id>`。
  - `autoPublish=false`：上传并通过校验后停在 Portal，需要人工在 <https://central.sonatype.com/publishing/deployments> 点 Publish。
  - `waitUntil=validated`：上传后等待 Portal 校验结果。
- **防误发布的两道开关**：
  1. `central.skipUpload` 默认 `true`，即使执行 `deploy` 也不上传。
  2. 设置 `-Dcentral.skipUpload=false` 时，绑定在 `validate` 阶段的 enforcer 规则要求同时传入 `-Dyudao.release.approved=true`，否则构建一开始就失败。只有确认要发布时才加这个参数。

SNAPSHOT 与正式版本在 `skipUpload=true` 时的行为不同：

| 版本类型 | `skipUpload=true` 时 |
| --- | --- |
| `-SNAPSHOT` | 走 snapshot 流程，跳过上传，不生成 release bundle |
| 正式版本（例如 `3.0.0`） | 在 `target/central-publishing/central-bundle.zip` 生成 bundle，不上传 |

所以要检查 bundle 内容，必须先改成正式版本号。

## 3 凭据与签名准备

凭据和私钥都不进仓库。

- **Portal 命名空间**：groupId `cn.iocoder.ureport2` 所属的命名空间需要在 Central Portal 中完成验证。要发布 SNAPSHOT，还需要为该命名空间启用 snapshots。
- **Portal user token**：在 Portal 生成 user token，写入仓库之外的私有 settings 文件（建议权限 600）。`publishing/settings.xml.example` 是模板，只有 `${env.CENTRAL_TOKEN_USERNAME}`、`${env.CENTRAL_TOKEN_PASSWORD}` 占位，可以复制后使用，也可以合并进自己的 `~/.m2/settings.xml`。
- **GPG 签名 key**：生成一把有口令保护的签名 key，把公钥上传到公共 keyserver（例如 `keyserver.ubuntu.com`），发布前确认能按 key ID 读回。私钥备份和撤销证书保存在仓库之外。

## 4 命令

下面的命令使用占位变量，按实际情况替换：

```bash
export JAVA_HOME=<本线对应的 JDK>
export UREPORT_SETTINGS=<私有 settings.xml 路径>
export GPG_KEY_ID=<签名 key 的 ID 或指纹>
export MAVEN_GPG_PASSPHRASE=<GPG 口令>
export CENTRAL_TOKEN_USERNAME=<Portal token 用户名>
export CENTRAL_TOKEN_PASSWORD=<Portal token 密码>
```

### 4.1 普通构建（不签名、不上传）

```bash
mvn -B clean install
```

### 4.2 发布产物预检（不需要凭据）

```bash
mvn -B -P release -Dgpg.skip=true clean verify          # 生成 sources / javadoc，跳过签名
mvn -B -P release help:effective-pom -pl ureport2-parent
```

### 4.3 本地签名并校验（不上传）

```bash
mvn -B -P release -Dgpg.keyname="$GPG_KEY_ID" clean verify
for f in */target/*.asc; do gpg --verify "$f" "${f%.asc}"; done
```

`verify` 不进入 `deploy` 阶段，不会上传。

### 4.4 本地生成 release bundle（不上传）

只在已经改成正式版本号、并决定准备发布时执行：

```bash
mvn -B -s "$UREPORT_SETTINGS" -P release -Dgpg.keyname="$GPG_KEY_ID" clean deploy
```

默认 `central.skipUpload=true`，这一步只在各模块的 `target/central-publishing/` 下生成 bundle，不上传。检查 bundle 中每个产物的 pom、jar、`-sources.jar`、`-javadoc.jar` 以及对应的 `.asc` 是否齐全（parent 和 bom 只有 pom）。

### 4.5 上传到 Central Portal

只有在维护者明确批准发布后执行：

```bash
mvn -B -s "$UREPORT_SETTINGS" -P release -Dgpg.keyname="$GPG_KEY_ID" \
    -Dcentral.skipUpload=false -Dyudao.release.approved=true \
    clean deploy
```

上传后因为 `autoPublish=false`，产物停在 Portal。核对 Portal 的校验结果和文件列表后，再人工点 Publish。

## 5 许可证注意事项

- 本项目源码沿用 UReport2 的 Apache License 2.0（`LICENSE.txt`），原作者 Bstek 的署名见 `NOTICE`，发布产物要保留。
- 第三方依赖保留各自的许可证，并不全是 Apache 许可。`com.itextpdf:itextpdf` 5.5.13 是 AGPL / 商业双许可，发布前要确认分发方式符合其许可要求，并在文档中提示使用方。

## 6 发布前检查清单

- [ ] POM 中的 `url` / `scm` 与 Gitee 主仓库 <https://gitee.com/yudaocode/yudao-ureport2> 一致。
- [ ] 已确定正式版本号，主号与本线 Boot 大版本一致；`ureport2-parent` 与 `ureport2-bom` 的 `revision` 一致，或统一用 `-Drevision` 传入。
- [ ] 三条线分别在对应 JDK 下通过完整测试。
- [ ] 源码已提交并打 tag。
- [ ] 命名空间已验证，user token 可用，公钥能从 keyserver 读回。
- [ ] 本地签名校验（4.3）通过，本地 bundle（4.4）内容齐全。
- [ ] 已确认 itextpdf 的许可证影响。
- [ ] 已得到明确的发布批准，再执行 4.5；上传后核对 Portal 校验结果，再人工 Publish。

## 7 宿主引入示例

三条线的 artifactId 相同，按宿主 Spring Boot 大版本选择版本主号：

```xml
<properties>
    <!-- Spring Boot 2.7.x / JDK 8 用 2.x；3.5.x / JDK 17 用 3.x；4.1.x / JDK 17+ 用 4.x -->
    <ureport2.version>3.0.0</ureport2.version>
</properties>

<dependency>
    <groupId>cn.iocoder.ureport2</groupId>
    <artifactId>ureport2-spring-boot-starter</artifactId>
    <version>${ureport2.version}</version>
</dependency>
```

可选 BOM（版本同样按主号选择）：

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

`ureport2-core`、`ureport2-font`、`ureport2-console` 必须与 starter 使用同一个版本，不要混用不同主号，也不要同时引入 `com.bstek.ureport:*`。发布到 Central 之前，这些坐标只能在执行过 `mvn install` 的机器上解析。
