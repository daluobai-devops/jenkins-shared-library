package com.daluobai.jenkinslib.delivery

class JenkinsSourceRepositoryAdapter implements SourceRepositoryAdapter {
    private final def steps

    JenkinsSourceRepositoryAdapter(def steps) {
        this.steps = steps
    }

    @Override
    String resolveRevision(Map source) {
        String repository = shellQuote(source.repository?.toString())
        String reference = shellQuote(source.reference?.toString())
        String command = """
set -eu
probe_dir=\$(mktemp -d)
trap 'rm -rf "\$probe_dir"' EXIT
git -C "\$probe_dir" init -q
git -C "\$probe_dir" fetch -q --depth=1 ${repository} ${reference}
git -C "\$probe_dir" rev-parse FETCH_HEAD
""".stripIndent()
        return withSourceCredentials(source) {
            steps.sh(script: command, returnStdout: true).toString().trim()
        } as String
    }

    @Override
    boolean directoryExists(Map source, String revision) {
        String repository = shellQuote(source.repository?.toString())
        String pinnedRevision = shellQuote(revision)
        String directory = shellQuote(source.directory?.toString())
        String command = """
set -eu
probe_dir=\$(mktemp -d)
trap 'rm -rf "\$probe_dir"' EXIT
git -C "\$probe_dir" init -q
git -C "\$probe_dir" fetch -q --depth=1 ${repository} ${pinnedRevision}
git -C "\$probe_dir" cat-file -e "FETCH_HEAD:${directory}"
""".stripIndent()
        if (source.directory?.toString() == '.') {
            return true
        }
        return withSourceCredentials(source) {
            steps.sh(script: command, returnStatus: true) == 0
        } as boolean
    }

    @Override
    void checkout(Map source, String revision) {
        Map remote = [url: source.repository.toString()]
        if (source.credentialsId) {
            remote.credentialsId = source.credentialsId.toString()
        }
        steps.checkout(changelog: true, poll: false, scm: [
                '$class'          : 'GitSCM',
                branches          : [[name: revision]],
                userRemoteConfigs : [remote],
                extensions        : [
                        ['$class': 'CleanBeforeCheckout'],
                        ['$class': 'RelativeTargetDirectory', relativeTargetDir: 'source']
                ]
        ])
    }

    private static String shellQuote(String value) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException('源码参数为空')
        }
        return "'${value.replace("'", "'\\''")}'"
    }

    private def withSourceCredentials(Map source, Closure action) {
        if (source.credentialsId) {
            try {
                return steps.withCredentials([steps.sshUserPrivateKey(
                        credentialsId: source.credentialsId.toString(),
                        keyFileVariable: 'DELIVERY_SOURCE_SSH_KEY'
                )]) {
                    steps.withEnv([
                            "GIT_SSH_COMMAND=ssh -i '${steps.env.DELIVERY_SOURCE_SSH_KEY}' -o IdentitiesOnly=yes -o StrictHostKeyChecking=accept-new"
                    ]) {
                        action.call()
                    }
                }
            } catch (MissingMethodException | NoSuchMethodError ignored) {
                // 没有安装 Credentials Binding 插件时保留匿名/宿主机 SSH 的历史回退行为。
            }
        }
        return action.call()
    }
}
