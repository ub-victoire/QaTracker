# QA Tracker

A small, menu-driven Java application for the Module 2 capstone. It manages manual
and automated test cases and restores them from a local file between sessions.

**Recommended environment:** JDK 25, Ubuntu, VS Code, and Git.
The source deliberately targets Java 21 for portability and works on JDK 25.

## 1. Run on Ubuntu

```bash
java -version
javac -version
bash check.sh
bash run.sh
```

No Maven, Gradle, database, internet access, or external Java dependency is needed.
- **Ntice** If using a file manager, scripts do not need executable permission when run with `bash`.

`run.sh` changes into the project root, compiles the source, and launches
`org.qatracker.Main`. The default save file is **testcases.xml in the project root**.
The first launch starts empty; the file is created by Save & Exit.

### Compile and launch manually

```bash
mkdir -p out/main
find src/main/java -name '*.java' -print > out/main-sources.txt
javac --release 21 -encoding UTF-8 -d out/main @out/main-sources.txt
java -cp out/main org.qatracker.Main
```

The `@out/main-sources.txt` argument tells the compiler to read the source-file
list from a file. Run the **class name**, not the `.java` path, to execute the
compiled program. With a manual launch, the default data path is relative to your
current working directory, so launch from the project root.

An optional path is useful for practice without touching the normal save:

```bash
bash run.sh practice-testcases.xml
```
This files are not commited
`testcases.xml` is already ignored by Git. Add other local data filenames to
`.gitignore` as needed.

## 2. Menu

```text
1. Add Test Case
2. View All Test Cases
3. Update Test Status
4. Search Test Cases
5. Generate Report
6. Save & Exit
```

### Add

Enter a title, description, preconditions, expected result, and type. Title and
expected result cannot be blank, including whitespace-only input. Type must be
`Manual` or `Automated` (case-insensitive). Invalid type input is re-prompted.

For a manual case, enter a tester name; for an automated case, enter a script path.
The application assigns the ID and starts the case at `NOT_RUN`.
An invalid title or expected result returns to the menu without creating a case
or consuming an ID. It does not create a partially populated object.

**Explicit interpretation of unspecified rules:** description, preconditions,
tester name, and script path may be blank because the brief marks only title,
expected result, and type as required. The type-specific field is always prompted
for, shown, and persisted even if blank. A script path is stored as text: the
program does not require it to exist or execute the script.

### View

The table shows ID, title, type, status, and tester/script detail. Empty collections
get a friendly message. Titles and details are not silently truncated; very long
values may extend beyond a narrow terminal window.

### Update

Enter an existing numeric ID, then choose `1 PASS`, `2 FAIL`, `3 BLOCKED`, or
`4 NOT_RUN`. Invalid statuses are re-prompted. An unknown ID produces a message and
returns to the menu. All four statuses are valid transitions from any current status.

### Search

Matches title **or** description case-insensitively, returning every match.
An empty or whitespace-only keyword returns all cases. No match gets a message.

### Report

Shows total cases, counts by type, counts by each status, and a one-decimal pass
percentage. The denominator is **all test cases**, including BLOCKED and NOT_RUN,
not just executed cases. Five passes out of eight cases display `62.5%`.
An empty report displays zero counts and `0.0%`.

### Save & Exit

Writes all fields and exits with a farewell only after a successful save. Restart
to verify restoration. A failed save keeps the application at the menu so you can
fix the path/permissions and retry. Normal input EOF also attempts to save completed
cases; an incomplete addition is discarded. Do not rely on Ctrl+C, a forced kill,
or a power failure to save unsaved in-memory changes.

## 3. Structure

```text
QaTracker/
├── .gitignore
├── .vscode/settings.json
├── README.md
├── run.sh
├── check.sh
├── docs/
│   ├── DESIGN.md
│   ├── DEMO-CHECKLIST.md
│   └── VERIFICATION.md
├── examples/demo-input.txt
├── tests/cli_checks.py
└── src/
    ├── main/java/org/qatracker/
    │   ├── Main.java
    │   ├── model/
    │   │   ├── TestCase.java
    │   │   ├── ManualTestCase.java
    │   │   ├── AutomatedTestCase.java
    │   │   ├── TestType.java
    │   │   ├── TestStatus.java
    │   │   └── ExecutionReport.java
    │   ├── service/TestCaseService.java
    │   ├── storage/TestCaseFileStore.java
    │   └── ui/ConsoleApp.java
    └── test/java/org/qatracker/QaTrackerTests.java
```

Generated `out/` and the runtime save file are not included in the source archive.
This is a conventional source layout without requiring a build tool.

## 4. Read the code in this order

1. `TestStatus` and `TestType`: enums limit valid values.
2. `TestCase`: shared fields, constructor rules, and abstract methods.
3. `ManualTestCase` and `AutomatedTestCase`: genuine specialization and overrides.
4. `TestCaseService`: list operations, IDs, status updates, search, and counts.
5. `ExecutionReport`: an immutable record carrying report results.
6. `ConsoleApp`: menu loop, line-based input, messages, and table/report output.
7. `TestCaseFileStore`: JDK XML properties, loading, and safe replacement on save.
8. `Main`: creates the collaborating objects and starts the application.
9. `QaTrackerTests`: executable examples of rules and edge cases.

See `docs/DESIGN.md` for the OOP explanation. You should be able to explain the
implementation yourself during the practical assessment; do not submit code you
cannot walk through. Follow your course's rules about external assistance.

## 5. Tests

```bash
bash check.sh
```

This compiles the project and runs 57 checks using plain Java. JUnit is not needed.
Checks use explicit failures, so the Java `-ea` assertion flag is not required.
Tests use temporary directories and do not alter your real save file.

For the optional terminal integration checks, install nothing extra if Python 3
is already on Ubuntu, compile first, then run:

```bash
bash check.sh
python3 tests/cli_checks.py
```

The Python helper uses only the standard library. It starts the actual Java
program in temporary working directories, runs 18 checks, and tests restart behavior.
It is a development tool, not a dependency of the application.

A scripted demo input is also included. Use a disposable data file, because
reusing an existing file would add another pair of sample cases:

```bash
bash run.sh demo-testcases.xml < examples/demo-input.txt
```

## 6. File persistence decisions

The assessment does not prescribe CSV, JSON, or any particular extension. This
implementation uses the JDK's `Properties.storeToXML` / `loadFromXML` API so Java
handles XML escaping, Unicode, delimiter characters, and multiline fields. There
is no hand-written CSV parser and no Java object deserialization.

Every case stores ID, title, description, preconditions, expected result, type,
status, and type-specific detail. A format version and count are included. IDs
resume from the largest restored ID plus one, not from the collection size.

A genuinely missing file is normal. A malformed, unreadable, or unsupported file
stops startup with a clear message rather than silently replacing your data with
an empty list. Back up the file before repair; no automatic destructive recovery
is performed. Save writes a temporary sibling file, then replaces the destination,
using an atomic move when the filesystem supports it. The non-atomic fallback is
not a guarantee against every possible power-loss scenario.

This is a **single-process learning application**. Do not run two copies against
the same save file; it has no cross-process locking or merge support. Saved data
is not encrypted. Use fictional examples and avoid storing credentials or secrets.

## 7. Git submission

Create a new independent repository at this project root, not inside another
exercise repository. Check first:

```bash
git rev-parse --show-toplevel
```

If no repository exists and no parent repository contains this folder:

```bash
git init -b main
git add .
git diff --cached --stat
git diff --cached
git commit -m "Build QA Tracker Java capstone"
```

Review the staged files: no `out/`, `.class`, runtime data, or editor backups.
Create an empty hosted repository under your account, add its actual URL as
`origin`, and push with `git push -u origin main`. Coordinate ownership and branch
workflow with your group; the assessment is a group project. Do not claim work
you did not do. Demonstrate each required feature and explain your contribution.
