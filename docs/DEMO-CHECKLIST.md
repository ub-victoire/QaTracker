# Assessment demonstration checklist

Run from the project root. Use a fresh disposable data filename for a repeatable
live demonstration. Do not delete an existing real save without a backup.

## Preparation

- Run `bash check.sh` and show the checks passing.
- Run `bash run.sh assessment-demo.xml` with a previously unused demo filename.
- Explain the hierarchy and package responsibilities using DESIGN.md.

## Live actions and expected results

| Action | Expected result |
|---|---|
| View all before adding anything | Friendly empty message |
| Generate report before adding anything | All zeros; pass rate 0.0% |
| Enter `abc` as the menu choice | Whole-number error; menu continues |
| Enter menu choice `9` | Range message; menu continues |
| Add with a whitespace-only title | Clear error; no record created |
| Add with a whitespace-only expected result | Clear error; no record created |
| Add a manual case with title Login works, description Valid credentials, precondition Account exists, expected Dashboard opens, tester Alex | ID 1, MANUAL, NOT_RUN |
| Enter an invalid type before the valid type | Re-prompt; no invalid type accepted |
| Add an automated case with title API search, description Find LOGIN field, expected 200 response, script tests/search.java | ID 2, AUTOMATED, NOT_RUN |
| View all | Both records and both type-specific details visible |
| Update ID 999 | Clear missing-ID message; menu continues |
| Update ID 1; enter `abc`, then `8`, then `1` | Invalid status choices rejected; final status PASS |
| Update ID 2 to FAIL | Status becomes FAIL |
| Search `lOgIn` | Both cases match, one by title and one by description |
| Search an empty keyword | Both cases returned |
| Search a keyword absent from both cases | No-match message |
| Generate report | Total 2, Manual 1, Automated 1, PASS 1, FAIL 1, pass rate 50.0% |
| Save & Exit | Successful save and farewell |
| Restart with the same demo filename; view all | Both types, IDs, statuses, and details restored |
| Add another valid case | ID continues at 3 |
| Set statuses to BLOCKED and NOT_RUN | Both allowed; report counts update |
| Save & Exit again | Successful replacement save |

## Code review talking points

- Why ManualTestCase extends TestCase, but ConsoleApp does not extend the service.
- Why TestCase is abstract and its fields are private.
- Which methods are overridden and how polymorphism is used during display.
- Why records and enums fit their particular jobs.
- Why pass rate includes NOT_RUN cases in the denominator.
- Why next ID is restored from the maximum ID rather than list length.
- Why invalid additions do not consume IDs.
- Why the file loader refuses to silently overwrite corrupted data.
- How the temporary-file save reduces the risk of truncating a previous save.
- Why Save & Exit is required before an ordinary restart persistence demonstration.

## Suggested group work, not a claimed history

If your group is still dividing responsibilities, one member can own the model
and service, one the CLI, one persistence, and one tests/documentation. Review
interfaces together and have everyone explain the complete add-save-reload flow.
Record your actual contributions rather than presenting this suggestion as work
already completed.
