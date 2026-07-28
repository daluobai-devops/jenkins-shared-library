# 09 — 建立可替换的 Codeup seam

**What to build:** 在不改变当前仓库分发结果的前提下，让 Codeup 仓库枚举、目录读取和配置内容读取通过可替换 adapter 提供，使分发测试可以使用内存数据而不修改全局元类或访问网络。

**Blocked by:** 01 — 锁定既有入口兼容基线.

**Status:** ready-for-agent

- [ ] 生产 Codeup adapter 保持现有域名、授权头、分页、编码和错误处理语义。
- [ ] 内存 Codeup adapter 可以表达多个仓库、递归文件树、配置内容、缺失、撤回和读取失败。
- [ ] 仓库分发入口通过显式 seam 使用 Codeup adapter。
- [ ] 普通分发测试不访问网络，也不安装全局 metaClass 替换。
- [ ] 未授权域名、缺少组织标识和非成功响应继续产生既有错误。
- [ ] 仓库白名单、显式允许全部仓库和空白名单的既有行为不变。
- [ ] 受限声明 parser 保持独立 deep module，不被折回 Codeup adapter。
- [ ] 仓库分发兼容基线保持通过。
