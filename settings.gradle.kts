plugins {
    id("org.danilopianini.gradle-pre-commit-git-hooks") version "2.1.7"
}

gitHooks {
    preCommit {
        tasks("preCommitCheck")
    }
    commitMsg {
        conventionalCommits {
            defaultTypes()
            types("major")
        }
    }
    createHooks(true)
}

rootProject.name = "Passbird"
