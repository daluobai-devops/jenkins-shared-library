## 配置兼容性

- 修改配置相关逻辑时，必须兼容已有的旧版配置文件。
- 兼容处理应集中在配置读取或解析入口，统一将旧配置转换为当前内部结构后，再交给后续实现处理。
- 实现层应只依赖转换后的统一配置结构；除非入口转换无法合理处理，否则不要在具体实现中散落旧配置的兼容判断和分支。

<!-- gitnexus:start -->
# GitNexus — Code Intelligence

This project is indexed by GitNexus as **jenkins-shared-library** (216 symbols, 197 relationships, 0 execution flows). Use the GitNexus MCP tools to understand code, assess impact, and navigate safely.

> Index stale? Run `node .gitnexus/run.cjs analyze` from the project root — it auto-selects an available runner. No `.gitnexus/run.cjs` yet? `npx gitnexus analyze` (npm 11 crash → `npm i -g gitnexus`; #1939).

## Always Do

- **MUST run impact analysis before editing any symbol.** Before modifying a function, class, or method, run `impact({target: "symbolName", direction: "upstream"})` and report the blast radius (direct callers, affected processes, risk level) to the user.
- **MUST run `detect_changes()` before committing** to verify your changes only affect expected symbols and execution flows. For regression review, compare against the default branch: `detect_changes({scope: "compare", base_ref: "main"})`.
- **MUST warn the user** if impact analysis returns HIGH or CRITICAL risk before proceeding with edits.
- When exploring unfamiliar code, use `query({search_query: "concept"})` to find execution flows instead of grepping. It returns process-grouped results ranked by relevance.
- When you need full context on a specific symbol — callers, callees, which execution flows it participates in — use `context({name: "symbolName"})`.
- For security review, `explain({target: "fileOrSymbol"})` lists taint findings (source→sink flows; needs `analyze --pdg`).

## Never Do

- NEVER edit a function, class, or method without first running `impact` on it.
- NEVER ignore HIGH or CRITICAL risk warnings from impact analysis.
- NEVER rename symbols with find-and-replace — use `rename` which understands the call graph.
- NEVER commit changes without running `detect_changes()` to check affected scope.

## Resources

| Resource | Use for |
|----------|---------|
| `gitnexus://repo/jenkins-shared-library/context` | Codebase overview, check index freshness |
| `gitnexus://repo/jenkins-shared-library/clusters` | All functional areas |
| `gitnexus://repo/jenkins-shared-library/processes` | All execution flows |
| `gitnexus://repo/jenkins-shared-library/process/{name}` | Step-by-step execution trace |

## CLI

| Task | Read this skill file |
|------|---------------------|
| Understand architecture / "How does X work?" | `.claude/skills/gitnexus/gitnexus-exploring/SKILL.md` |
| Blast radius / "What breaks if I change X?" | `.claude/skills/gitnexus/gitnexus-impact-analysis/SKILL.md` |
| Trace bugs / "Why is X failing?" | `.claude/skills/gitnexus/gitnexus-debugging/SKILL.md` |
| Rename / extract / split / refactor | `.claude/skills/gitnexus/gitnexus-refactoring/SKILL.md` |
| Tools, resources, schema reference | `.claude/skills/gitnexus/gitnexus-guide/SKILL.md` |
| Index, status, clean, wiki CLI commands | `.claude/skills/gitnexus/gitnexus-cli/SKILL.md` |

<!-- gitnexus:end -->

<!-- gitnexus-init-wiki:start -->

## GitNexus / Project Wiki

This repository is indexed with GitNexus and has a project wiki generated from GitNexus MCP graph context.

For architecture questions, onboarding, debugging, refactoring, impact analysis,
or cross-module changes, use GitNexus MCP/tools and the project wiki before
making assumptions from plain text search.

How to use it:
- Start with the wiki entry page for architecture and module overview.
- Read module-specific wiki pages only when the task touches that module.
- Treat wiki pages as orientation docs, not the final source of truth.
- Verify implementation details against source code and GitNexus graph results.
- Before editing important symbols, run GitNexus impact/context analysis when available.
- If GitNexus reports a stale index, run `gitnexus analyze` before relying on graph results.
- GitNexus may refresh its native `AGENTS.md` context block whenever `gitnexus analyze` runs.
- This workflow does not retain GitNexus-generated or GitNexus-modified `CLAUDE.md` unless explicitly requested.
- After major architecture, module-boundary, or core-flow changes, refresh the index and regenerate this wiki through GitNexus MCP.
<!-- gitnexus-init-wiki:end -->

<!-- obsidian:start -->

# AGENTS.md

## Obsidian 项目上下文

Obsidian Vault 根目录：

C:\Users\wuzhao\BaiduSyncdisk\workspace\Obsidian\Obsidian Vault

关联 Obsidian 项目：

C:\Users\wuzhao\BaiduSyncdisk\workspace\Obsidian\Obsidian Vault\projects\active\jenkins-pipeline

处理本代码项目相关任务前，先读取：

- C:\Users\wuzhao\BaiduSyncdisk\workspace\Obsidian\Obsidian Vault\AGENTS.md
- C:\Users\wuzhao\BaiduSyncdisk\workspace\Obsidian\Obsidian Vault\projects\active\jenkins-pipeline\project.md
- C:\Users\wuzhao\BaiduSyncdisk\workspace\Obsidian\Obsidian Vault\projects\active\jenkins-pipeline\tasks.md
- C:\Users\wuzhao\BaiduSyncdisk\workspace\Obsidian\Obsidian Vault\projects\active\jenkins-pipeline\risks.md

把 `project.md` 作为本项目的业务背景、目标、约束和运营上下文使用。  
代码规范、运行命令、测试命令仍以本代码仓库的 `AGENTS.md` 为准。

所有路径必须按 Windows 绝对路径解析。

<!-- obsidian:end -->
