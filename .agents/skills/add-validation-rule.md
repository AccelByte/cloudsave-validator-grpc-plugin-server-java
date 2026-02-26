---
name: add-validation-rule
description: Add a new cloud save validation rule. Scaffolds the validator function, wires it into the handler, and adds a test case.
user-invocable: true
allowed-tools: Bash, Read, Glob, Grep, Edit, Write
argument-hint: <rule description, e.g. "reject saves where player health exceeds max_health">
---

# Add Validation Rule

Add a new cloud save validation rule to this Extend Override app. Scaffolds the validator logic, wires it into the validation handler, and adds a test case.

## Arguments

`$ARGUMENTS`

Parse for:
- **Rule description**: What the validation checks (required)
- Field names, constraints, or game state references if provided

## Process

### Step 1: Understand existing validators
Read the service implementation file (see Key Files in `AGENTS.md`) to understand:
- How existing validation rules are structured
- How validators are registered and dispatched
- The request/response format from the proto

### Step 2: Design the validation rule
Based on the user's description, propose:
- Which fields to validate
- What conditions to check
- What error message to return on failure

Show the user the design and ask for confirmation.

### Step 3: Implement the validator
Add the validation logic following existing patterns:
- Match the code style of existing validators
- Handle edge cases (missing fields, null values, type mismatches)
- Return clear, actionable error messages

### Step 4: Wire into handler
If validators are registered in a dispatch table or switch statement, add the new rule there. Follow the existing registration pattern exactly.

### Step 5: Add test case
Create a test that covers:
- Valid input (should pass)
- Invalid input matching the new rule (should fail with correct error)
- Edge cases specific to the rule

### Step 6: Verify
- Build the project
- Run all tests (existing + new)
- Show the user the complete diff

## Important

- **Don't modify the proto file** — this is an Override app, proto is AccelByte-provided
- **Match existing validation patterns** — consistency matters more than cleverness
- **Test both pass and fail cases** — validators must correctly accept valid data too
