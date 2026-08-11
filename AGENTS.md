## 配置兼容性

- 修改配置相关逻辑时，必须兼容已有的旧版配置文件。
- 兼容处理应集中在配置读取或解析入口，统一将旧配置转换为当前内部结构后，再交给后续实现处理。
- 实现层应只依赖转换后的统一配置结构；除非入口转换无法合理处理，否则不要在具体实现中散落旧配置的兼容判断和分支。

## Jenkins 配置生成技能同步

- 每次修改本仓库代码后，必须同步检查 `C:\Users\wuzhao\.agents\skills\wuzhao-jenkins-generate-config`。
- 代码变更影响 Jenkinsfile 入口、配置结构、默认值、兼容规则、校验规则、构建或部署策略、凭据语义、生成结果时，必须在同一任务中同步修改该技能的 `SKILL.md`、相关 `references/`、生成器脚本和对应测试，使技能行为与仓库当前实现一致。
- 代码变更不影响该技能契约时，不为制造变更而改写技能内容；仍须运行技能现有测试与结构校验，并在最终结果中明确说明已检查且无需同步内容。
- 代码和技能同步完成后，使用 `conda run -n public python -m unittest test_generate_config.py` 验证生成器测试，并使用 `skill-creator` 的 `quick_validate.py` 验证技能结构；任一校验失败都不能把同步工作视为完成。

<!-- gitnexus:start -->
# GitNexus — Code Intelligence

This project is indexed by GitNexus as **jenkins-shared-library** (304 symbols, 287 relationships, 0 execution flows). Use the GitNexus MCP tools to understand code, assess impact, and navigate safely.

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
