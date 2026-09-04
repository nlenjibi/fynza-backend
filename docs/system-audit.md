SECURITY AUDIT PROCESS
Step 1 — System Overview

Understand:

Code functionality
Input sources
Output destinations
Data flows
Authentication logic
External dependencies
Step 2 — Vulnerability Detection

Check for common issues:

Injection Attacks
SQL injection
Command injection
NoSQL injection
Authentication Issues
Weak password policies
Session management flaws
Hardcoded credentials
Authorization Issues
Missing access checks
Privilege escalation
Cryptography Problems
Plaintext secrets
Weak algorithms
Missing encryption
Configuration Problems
Debug mode enabled
Verbose error messages
Default credentials
Dependency Risks
Outdated libraries
Known CVEs

For each vulnerability record:

| Location | Type | Description | Severity |

Step 3 — Exploitation Scenarios

Explain how attackers could exploit critical vulnerabilities.

Include realistic attack scenarios.

Step 4 — Business Impact

Assess potential consequences:

Data exposure
Financial loss
Compliance violations
Reputation damage
Step 5 — Remediation Plan

Provide actionable fixes.

Include:

| Issue | Fix | Priority | Effort |

Step 6 — Secure Code Examples

Provide before/after code where applicable.

Explain why the fix works.

Step 7 — Security Report
SECURITY AUDIT REPORT

Executive Summary

Key Findings

Detailed Vulnerabilities

Exploitation Scenarios

Remediation Plan

Secure Code Examples

Audit Methodology
Security Audit Rules
Avoid false positives.
Provide precise vulnerability locations.
Only report exploitable issues.
Ensure fixes match the programming language.
INPUT FORMAT
CODE
[Paste code]

CONTEXT
[Application type, data sensitivity, deployment environment]