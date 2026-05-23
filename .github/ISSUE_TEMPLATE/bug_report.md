---
name: Bug Report
about: Report a bug in the password manager
title: ""
labels: bug
assignees: ''

---

### **1. Summary**
- Concise problem statement:  
  _e.g., "Using the get command to retrieve a password does not copy it to the clipboard."_
- Impact:  
  _Who or what is affected, and why does it matter?_

### **2. Reproduction or Evidence**
- Steps to reproduce:  
  _e.g., "1. Open Passbird. 2. Use the get command to retrieve a saved password. 3. Observe that the password is not copied to the clipboard."_
- If this is a static review finding or non-interactive defect, describe the code path, workflow, or evidence instead:  
  _e.g., "The backup finalizer deletes old backups even when no new backup is written."_

### **3. Expected Behavior**
- What should happen instead?  
  _e.g., "The retrieved password should be copied to the clipboard automatically upon running the get command."_

### **4. Actual Behavior**
- What happens instead of the expected behavior?  
  _e.g., "Passbird logs the successful get action in the terminal, but the password is not copied to the clipboard."_

### **5. Suspected Cause or Proposed Fix** (Optional)
- If known, describe the likely cause or a preferred direction for the fix.  
  _e.g., "The clipboard adapter reports success even when the copy operation fails."_

### **6. Acceptance Criteria**
-  
-  

### **7. Environment**
- Provide details about the environment where the issue occurs:
    - Operating System:  
      _e.g., Windows 11 Pro 22H2 (Build 22621.1928), or `N/A (static code finding)`_
    - Java Version:  
      _e.g., OpenJDK 21.0.1, or `N/A (static code finding)`_
    - Passbird Version / commit / branch:  
      _e.g., 5.1.2 or `main @ abc1234`_

### **8. Additional Context (Optional)**
- Add any other relevant information, logs, or screenshots.  
  _e.g., "The previous content remains in the clipboard after using the get command."_
