plugins {
    id("org.danilopianini.gradle-pre-commit-git-hooks") version "2.1.23"
}

val conventionalCommitTypes = listOf(
    "fix",
    "feat",
    "build",
    "chore",
    "ci",
    "docs",
    "perf",
    "refactor",
    "revert",
    "style",
    "test",
    "major",
)

val conventionalCommitScopes = listOf(
    "backup",
    "boot",
    "clipboard",
    "commands",
    "configuration",
    "deps",
    "egg",
    "events",
    "exchange",
    "gradle",
    "inactivity",
    "keystore",
    "memory",
    "nest",
    "password",
    "passwordtree",
    "protein",
    "release",
    "security",
    "userinterface",
)

val conventionalCommitTypesForScript = conventionalCommitTypes.joinToString("\n") { "    $it" }
val conventionalCommitScopesForCase = conventionalCommitScopes.joinToString(" | ")

gitHooks {
    preCommit {
        tasks("preCommitCheck")
    }
    hook("pre-push") {
        from {
            """
            |set -e
            |./gradlew --no-configuration-cache prePushCheck
            |set +e
            """.trimMargin()
        }
    }
    commitMsg {
        from {
            """
            |types=(
            |$conventionalCommitTypesForScript
            |)
            |
            |msg_file="${'$'}1"
            |first_line="$(head -n 1 "${'$'}msg_file")"
            |
            |r_types="(${conventionalCommitTypes.joinToString("|")})"
            |r_scope_value='[a-z0-9-]+'
            |r_scope="(\\(${'$'}r_scope_value\\))?"
            |r_delim='!?:'
            |r_subject=" [[:graph:]].+"
            |pattern="^${'$'}r_types${'$'}r_scope${'$'}r_delim${'$'}r_subject${'$'}"
            |scope_regex="^[a-z]+\\((${'$'}r_scope_value)\\)!?: "
            |
            |if test -t 1 && test -n "$(tput colors)"; then
            |    RED='\033[0;31m'
            |    GREEN='\033[0;32m'
            |    BLUE='\033[0;34m'
            |    PURPLE='\033[0;35m'
            |    NC='\033[0m'
            |fi
            |
            |if ! grep -Eq "${'$'}pattern" "${'$'}msg_file"; then
            |    echo -e "${'$'}{RED}ERROR: Invalid commit message${'$'}{NC}:
            |${'$'}{PURPLE}$( cat "${'$'}msg_file" )${'$'}{NC}
            |"
            |    echo -e "
            |Your commit message does ${'$'}{RED}not${'$'}{NC} follow ${'$'}{PURPLE}Conventional Commits${'$'}{NC} formatting: ${'$'}{BLUE}https://www.conventionalcommits.org/${'$'}{NC}
            |Conventional Commits start with one of the following types:
            |    ${'$'}{GREEN}$(IFS=' '; echo "${'$'}{types[*]}")${'$'}{NC}
            |followed by an ${'$'}{PURPLE}optional scope within parentheses${'$'}{NC},
            |followed by an ${'$'}{RED}exclamation mark${'$'}{NC} (${'$'}{RED}!${'$'}{NC}) in case of ${'$'}{RED}breaking change${'$'}{NC},
            |followed by a colon (:),
            |followed by the commit message.
            |Example commit message fixing a bug non-breaking backwards compatibility:
            |    ${'$'}{GREEN}fix(passwordtree): preserve checksum verification on restore${'$'}{NC}
            |Example commit message adding a non-breaking feature:
            |    ${'$'}{GREEN}feat(protein): add update confirmation message${'$'}{NC}
            |Example commit message with a breaking change:
            |    ${'$'}{GREEN}refactor(commands)!: remove legacy command parser${'$'}{NC}
            |"
            |    exit 1
            |fi
            |
            |if [[ "${'$'}first_line" =~ ${'$'}scope_regex ]]; then
            |    scope="${'$'}{BASH_REMATCH[1]}"
            |    case "${'$'}scope" in
            |        $conventionalCommitScopesForCase)
            |            ;;
            |        *)
            |            cat <<'EOF'
            |ERROR: Invalid commit scope
            |
            |When a scope is used, it must be one of:
            |    ${conventionalCommitScopes.joinToString(", ")}
            |
            |Scopes are optional. Prefer them for software areas such as adapters
            |or focused domain/application changes.
            |
            |Examples:
            |    fix(passwordtree): preserve checksum verification on restore
            |    feat(protein): add update confirmation message
            |    chore(deps): update dependency gradle to v9.5.1
            |    ci: upload owasp report on failure
            |EOF
            |            exit 1
            |            ;;
            |    esac
            |fi
            """.trimMargin()
        }
    }
    createHooks(true)
}

rootProject.name = "Passbird"
