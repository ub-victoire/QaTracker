# Verification performed for this delivery

## Environment and limits

The source was compiled and executed in the build environment using OpenJDK
21.0.12.1. Compilation used source/target 21 with `-Xlint:all -Werror` and completed
without warnings. The environment's reduced Java installation lacked the release
symbol archive, so the delivered `--release 21` command was not used directly in
that environment. On a normal full JDK 21 or JDK 25, `--release 21` is the supported
way to target that language/API release.

No JDK 25 runtime was available here, so this is **not a claim of an actual JDK 25
test run**. The code uses standard Java 21 APIs and bytecode compatible with JDK 25.
Run `bash check.sh` and `python3 tests/cli_checks.py` on your machine to confirm
both in your exact environment.

## Results

- 57 Java checks passed: model/service validation, object types, statuses, search,
  report counts, 62.5% pass rate, zero report, immutable list structure, ID
  exhaustion/gaps, and file round trips.
- Persistence tests restored all fields for both types, including Unicode,
  delimiters, quotes, newlines, empty optional detail, status, and continued IDs.
- File checks rejected malformed XML, unsupported versions, invalid saved enums,
  blank required fields, invalid IDs, duplicate IDs, missing fields, and invalid
  record counts; failed reads preserved original content.
- 18 end-to-end terminal checks passed, including menu/type/status errors,
  whitespace-only required fields, missing IDs, search behavior, empty output,
  saving/restarting, default project-local data creation, restored subtype fields,
  continued IDs, 33.3% rounding, EOF, corrupt-file startup, and failed-save handling.
- A save-failure integration check verified that the CLI did not print a false
  success/farewell when the destination became unwritable as a regular file.

These are development checks, not a guarantee against every possible input,
filesystem failure, abrupt process kill, or concurrent writer. See README.md for
scope and persistence limitations. The Python checks run in temporary directories
and the Java suite removes its temporary fixtures.
