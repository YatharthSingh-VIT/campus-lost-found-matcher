# Campus Lost & Found Matcher

A Java command-line app for a shared campus help desk. Students can record lost or
found items, search existing reports, and see possible matches with a score breakdown.

**Student:** Yatharth Singh

**Registration:** 25BAI10657

**Course:** Programming in Java

## Features

1. **Report management:** add, view, edit, delete, resolve, and reopen reports.
2. **Matching:** rank possible lost/found pairs using category, location, incident
   date, and keywords. Show why each candidate received its score.
3. **Search and reporting:** combine filters and view counts by type, status, and category.

Reports are saved in a local UTF-8 CSV file. The app validates input, handles save
errors, and prevents two app instances from editing the same dataset. No GUI is needed.

## Setup

1. Install **JDK 21 or newer**. Both `java` and `javac` must be on `PATH`.
   A JRE alone is not enough. An installation guide is available from
   [Microsoft OpenJDK](https://learn.microsoft.com/en-us/java/openjdk/install).
2. Open a new terminal and check:

   ```sh
   java -version
   javac -version
   ```

   Both must report version 21 or newer.
3. Clone this repository or extract its ZIP. Open a terminal in the folder
   containing this README. In Antigravity/Vs Code, use **File > Open Folder** to select it.
4. No external Java libraries, Maven, Gradle, database server, accounts, or API keys
   are needed. The data folder must be writable.

The project was tested on Windows with Microsoft OpenJDK 21.0.12. Commands for
macOS/Linux are included; those operating systems have not been tested.

## Run

Windows, from PowerShell or Command Prompt:

```powershell
.\run.cmd --demo --plain
```

macOS/Linux:

```sh
sh run.sh --demo --plain
```

These scripts compile the source and start the app. Omit `--demo` to use the empty
default dataset in `data/items.csv`. Omit `--plain` for optional terminal styling.

To compile and run directly from the project root:

```sh
javac -encoding UTF-8 --release 21 -d out "@sources.txt"
java -Dfile.encoding=UTF-8 -cp out com.lostfound.app.Main --demo --plain
```

Keep the quotes around `@sources.txt` in PowerShell. Recompile after changing Java code.

## Options and data

| Option | Purpose |
| --- | --- |
| `--demo` | Copy fictional examples to `data/demo-items.csv` on first use. Later runs keep your demo edits. |
| `--data "path/to/items.csv"` | Use a different CSV file. Cannot be combined with `--demo`. |
| `--plain` | Use ASCII borders with no ANSI colours. |
| `--color` | Force colour and Unicode borders on a compatible terminal. |
| `--help` | Show usage and exit. |

`--plain` takes priority over `--color`. The `NO_COLOR` environment variable also
disables styling. Completed changes save immediately. Exiting during an unfinished
form discards that form.

The default dataset is header-only. `examples/items.csv` contains seven fictional
reports. To reset a demo, close the app and copy that example file over
`data/demo-items.csv`. Keep real reports outside the repository with `--data`.

## Try the demo

1. Start with `--demo --plain`.
2. Choose **3**, then enter **L001**. You should see **F001 at 99/100** and
   **F002 at 63/100** with their score breakdowns.
3. Choose **4**, enter `bottle`, and leave the other filters blank to find three reports.
4. Choose **5** for statistics. The untouched demo has seven reports: three lost,
   four found, five open, and two resolved.
5. Choose **0** to exit.

Choose **2** to edit, resolve, reopen, or delete a report. Lists pause after five
records; press Enter for the next page. Use a full report ID when opening details.

![Matching results captured from the CLI](docs/results/matching.png)

This image renders actual program output; it is not a desktop screenshot.

## Tests

Windows:

```powershell
.\test.cmd
```

macOS/Linux:

```sh
sh test.sh
```

The scripts compile and run dependency-free Java tests. They use temporary data
and leave real and demo reports alone. A failed check exits with a nonzero code.
Success ends with **`SUCCESS: 77 checks passed.`** No JUnit or `-ea` flag is needed.

Checks cover CRUD, matching scores and boundaries, combined filters, dates, quoted
CSV, duplicate IDs, restart persistence, file locking, failed saves, and complete
CLI workflows. A benchmark also ranks 10,000 candidates for one report. Its timing
is a local sample, not an accuracy result or a guarantee on other machines.

Final recorded run: **77/77 passed on 15 September 2026**, with no compiler warnings
under `--release 21 -Xlint:all`. See [test output](docs/results/tests.txt).

## Matching rules

Candidates must be open reports of opposite types in the same category. The found
date must be on or after the lost date, within 30 days. Report IDs are compared
without regard to letter case.

| Evidence | Points |
| --- | ---: |
| Same category, required | 35 |
| Same normalized location | 25 |
| Date proximity | `round(25 * (30 - days) / 30)` |
| Keyword overlap | `round(15 * intersection / union)` |

Keywords come from the title and description. They are lowercased, split on
punctuation, and stored as sets. Short tokens and a small list of common words are
removed. Empty keyword sets earn zero points. Locations ignore case and extra spaces.

Scores of **60 or more** are shown in descending order, then by smaller day gap,
then by ID. The weights are design choices, not values trained on a campus dataset.
A score suggests a possible match; it does not prove ownership.

## Project structure

```text
lost-found-matcher/
├── README.md
├── statement.md
├── .gitignore
├── .gitattributes
├── run.cmd / run.sh
├── test.cmd / test.sh
├── sources.txt / test-sources.txt
├── data/items.csv
├── examples/items.csv
├── src/com/lostfound/
│   ├── app/Main.java
│   ├── model/Item.java, LostItem.java, FoundItem.java
│   ├── service/Matcher.java, ReportService.java
│   ├── storage/FileManager.java
│   └── util/ConsoleUI.java, ValidationUtil.java, InvalidInputException.java
├── test/com/lostfound/
│   ├── TestRunner.java, TestSupport.java, CliTest.java
│   ├── service/MatcherTest.java, ReportServiceTest.java
│   ├── storage/FileManagerTest.java
│   └── util/ValidationUtilTest.java
└── docs/
    ├── project-report.pdf
    ├── requirements.md
    ├── storage-design.md
    ├── diagrams/  (six diagrams)
    └── results/   (test output and two CLI result images)
```

`out/`, `test-out/`, local editor settings, demo working data, and lock files are
generated locally and excluded from Git. The hidden `.git` folder holds version
history; keep it when working with Git.

## Design and report

- [Requirements and acceptance criteria](docs/requirements.md)
- [Storage schema](docs/storage-design.md)
- [Architecture](docs/diagrams/architecture.png), [workflow](docs/diagrams/workflow.png),
  [use cases](docs/diagrams/use-case.png), [classes](docs/diagrams/class.png),
  [sequence](docs/diagrams/sequence.png), [ER/storage](docs/diagrams/er.png)
- [Project report: all 15 required sections](docs/project-report.pdf)
- [Statistics output](docs/results/statistics.png)

## Limits and troubleshooting

This is a shared-terminal prototype. Separate copies do not synchronize. CSV is
plaintext, and there are no accounts or automatic ownership checks. All records
are loaded into memory, and each change rewrites the file. Use a writable local
filesystem that supports atomic file replacement.

- **`javac` not found:** install a JDK, check `PATH`, and reopen the terminal.
- **Release 21 not supported:** the active compiler is too old.
- **Main class not found:** compile first and run from the project root.
- **File locked:** close the other instance using that CSV. A leftover lock file
  alone is harmless; the operating-system lock is released on exit.
- **Invalid CSV:** back up the file and repair the row shown in the error. The app
  stops loading instead of silently removing invalid records.
- **Save failed:** check file permissions and atomic-move support. A failed save
  does not change the service's current in-memory list.
- **Garbled borders:** use `--plain` and a UTF-8 terminal for non-ASCII report text.

## References

Java references: [javac](https://docs.oracle.com/en/java/javase/21/docs/specs/man/javac.html),
[LocalDate](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/LocalDate.html),
and [Files](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/nio/file/Files.html).
