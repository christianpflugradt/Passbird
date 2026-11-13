const parserOpts = {
  revertPattern: /^revert(?:\([^)]+\))?!?:\s"?([\s\S]+?)"?\s*This reverts commit (\w*)\./i,
  revertCorrespondence: ["header", "hash"],
};

export default {
  repositoryUrl: "https://github.com/christianpflugradt/Passbird.git",
  branches: ["main"],
  tagFormat: "${version}",
  plugins: [
    ["@semantic-release/commit-analyzer", {
      preset: "conventionalcommits", parserOpts,
      releaseRules: [
        { breaking: true, release: "major" },
        { type: "feat", release: "minor" },
        ...["fix", "perf", "chore", "refactor", "revert", "config"].map(type => ({ type, release: "patch" })),
        { type: "build", scope: "!release", release: "patch" },
        { type: "build", scope: null, release: "patch" },
        { type: "build", scope: "release", release: false },
        ...["ci", "docs", "style", "test"].map(type => ({ type, release: false })),
      ],
    }],
    ["@semantic-release/release-notes-generator", {
      preset: "conventionalcommits", parserOpts,
      presetConfig: {
        types: [
          { type: "feat", section: "Features" },
          { type: "fix", section: "Bug Fixes" },
          { type: "perf", section: "Performance Improvements" },
          { type: "chore", section: "Maintenance" },
          { type: "build", scope: "release", hidden: true },
          { type: "build", section: "Build System" },
          { type: "refactor", section: "Code Refactoring" },
          { type: "revert", section: "Reverts" },
          { type: "config", section: "Configuration" },
          ...["ci", "docs", "style", "test"].map(type => ({ type, hidden: true })),
        ],
      },
    }],
    "./.github/scripts/semantic-release-plugin.mjs",
    ["@semantic-release/github", {
      assets: [
        { path: "dist/passbird.jar", name: "passbird.jar", label: "Passbird executable JAR" },
        "dist/SHA256SUMS", "dist/metadata.json",
      ],
      successComment: false, failComment: false, releasedLabels: false, addReleases: false,
    }],
  ],
};
