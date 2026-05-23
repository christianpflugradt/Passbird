---
name: Review Finding
about: Track an actionable review finding with enough context for a follow-up agent
title: ""
labels: finding
assignees: ''

---

### **1. Review Context**
- Review area:  
  _e.g., `security`, `architecture`, `integrity`, `behavior`, or `delivery`_
- Review trigger:  
  _e.g., current task, diff, incident, workflow failure, or release concern_
- Review date:  
  _e.g., `2026-05-23`_
- Branch / commit / PR / workflow context:  
  _Link or describe the relevant branch, commit, PR, workflow, or run_

### **2. Finding Summary**
- Priority:  
  _e.g., `P1`_
- Area label to add after issue creation:  
  _e.g., `security`, `architecture`, `integrity`, `behavior`, or `delivery`_
- Concise problem statement:  
  _Describe the defect or gap in one or two sentences._
- Why this was flagged during review:  
  _Explain what broader guarantee, workflow, or expectation is at risk._

### **3. Evidence**
- Files and locations:  
  _List the relevant files and line references._
- Commands, workflows, runs, or issues inspected:  
  _List the GitHub CLI commands, workflow runs, or related issues that support the finding._
- Observed behavior or gap:  
  _Describe what currently happens._
- Why the current state supports the finding:  
  _Connect the evidence to the conclusion._

### **4. Impact**
- User, security, integrity, or delivery impact:  
  _Describe who or what is affected._
- Failure mode or attack path:  
  _Explain how this can fail in practice._
- Why this priority fits:  
  _Justify the chosen `P0` to `P3` severity._

### **5. Proposed Fix**
- Recommended change:  
  _Describe the preferred fix._
- Constraints and guardrails:  
  _Call out architecture, security, compatibility, or wording constraints._
- Non-goals:  
  _List tempting adjacent changes that should stay out of scope._

### **6. Acceptance Criteria**
-  
-  

### **7. Handoff Notes**
- Suggested starting points:  
  _Name the files, tests, workflows, or issue threads another agent should open first._
- Verification to run:  
  _List the focused checks that should confirm the fix._
- Open questions or follow-ups:  
  _Capture any unresolved details that matter for the implementation._
