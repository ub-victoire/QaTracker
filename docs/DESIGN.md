# Design and OOP walkthrough

## Responsibilities

| Component | Responsibility | Deliberately does not do |
|---|---|---|
| Main | Wire objects together and load saved data | Parse menu commands |
| ConsoleApp | Prompt, parse input, print output | Read or write the XML format |
| TestCaseService | Business operations and ID allocation | Read from System.in |
| TestCaseFileStore | Load/save complete records | Decide menu behavior |
| TestCase hierarchy | Represent cases and enforce core validity | Manage a collection of cases |
| ExecutionReport | Carry calculated counts | Print to the terminal |

This separation keeps changes local without adding controllers, factories,
repositories, dependency-injection frameworks, or interfaces used only once.

## The inheritance relationship is genuine

A ManualTestCase **is a** TestCase. An AutomatedTestCase **is a** TestCase.
This is a sounder inheritance relationship than making unrelated domains extend
one another just to reuse methods.

TestCase is abstract, so you cannot instantiate it directly. Shared fields live
in one place, and each concrete subtype supplies its own type, detail label, and
specific detail. `super(...)` initializes the base state. Constructors are not
inherited. Creating a subtype initializes one object through the constructor chain.

Both subclasses are final because this application has no further specialization.
TestCase is not sealed: sealing is unnecessary for this brief and would introduce
extra learning without fixing a current problem.

## Encapsulation

Fields are private. IDs and descriptive data are final. Status changes go through
an explicit method. Constructors reject invalid core data, so file reload and
interactive additions obey the same fundamental model rules. The service controls
ID generation and only increments the next ID after creation succeeds.

The service returns an unmodifiable list snapshot from getAll(). The TestCase
objects themselves still allow explicit status changes: it is not a deep immutable
snapshot. For this small application, ConsoleApp uses the service for updates.

## Polymorphism and abstraction

The collection is `List<TestCase>` but holds both concrete subtypes. The table and
file store call `getType()`, `getDetailLabel()`, and `getDetail()` on TestCase
references. Java invokes the implementation for the runtime object type.
The caller does not need repeated casts or instanceof checks to display details.

The file loader does need a type switch to construct the right subtype, because
it starts from a stored type value rather than an existing Java object.

## Composition

ConsoleApp has a service, a file store, and a Scanner. These are collaborating
objects passed through its constructor. ConsoleApp should not extend the service
just to use its methods: it is not a kind of TestCaseService.

## Modern Java used where it helps

- Enums prevent arbitrary type/status strings inside the program.
- A record carries immutable report values without repetitive getter code.
- Switch expressions map valid type/status choices clearly.
- Local `var` is used when the initializer makes the type apparent.
- try-with-resources closes file streams.
- Path and Files provide standard file operations.
- List.copyOf protects the collection structure exposed to callers.

No preview flags or Java 25-specific syntax are required. This is intentional
portability, not a request to replace your JDK 25 installation.

## Execution flow: add then save

1. Main loads the file or starts with an empty collection if it is absent.
2. ConsoleApp reads complete lines, avoiding Scanner nextInt/nextLine mixing.
3. The service constructs the appropriate subtype and assigns a unique ID.
4. The constructor validates the required fields.
5. The service appends the complete object and advances its next ID.
6. Save & Exit asks the file store to persist all records.
7. On the next launch, the loader reconstructs the correct concrete subtypes.

## Error handling

Validation errors produce actionable messages instead of stack traces. Invalid
numeric input does not terminate the menu. Expected file failures are caught as
IOExceptions. A malformed file aborts loading rather than returning partial data,
which avoids accidental overwrite of records that could not be parsed.

We do not catch every RuntimeException or Error: concealing programming bugs and
out-of-memory failures would not be sound error handling. This is a small local
application, not a hostile-input internet service.

## Deliberate scope limits

No delete/edit-description feature, login, GUI, database, script execution,
concurrent writers, or autosave is required. The code does not add these features.
The table prints full values; very long strings can exceed a narrow terminal.
Startup error messages currently return normally rather than exposing a separate
nonzero command-line exit-code contract; this assessment targets interactive use.
