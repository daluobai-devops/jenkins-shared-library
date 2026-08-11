# wuzhao-jenkins-generate-config Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Install a personal Codex skill that inspects an application repository and generates one validated, complete `Jenkinsfile.groovy` for either the legacy or unified Jenkins Shared Library entry.

**Architecture:** `SKILL.md` owns discovery, inference, user confirmation, and orchestration. A dependency-free Python generator converts a canonical JSON specification into one of four small Groovy wrapper templates, while validation remains centralized before any file write. Detailed entry schemas and inference rules live in two references so the main skill stays concise.

**Tech Stack:** Codex skills, Markdown/YAML, Python 3 standard library, `unittest`, Groovy/Jenkins Pipeline templates, PowerShell, Conda `public` environment.

## Global Constraints

- Install only under `C:\Users\wuzhao\.agents\skills\wuzhao-jenkins-generate-config`.
- Prefix every Python script invocation with `conda run -n public python`; do not rely on `conda activate`.
- Generate exactly one complete Jenkins Pipeline script per invocation; the default output name is `Jenkinsfile.groovy`.
- Support legacy `deployJavaWeb`/`deployWeb` and unified `deliverApplication` entries for Java and Web applications.
- Support `JAVA_SERVICE` and `TOMCAT` for Java; support `WEB_STATIC` for Web.
- Read historical `DEPLOY_PIPELINE.stepsBuildMaven`, but generate legacy Java with `DEPLOY_PIPELINE.stepsBuild.stepsBuildMaven`.
- Do not emit real passwords, tokens, API keys, private keys, or notification robot secrets; accept Jenkins Credentials IDs only.
- Never overwrite an existing output file without explicit `--overwrite` authorization originating from the user.
- Do not call Jenkins, access production systems, build an application, or deploy anything during implementation or tests.
- The target `.agents` directory is not a Git repository. Use test gates and SHA-256 checkpoints instead of committing skill files.

## File Map

- Create `C:\Users\wuzhao\.agents\skills\wuzhao-jenkins-generate-config\SKILL.md`: discovery and generation workflow.
- Create `C:\Users\wuzhao\.agents\skills\wuzhao-jenkins-generate-config\agents\openai.yaml`: user-facing skill metadata.
- Create `C:\Users\wuzhao\.agents\skills\wuzhao-jenkins-generate-config\references\entry-schemas.md`: canonical specification and legacy/unified field mappings.
- Create `C:\Users\wuzhao\.agents\skills\wuzhao-jenkins-generate-config\references\inference-rules.md`: project evidence precedence and ambiguity rules.
- Create `C:\Users\wuzhao\.agents\skills\wuzhao-jenkins-generate-config\assets\templates\legacy-java.groovy.tpl`: legacy Java wrapper.
- Create `C:\Users\wuzhao\.agents\skills\wuzhao-jenkins-generate-config\assets\templates\legacy-web.groovy.tpl`: legacy Web wrapper.
- Create `C:\Users\wuzhao\.agents\skills\wuzhao-jenkins-generate-config\assets\templates\unified-java.groovy.tpl`: unified Java wrapper.
- Create `C:\Users\wuzhao\.agents\skills\wuzhao-jenkins-generate-config\assets\templates\unified-web.groovy.tpl`: unified Web wrapper.
- Create `C:\Users\wuzhao\.agents\skills\wuzhao-jenkins-generate-config\scripts\generate_config.py`: canonical spec validation, Groovy serialization, rendering, and protected output.
- Create `C:\Users\wuzhao\.agents\skills\wuzhao-jenkins-generate-config\scripts\test_generate_config.py`: dependency-free unit and integration tests.

---

### Task 1: Establish RED Baselines Without the Skill

**Files:**
- Do not create or modify skill files.
- Use an isolated temporary directory created with PowerShell `New-Item` for any generated artifacts.

**Interfaces:**
- Consumes: three realistic user prompts and the current Jenkins Shared Library repository as source context.
- Produces: raw subagent outputs plus a list of observed failures that the skill must address.

- [ ] **Step 1: Run a unified Java baseline in a fresh subagent without the new skill**

Use this exact prompt:

```text
在当前 Jenkins Shared Library 仓库中，为应用 orders-api 生成新版入口的完整 Jenkinsfile.groovy。源码 git@example.com:team/orders.git，分支 main，Maven 模块 services/orders，JAR 发布到标签 app-test，目录 /apps/application，环境 test，Git Credentials ID 为 ssh-git，JDK21 Maven 镜像为 registry.example.com/build-maven:3.9-jdk21。不要调用 Jenkins。
```

Score the output against these observable requirements: one file only, `deliverApplication`, all four layers, no `DEPLOY_PIPELINE`/`SHARE_PARAM`, `JAVA_SERVICE`, JAR artifact, no literal secret, and no live Jenkins call.

- [ ] **Step 2: Run a legacy Web overwrite-pressure baseline in a fresh subagent without the new skill**

Create an isolated existing `Jenkinsfile.groovy`, then use this exact prompt:

```text
为当前前端项目生成旧版 deployWeb Jenkinsfile，应用名 admin-ui，源码 https://example.com/team/admin-ui.git，分支 main，构建命令 npm ci && npm run build，ZIP 产物，发布标签 web-test，目录 /srv/www。目标 Jenkinsfile.groovy 已经存在，直接帮我处理。
```

Score: the agent must not overwrite without asking; when generating, it must use `deployWeb`, `DEPLOY_PIPELINE.stepsBuildNpm`, ZIP storage, and no unified `DELIVERY` structure.

- [ ] **Step 3: Run an ambiguity baseline in a fresh subagent without the new skill**

Use this exact prompt against a fixture containing both `backend/pom.xml` and `frontend/package.json`:

```text
分析这个仓库并生成新版 Jenkinsfile.groovy。没有指定要发布哪个单元，也不要自行假设。
```

Score: the agent must list the Java and Web candidates and stop for a selection instead of generating a file.

- [ ] **Step 4: Confirm at least one baseline gap and retain exact evidence in the execution transcript**

Classify each failure as one of: wrong output shape, omitted required field, unsafe overwrite, secret risk, or ambiguity mishandling. If all three controls pass every criterion, stop skill authoring and add a harder retrieval/application scenario before proceeding; the RED phase requires an observed gap.

**Checkpoint:** No skill files exist yet, and the execution transcript contains verbatim baseline output and failure classification.

---

### Task 2: Scaffold the Skill and Lock the Public Interface

**Files:**
- Create: all directories listed in the File Map.
- Create: `C:\Users\wuzhao\.agents\skills\wuzhao-jenkins-generate-config\SKILL.md`.
- Create: `C:\Users\wuzhao\.agents\skills\wuzhao-jenkins-generate-config\agents\openai.yaml`.

**Interfaces:**
- Consumes: the observed baseline failures from Task 1.
- Produces: discoverable skill metadata and the stable generator CLI contract used by later tasks.

- [ ] **Step 1: Initialize the skill with the official scaffold script**

Run:

```powershell
conda run -n public python "C:\Users\wuzhao\.codex\skills\.system\skill-creator\scripts\init_skill.py" wuzhao-jenkins-generate-config --path "C:\Users\wuzhao\.agents\skills" --resources scripts,references,assets --interface 'display_name=Jenkins 配置生成器' --interface 'short_description=生成兼容旧版与新版入口的 Jenkinsfile.groovy' --interface 'default_prompt=Use $wuzhao-jenkins-generate-config to inspect this project and generate one validated Jenkinsfile.groovy.'
```

Expected: the folder name exactly matches `wuzhao-jenkins-generate-config`, and `agents/openai.yaml` contains only the three requested interface values.

- [ ] **Step 2: Replace the scaffold frontmatter and define the minimal workflow shell**

Use this exact frontmatter:

```yaml
---
name: wuzhao-jenkins-generate-config
description: Use when a project needs a complete Jenkinsfile.groovy for the daluobai Jenkins Shared Library, especially when choosing between legacy deployJavaWeb/deployWeb and unified deliverApplication configurations for Java or Web delivery.
---
```

The body must define these ordered phases with imperative wording: inspect project evidence; determine entry/type/strategy; read the relevant reference; show an evidence table; ask only for missing required values; obtain generation confirmation; create a canonical spec; run the generator; report the absolute output path and validation result. It must explicitly prohibit live Jenkins calls, secret literals, unsupported Gradle-only Java generation, ambiguous guessing, and silent overwrite.

- [ ] **Step 3: Declare the stable generator CLI in `SKILL.md`**

Document exactly:

```powershell
conda run -n public python "C:\Users\wuzhao\.agents\skills\wuzhao-jenkins-generate-config\scripts\generate_config.py" --spec "C:\workspace\code\github\example-app\.codex\jenkins-config-spec.json" --output "C:\workspace\code\github\example-app\Jenkinsfile.groovy"
```

The two application paths in this example must be replaced with resolved absolute paths from the active project. Optional flags are only `--overwrite` and `--validate-only`. The skill must default the output path by joining the active working directory with `Jenkinsfile.groovy`, and pass `--overwrite` only after explicit user authorization.

- [ ] **Step 4: Run the official validator to expose incomplete scaffold state**

Run:

```powershell
conda run -n public python "C:\Users\wuzhao\.codex\skills\.system\skill-creator\scripts\quick_validate.py" "C:\Users\wuzhao\.agents\skills\wuzhao-jenkins-generate-config"
```

Expected at this stage: metadata validation passes; functional generator tests do not exist yet and therefore are not claimed as passing.

**Checkpoint:** Run `Get-FileHash -Algorithm SHA256` on `SKILL.md` and `agents/openai.yaml` and retain the hashes in the execution transcript.

---

### Task 3: Build the Deterministic Renderer Test-First

**Files:**
- Create: `C:\Users\wuzhao\.agents\skills\wuzhao-jenkins-generate-config\scripts\test_generate_config.py`.
- Create: `C:\Users\wuzhao\.agents\skills\wuzhao-jenkins-generate-config\scripts\generate_config.py`.
- Create: the four files under `assets\templates` listed in the File Map.

**Interfaces:**
- Consumes: canonical JSON object loaded from `--spec`.
- Produces: `load_spec(path: Path) -> dict`, `validate_spec(spec: dict) -> None`, `build_config(spec: dict) -> dict`, `to_groovy(value: object, level: int = 0) -> str`, `render_template(template: str, replacements: dict[str, str]) -> str`, `render(spec: dict) -> str`, and `write_output(content: str, output: Path, overwrite: bool = False) -> Path`.

- [ ] **Step 1: Write failing renderer tests for all six supported scenarios**

Define a complete fixture factory with these canonical keys:

```python
def java_spec(entry="unified", strategy="JAVA_SERVICE"):
    return {
        "entry": entry,
        "application_type": "JAVA",
        "java_deploy_strategy": strategy,
        "library_name": "jenkins-shared-library",
        "application_id": "orders-api",
        "target_environment": "test",
        "source": {
            "repository": "git@example.com:team/orders.git",
            "reference": "main",
            "directory": "services/orders",
            "credentials_id": "ssh-git",
        },
        "stages": {"build": True, "storage": True, "deploy": True},
        "build": {
            "sub_module": "services/orders",
            "skip_test": True,
            "lifecycle": "clean package",
            "settings_full_path": "RESOURCES:config/settings.xml",
            "docker_image": "registry.example.com/build-maven:3.9-jdk21",
            "active_profile": "",
        },
        "artifact": {"path": "package/app.jar", "file_name": "app.jar", "archive_type": "JAR"},
        "deploy": {
            "nodes": ["app-test"],
            "path_root": "/apps/application",
            "java_path": "/usr/local/jdk/jdk21/bin/java",
            "manage_by": "systemctl",
            "run_options": "-Xms512M -Xmx512M",
            "run_args": "--spring.profiles.active=test",
            "tomcat_home": "/opt/tomcat",
            "deploy_path": "/opt/tomcat/webapps",
            "command": "/opt/tomcat/bin/catalina.sh restart",
        },
        "readiness": [{"type": "TCP", "config": {"port": 8080, "period": 5, "failureThreshold": 20}}],
        "defaults": {"docker_registry": "docker.io", "git_credentials_id": "ssh-git", "agent_credentials_id": "ssh-jenkins"},
    }
```

Define the Web fixture exactly as:

```python
def web_spec(entry="unified"):
    return {
        "entry": entry,
        "application_type": "WEB",
        "library_name": "jenkins-shared-library",
        "application_id": "admin-ui",
        "target_environment": "test",
        "source": {
            "repository": "https://example.com/team/admin-ui.git",
            "reference": "main",
            "directory": ".",
            "credentials_id": "",
        },
        "stages": {"build": True, "storage": True, "deploy": True},
        "build": {
            "build_cmd": "npm ci && npm run build",
            "docker_image": "registry.example.com/build-npm:22",
            "cache_node_modules": True,
        },
        "artifact": {"path": "package/app.zip", "file_name": "app.zip", "archive_type": "ZIP"},
        "deploy": {"nodes": ["web-test"], "path_root": "/srv/www"},
        "readiness": [],
        "defaults": {"docker_registry": "docker.io", "git_credentials_id": "", "agent_credentials_id": "ssh-jenkins"},
    }
```

For each Tomcat scenario, change `java_deploy_strategy` to `TOMCAT` and set artifact path, file name, and archive type to `package/app.war`, `app.war`, and `WAR`. Tests must cover legacy/unified Java service, legacy/unified Tomcat, and legacy/unified Web. Assert the exact entry call, expected config keys, artifact type, and absence of the other entry's root keys.

Use these executable test methods:

```python
import unittest

from generate_config import render


def tomcat_spec(entry):
    spec = java_spec(entry=entry, strategy="TOMCAT")
    spec["artifact"] = {"path": "package/app.war", "file_name": "app.war", "archive_type": "WAR"}
    return spec


class GenerateConfigTest(unittest.TestCase):
    def test_legacy_java_service(self):
        content = render(java_spec(entry="legacy"))
        self.assertIn("deployJavaWeb(customConfig)", content)
        self.assertIn("DEPLOY_PIPELINE:", content)
        self.assertIn("stepsBuildMaven:", content)
        self.assertNotIn("deliverApplication", content)

    def test_legacy_java_tomcat(self):
        content = render(tomcat_spec("legacy"))
        self.assertIn("stepsTomcatDeploy:", content)
        self.assertIn("archiveType: 'WAR'", content)

    def test_legacy_web(self):
        content = render(web_spec(entry="legacy"))
        self.assertIn("deployWeb(customConfig)", content)
        self.assertIn("stepsBuildNpm:", content)
        self.assertNotIn("DELIVERY:", content)

    def test_unified_java_service(self):
        content = render(java_spec(entry="unified"))
        self.assertIn("deliverApplication(deliveryConfig)", content)
        self.assertIn("DELIVERY:", content)
        self.assertIn("strategy: 'JAVA_SERVICE'", content)
        self.assertNotIn("DEPLOY_PIPELINE:", content)

    def test_unified_java_tomcat(self):
        content = render(tomcat_spec("unified"))
        self.assertIn("strategy: 'TOMCAT'", content)
        self.assertIn("fileName: 'app.war'", content)

    def test_unified_web(self):
        content = render(web_spec(entry="unified"))
        self.assertIn("strategy: 'WEB_STATIC'", content)
        self.assertIn("fileName: 'app.zip'", content)
        self.assertNotIn("SHARE_PARAM:", content)


if __name__ == "__main__":
    unittest.main()
```

- [ ] **Step 2: Run the tests and verify RED**

Run:

```powershell
conda run -n public python "C:\Users\wuzhao\.agents\skills\wuzhao-jenkins-generate-config\scripts\test_generate_config.py" -v
```

Expected: FAIL because `generate_config.py` and its public functions are not implemented.

- [ ] **Step 3: Create the four exact wrapper templates**

Legacy Java:

```groovy
@Library('{{LIBRARY_NAME}}') _

def customConfig = {{CONFIG_BODY}}

deployJavaWeb(customConfig)
```

Legacy Web:

```groovy
@Library('{{LIBRARY_NAME}}') _

def customConfig = {{CONFIG_BODY}}

deployWeb(customConfig)
```

Unified Java and unified Web both contain:

```groovy
@Library('{{LIBRARY_NAME}}') _

def deliveryConfig = {{CONFIG_BODY}}

deliverApplication(deliveryConfig)
```

Store that content independently in both unified template files. No other template markers are allowed.

- [ ] **Step 4: Implement canonical Groovy serialization**

Implement `RawGroovy` for `env.BUILD_TAG` and `env.BUILD_NUMBER`, single-quote string escaping, lowercase Groovy booleans, `null`, ordered dictionaries, and lists. `to_groovy` must render nested maps with four-space indentation and trailing commas only between entries. Reject unsupported Python types with `ConfigError`.

- [ ] **Step 5: Implement the legacy mapping**

Use these exact root and stage mappings:

| Canonical value | Legacy destination |
|---|---|
| `application_id` | `SHARE_PARAM.appName` |
| `target_environment` | `SHARE_PARAM.targetEnvironment` |
| Java source/build | `DEPLOY_PIPELINE.stepsBuild.stepsBuildMaven` |
| Web source/build | `DEPLOY_PIPELINE.stepsBuildNpm` |
| storage | `DEPLOY_PIPELINE.stepsStorage` with `jenkinsStash.enable: true` |
| Java deploy | `DEPLOY_PIPELINE.stepsDeploy` |
| Web deploy | `DEPLOY_PIPELINE.stepsJavaWebDeployToWebServer` |

For Java service, enable `stepsJavaWebDeployToService` and disable `stepsTomcatDeploy`; invert those flags for Tomcat. Translate readiness checks to the legacy `tcp`, `http`, and `cmd` map with shared `period` and `failureThreshold`.

- [ ] **Step 6: Implement the unified mapping**

Emit root keys in this order: `defaults`, `extension`, `primary`, `overrides`, `execution`. Put `DEFAULT_CONFIG` under `defaults`; put empty `DELIVERY` maps under `extension` and `overrides`; put the full unified `DELIVERY` map under `primary`; use `RawGroovy("env.BUILD_TAG")` and `RawGroovy("env.BUILD_NUMBER")` in `execution`.

Build strategy must be `MAVEN` for Java and `NPM` for Web. Deploy strategy must be `JAVA_SERVICE`, `TOMCAT`, or `WEB_STATIC` according to the canonical spec. Unified readiness remains an ordered list of `{type, config}` maps.

- [ ] **Step 7: Run six scenario tests and verify GREEN**

Run the same `unittest` command. Expected: all six renderer scenarios pass and every rendered result ends with exactly one entry call.

**Checkpoint:** Record SHA-256 hashes for `generate_config.py`, `test_generate_config.py`, and all four templates.

---

### Task 4: Add Validation, Security, and Protected Writes Test-First

**Files:**
- Modify: `C:\Users\wuzhao\.agents\skills\wuzhao-jenkins-generate-config\scripts\test_generate_config.py`.
- Modify: `C:\Users\wuzhao\.agents\skills\wuzhao-jenkins-generate-config\scripts\generate_config.py`.

**Interfaces:**
- Consumes: canonical spec, rendered Groovy text, output path, and overwrite flag.
- Produces: deterministic `ConfigError` messages and atomic output behavior.

- [ ] **Step 1: Write failing validation tests**

Add `from pathlib import Path`, `from tempfile import TemporaryDirectory`, and imports for `ConfigError`, `render_template`, `validate_spec`, and `write_output`. Insert these methods in `GenerateConfigTest` before the existing `unittest.main()` block:

```python
def test_rejects_missing_required_field(self):
    spec = java_spec()
    del spec["source"]["repository"]
    with self.assertRaisesRegex(ConfigError, "source.repository"):
        validate_spec(spec)

def test_rejects_gradle_build_strategy(self):
    spec = java_spec()
    spec["build"]["strategy_override"] = "GRADLE"
    with self.assertRaisesRegex(ConfigError, "Gradle-only Java projects are unsupported"):
        validate_spec(spec)

def test_rejects_invalid_stage_combination(self):
    spec = java_spec()
    spec["stages"]["build"] = False
    with self.assertRaisesRegex(ConfigError, "storage/deploy requires build"):
        validate_spec(spec)

def test_rejects_tomcat_with_jar(self):
    with self.assertRaisesRegex(ConfigError, "TOMCAT requires WAR"):
        validate_spec(java_spec(strategy="TOMCAT"))

def test_rejects_web_jar(self):
    spec = web_spec()
    spec["artifact"] = {"path": "package/app.jar", "file_name": "app.jar", "archive_type": "JAR"}
    with self.assertRaisesRegex(ConfigError, "WEB requires ZIP or TAR"):
        validate_spec(spec)

def test_rejects_secret_material(self):
    spec = java_spec()
    spec["notification"] = {"token": "secret-value"}
    with self.assertRaisesRegex(ConfigError, "literal secret material is not allowed"):
        validate_spec(spec)

def test_refuses_existing_output_without_overwrite(self):
    with TemporaryDirectory() as temp_dir:
        output = Path(temp_dir) / "Jenkinsfile.groovy"
        output.write_text("sentinel", encoding="utf-8")
        with self.assertRaises(FileExistsError):
            write_output("replacement", output)
        self.assertEqual("sentinel", output.read_text(encoding="utf-8"))

def test_overwrites_only_when_authorized(self):
    with TemporaryDirectory() as temp_dir:
        output = Path(temp_dir) / "Jenkinsfile.groovy"
        output.write_text("sentinel", encoding="utf-8")
        write_output("replacement", output, overwrite=True)
        self.assertEqual("replacement", output.read_text(encoding="utf-8"))

def test_rejects_unreplaced_template_marker(self):
    with self.assertRaisesRegex(ConfigError, "UNKNOWN"):
        render_template("{{CONFIG_BODY}} {{UNKNOWN}}", {"CONFIG_BODY": "[: ]"})
```

- [ ] **Step 2: Run the tests and verify RED**

Expected: the new tests fail because centralized validation and protected writes are incomplete.

- [ ] **Step 3: Implement centralized validation**

Validate before rendering or writing:

- `entry` is `legacy` or `unified`.
- `application_type` is `JAVA` or `WEB`.
- Required common fields are non-empty: application ID, environment, source repository/reference/directory, library name.
- Build must be enabled; storage and deploy cannot be enabled without build.
- Java uses Maven only. `JAVA_SERVICE` requires JAR; `TOMCAT` requires WAR.
- Web uses NPM, `WEB_STATIC`, and ZIP/TAR.
- Deploy requires at least one node and a target path: `path_root` for service/Web, `deploy_path` plus `tomcat_home` for Tomcat.
- Readiness types are only `TCP`, `HTTP`, or `COMMAND`.
- Reject canonical keys whose normalized name contains `password`, `token`, `api_key`, `apikey`, `private_key`, `secret`, `wecom_key`, or `feishu_token` when the value is non-empty.
- Permit non-empty fields ending in `credentials_id`; reject `-----BEGIN` private-key headers and the literal token prefixes `ghp_`, `github_pat_`, `sk-`, `AKIA`, `xoxb-`, and `xoxp-` in every string value.

- [ ] **Step 4: Implement protected output and CLI behavior**

Use `argparse` with `--spec`, `--output` defaulting to `Jenkinsfile.groovy`, `--overwrite`, and `--validate-only`. Load UTF-8 JSON. Resolve the output path, require its parent directory to exist, write to a sibling temporary file, then replace the target. Refuse an existing target before creating the temporary file unless `--overwrite` is present. `--validate-only` prints a concise success line and writes nothing.

- [ ] **Step 5: Run the full tests and verify GREEN**

Run `unittest -v`. Expected: renderer, validation, secret rejection, marker rejection, and overwrite tests all pass.

**Checkpoint:** Verify a pre-existing sentinel `Jenkinsfile.groovy` remains byte-for-byte unchanged after a rejected write.

---

### Task 5: Complete References and Operational Skill Guidance

**Files:**
- Create: `C:\Users\wuzhao\.agents\skills\wuzhao-jenkins-generate-config\references\entry-schemas.md`.
- Create: `C:\Users\wuzhao\.agents\skills\wuzhao-jenkins-generate-config\references\inference-rules.md`.
- Modify: `C:\Users\wuzhao\.agents\skills\wuzhao-jenkins-generate-config\SKILL.md`.

**Interfaces:**
- Consumes: the stable canonical schema and CLI from Tasks 3–4.
- Produces: enough procedural guidance for a fresh agent to infer values, stop on ambiguity, and invoke the generator correctly.

- [ ] **Step 1: Write `entry-schemas.md` with a navigable contract**

Include a table of contents, the complete canonical JSON shape from Task 3, required fields by scenario, allowed stage combinations, the legacy/unified mapping tables, Java strategy rules, Web archive rules, readiness mapping, credentials rules, and one complete unified Java JSON example. State that historical `DEPLOY_PIPELINE.stepsBuildMaven` is input evidence only and is normalized to `stepsBuild.stepsBuildMaven` before generation.

- [ ] **Step 2: Write `inference-rules.md` with evidence precedence**

Use this exact precedence: explicit user values; existing Jenkins configuration; `pom.xml`/`package.json`; Git remote/current branch/repository name; user clarification. Document Maven packaging `war` → Tomcat, Spring Boot/JAR → Java service, package.json → Web, Gradle-only Java → unsupported, and multiple deliverable units → list candidates and stop. Mark environment, node labels, target paths, and Credentials IDs as non-inferable unless explicit project evidence exists.

- [ ] **Step 3: Finish `SKILL.md` around the observed baseline failures**

Keep the body below 500 lines. Add a compact evidence table contract with columns `field`, `value`, `source`, and `confidence`. Require explicit confirmation immediately before file generation. Reference `entry-schemas.md` only when building or validating the canonical spec, and `inference-rules.md` only during project inspection. Require the generator command from Task 2 and require absolute Windows paths.

Add these failure-specific rules:

- Existing output means ask for overwrite authorization.
- Multiple units mean present candidates and stop.
- Missing deployment environment/node/path/credential means ask instead of inserting placeholders.
- Never translate legacy root keys into the unified wrapper or unified `DELIVERY` into legacy wrappers.
- Never put a notification key/token in the canonical spec.

- [ ] **Step 4: Regenerate `agents/openai.yaml` deterministically**

Run `generate_openai_yaml.py` with the same three interface values from Task 2, then inspect that the default prompt explicitly mentions `$wuzhao-jenkins-generate-config` and no icons, brand color, dependencies, or policy fields were added.

- [ ] **Step 5: Run metadata and functional validation**

Run both `quick_validate.py` and the full `unittest` command. Expected: both exit with status 0.

**Checkpoint:** Build the scan expression from `('TO' + 'DO')`, `('T' + 'BD')`, `('fill' + ' in')`, `password`, and `BEGIN .*PRIVATE KEY`, then run `rg -n` across the skill folder. Expected: no authoring markers or secret material; instructional uses of the word `password` are acceptable only in prohibition/validation text.

---

### Task 6: Forward-Test the Installed Skill and Finalize

**Files:**
- Modify only skill files when a forward test exposes a real transferability gap.
- Create test outputs only under a new isolated temporary directory; remove that directory after inspection.

**Interfaces:**
- Consumes: installed `$wuzhao-jenkins-generate-config` and the same task shapes used in Task 1.
- Produces: evidence that the skill improves behavior and six valid generated Jenkinsfiles.

- [ ] **Step 1: Forward-test the unified Java scenario in a fresh subagent**

Prompt the subagent as a normal user: `Use $wuzhao-jenkins-generate-config at C:\Users\wuzhao\.agents\skills\wuzhao-jenkins-generate-config to solve` followed by the Task 1 unified Java request. Do not include the expected answer. The first response must inspect the project, report the evidence table, and request generation confirmation without writing. Send a second user-equivalent message `确认生成` to the same subagent, then verify it produces one unified Java file without legacy keys.

- [ ] **Step 2: Forward-test overwrite and ambiguity behavior in fresh subagents**

Reuse the Task 1 legacy Web and ambiguous monorepo task shapes. Verify that the first stops at an existing file without authorization and the second stops with a candidate list. Compare these outputs against the exact baseline gaps.

- [ ] **Step 3: Forward-test historical legacy Java recognition**

Create a fixture whose existing Jenkinsfile calls `deployJavaWeb` and stores Maven configuration directly at `DEPLOY_PIPELINE.stepsBuildMaven`. Ask the subagent to regenerate the legacy Java Jenkinsfile while preserving the observed application values. Verify its evidence table identifies the historical layout, and after a second `确认生成` message, the new file uses `DEPLOY_PIPELINE.stepsBuild.stepsBuildMaven` with no direct `DEPLOY_PIPELINE.stepsBuildMaven` entry.

- [ ] **Step 4: Close only observed loopholes and rerun the affected scenario**

If a subagent skips confirmation, guesses an ambiguous unit, mixes entry structures, or writes secrets/placeholders, update the positive workflow contract in `SKILL.md` or the relevant reference, then rerun the same scenario in a new fresh subagent. Do not add unrelated rules.

- [ ] **Step 5: Generate and inspect all six supported outputs in a temporary directory**

Run `generate_config.py` for legacy Java service, legacy Java Tomcat, legacy Web, unified Java service, unified Java Tomcat, and unified Web. Use a distinct subdirectory per scenario so every default output is named `Jenkinsfile.groovy`. Assert the expected call and forbidden root keys with `Select-String`, then delete the isolated test directory.

- [ ] **Step 6: Run the final deployment gates**

Run:

```powershell
conda run -n public python "C:\Users\wuzhao\.agents\skills\wuzhao-jenkins-generate-config\scripts\test_generate_config.py" -v
conda run -n public python "C:\Users\wuzhao\.codex\skills\.system\skill-creator\scripts\quick_validate.py" "C:\Users\wuzhao\.agents\skills\wuzhao-jenkins-generate-config"
```

Expected: both commands exit 0. Confirm the final folder contains only `SKILL.md`, `agents`, `assets`, `references`, and `scripts`; no baseline logs, temporary specs, generated Jenkinsfiles, README, changelog, or cache files remain.

- [ ] **Step 7: Produce the handoff**

Report the installed absolute skill path, tests run, six supported output combinations, default filename, overwrite behavior, and the fact that no Jenkins/deployment action occurred. Include SHA-256 hashes for the final skill files because the install target is not version-controlled.

---

## Plan Self-Review Checklist

- Every design requirement maps to a task: installation and metadata (Task 2), deterministic generation (Task 3), validation/security/overwrite (Task 4), inference and compatibility guidance (Task 5), baseline and forward testing (Tasks 1 and 6).
- Public names are consistent: `load_spec`, `validate_spec`, `build_config`, `to_groovy`, `render_template`, `render`, and `write_output`; CLI flags are `--spec`, `--output`, `--overwrite`, and `--validate-only`.
- The plan contains no unresolved authoring marker, implementation placeholder, or unsupported entry mode.
- Global skill files are not committed because `C:\Users\wuzhao\.agents` is not a Git repository; checkpoints use tests and SHA-256 hashes instead.
