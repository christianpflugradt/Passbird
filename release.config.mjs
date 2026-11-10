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
        { type: "major", release: "major" },
        { type: "feat", release: "minor" },
        ...["fix", "perf", "chore", "refactor", "revert"].map(type => ({ type, release: "patch" })),
        { type: "build", scope: "!release", release: "patch" },
        { type: "build", scope: null, release: "patch" },
        { type: "build", scope: "release", release: false },
      ],
    }],
    ["@semantic-release/release-notes-generator", {
      preset: "conventionalcommits", parserOpts,
      presetConfig: {
        types: [
          { type: "major", section: "Breaking Changes" },
          { type: "feat", section: "Features" },
          { type: "fix", section: "Bug Fixes" },
          { type: "perf", section: "Performance Improvements" },
          { type: "chore", section: "Maintenance & Dependencies" },
          { type: "build", scope: "release", hidden: true },
          { type: "build", section: "Build Adjustments" },
          { type: "refactor", section: "Refactoring" },
          { type: "revert", section: "Reverts" },
          ...["ci", "config", "docs", "style", "test"].map(type => ({ type, hidden: true })),
        ],
      },
    }],
    "./scripts/semantic-release-plugin.mjs",
    ["@semantic-release/github", {
      assets: [
        { path: "dist/passbird.jar", name: "passbird.jar", label: "Passbird executable JAR" },
        "dist/SHA256SUMS", "dist/metadata.json",
      ],
      successComment: false, failComment: false, releasedLabels: false, addReleases: false,
    }],
  ],
};
