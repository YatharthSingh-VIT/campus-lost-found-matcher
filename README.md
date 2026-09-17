# Campus Lost & Found Matcher

A Java terminal application for recording lost and found items on campus. It keeps reports in one place, suggests possible matches, and shows how each match was scored.

| Project details | |
| --- | --- |
| Student | Yatharth Singh |
| Registration number | 25BAI10657 |
| Course | Programming in Java |
| Platform | Command line, Java 21 or later |
| Storage | Local CSV files |

## What the application does

- **Manage reports:** create, view, edit, delete, resolve, and reopen lost or found reports.
- **Find possible matches:** compare category, location, dates, and description keywords. Results include the points awarded for each factor.
- **Search and review:** combine filters and view totals by report type, status, and category.

The application is intended for a shared campus help-desk terminal. A match is a suggestion for someone to investigate; ownership must still be checked in person.

## Requirements

Install **JDK 21 or later** and make sure both `java` and `javac` are available in your terminal. A JRE alone cannot compile the project.

```sh
java -version
javac -version
```

Both commands must report version 21 or later. See the [Microsoft OpenJDK installation guide](https://learn.microsoft.com/en-us/java/openjdk/install) if you need to install a JDK.

No Maven, Gradle, external Java libraries, database server, or API keys are required. The folder used for data must be writable.

## Setup and first run

1. Download and extract the repository ZIP, or clone the repository using an account with access.
2. Open a terminal in the extracted project folder. You should see `README.md`, `sources.txt`, and the `src` folder there.
3. Run the appropriate command below. The script compiles the code and starts the application with seven fictional sample reports.

**Windows - PowerShell or Command Prompt**

```powershell
.\run.cmd --demo --plain
```

**macOS or Linux**

```sh
sh run.sh --demo --plain
```

To start with an empty dataset, omit `--demo`. Saved reports are loaded again on the next run.

An IDE is optional. In Antigravity, open the folder containing this README and use its terminal for the same commands.

### Compile and run without scripts

Run these commands from the project root:

```sh
javac -encoding UTF-8 --release 21 -d out "@sources.txt"
java -Dfile.encoding=UTF-8 -cp out com.lostfound.app.Main --demo --plain
```

Keep the quotes around `@sources.txt` in PowerShell. Recompile whenever the Java source changes.

## A short demonstration

With the original demo data:

| Action | Input | Expected result |
| --- | --- | --- |
| Find matches | Choose `3`, enter `L001` | `F001`: **99/100**; `F002`: **63/100** |
| Search reports | Choose `4`, enter `bottle`, leave the other filters blank | Three reports |
| View statistics | Choose `5` | Seven reports: three lost, four found, five open, two resolved |
| Exit | Choose `0` | Application closes |

Choose `1` to add a report or `2` to manage an existing report. Enter dates as `YYYY-MM-DD`. Lists show five records at a time; press Enter when prompted to continue.

The demo is editable. If its results differ after you have changed reports, close the application and copy `examples/items.csv` over `data/demo-items.csv` to restore the sample data. This replaces any demo edits.

## Data and command-line options

| Option | Behaviour |
| --- | --- |
| `--demo` | Creates `data/demo-items.csv` from the examples on first use, then keeps subsequent changes. |
| `--data "path/to/items.csv"` | Uses a separate CSV file. Cannot be combined with `--demo`. |
| `--plain` | Uses ASCII borders without colour. |
| `--color` | Enables colour and Unicode borders on compatible terminals. |
| `--help` | Prints the usage information. |

Without a data option, reports are stored in `data/items.csv`. The supplied file contains only the column header. Each completed change is saved immediately; an unfinished form is discarded if input ends.

`--plain` overrides `--color`. Setting the `NO_COLOR` environment variable also disables styling. Use a UTF-8 terminal for accented characters. Keep real student records outside the repository using `--data`.

## How matching works

The application compares open reports of opposite types in the same category. The found date must be on or after the lost date, with a gap of no more than 30 days. A report cannot match another report with the same ID; ID checks ignore letter case.

| Factor | Points |
| --- | ---: |
| Same category | 35 |
| Same location, ignoring case and extra spaces | 25 |
| Date proximity | `round(25 × (30 − day gap) / 30)` |
| Shared keywords | `round(15 × shared words / distinct words across both reports)` |

Keywords come from the title and description. Repeated words count once. Punctuation, short tokens, and a small set of common words are removed. If both keyword sets are empty, the keyword score is zero.

Results scoring **60 or more** are shown in score order. Ties use the smaller date gap, then the candidate ID. These rules and weights are fixed design choices; the score is not a probability of ownership.

## Testing

All automated checks are in `test/ProjectTest.java`.

**Windows**

```powershell
.\test.cmd
```

**macOS or Linux**

```sh
sh test.sh
```

The scripts compile the application and tests, then print each check. A successful run ends with:

```text
SUCCESS: 77 checks passed.
```

The checks cover matching rules, invalid input, report operations, CSV persistence, duplicate IDs, file locking, failed saves, and complete terminal workflows. A timing check also ranks 10,000 sample candidates for one report. Tests use temporary files and do not change the normal or demo datasets. A failure produces a nonzero exit code.

Testing was performed on Windows with Microsoft OpenJDK 21.0.12. The macOS/Linux commands are provided but have not been verified on those systems. Detailed results and limitations are recorded in the project report.

## File structure

```text
lost-found-matcher/
├── README.md
├── statement.md
├── .gitignore
├── .gitattributes
├── sources.txt
├── run.cmd / run.sh
├── test.cmd / test.sh
├── data/items.csv
├── examples/items.csv
├── src/com/lostfound/
│   ├── app/Main.java
│   ├── model/Item.java, LostItem.java, FoundItem.java
│   ├── service/Matcher.java, ReportService.java
│   ├── storage/FileManager.java
│   └── util/ConsoleUI.java, ValidationUtil.java, InvalidInputException.java
├── test/ProjectTest.java
└── docs/project-report.pdf
```

There are ten application source files and one test source file. Compilation creates `out` and `test-out`; these folders are excluded from Git along with local editor settings and working demo files.

The [problem statement](statement.md) describes the scope and intended users. The [project report](docs/project-report.pdf) contains the required 15 sections, six design diagrams, storage schema, sample output, and testing results.

## Limitations and troubleshooting

This version stores all reports locally. Different installations do not share data. There are no user accounts, notifications, image matching, or automatic ownership checks. Searches scan reports in memory, and each saved change rewrites the CSV file.

| Problem | What to check |
| --- | --- |
| `javac` is not found | Install a JDK, add its `bin` folder to `PATH`, and reopen the terminal. |
| Release 21 is not supported | Check which compiler `javac -version` reports. |
| Main class cannot be found | Compile first and run from the project root. |
| Data file is locked | Close the other application instance using the same dataset. |
| CSV cannot be loaded | Back up the file and correct the row identified in the error. |
| Saving fails | Check folder permissions and use a local filesystem that supports atomic file replacement. |
| Borders or colours display incorrectly | Start with `--plain`. |

Invalid CSV files are left unchanged. Failed saves leave the service's current records unchanged. The app requires atomic file replacement; it reports an error when that operation is unsupported.

## References

- [Oracle: javac command](https://docs.oracle.com/en/java/javase/21/docs/specs/man/javac.html)
- [Oracle: LocalDate](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/LocalDate.html)
- [Oracle: Files](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/nio/file/Files.html)
