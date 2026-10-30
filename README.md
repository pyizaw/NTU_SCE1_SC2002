# Restaurant Rush (SC2002 group assignment)

Turn-based restaurant management game for the command line, written in Java 17 (no external libraries, works offline).

**Current stage: 1 - basic restaurant operation** (Regular customers, menu, Waiter, Chef, Cashier, tables, waiting queue, full turn sequence, victory and defeat).

## Build and run

IntelliJ IDEA: *File > Open* this folder, set the Project SDK to any JDK 17+ if asked, then run the **Play** run configuration.

Command line (from this folder):

```bash
javac --release 17 -d out $(find src -name "*.java")
```

```bash
java -cp out restaurantrush.Main
```

Windows PowerShell compile: `javac --release 17 -d out (Get-ChildItem -Recurse src -Filter *.java).FullName`

Replay a recorded run (commands are read from the file and echoed):

```bash
java -cp out restaurantrush.Main runs/stage1-victory.txt
```

```bash
java -cp out restaurantrush.Main runs/stage1-defeat.txt
```

## How to choose

Each staff member's possible tasks are listed as numbered options. Type the **option number** or the **command**:

| Staff   | Commands                                       |
|---------|------------------------------------------------|
| Waiter  | `seat n`, `order n`, `serve n`, `wait`         |
| Chef    | `cook n`, `wait`                               |
| Cashier | automatic (one payment per turn, oldest first) |

`n` is the customer number (an order has the same number as its customer). Short forms: `s`, `o`, `v`, `c`, `w`. Press Enter when `wait` is the only option. Type `help` at any prompt.

## Documented rules

* **Food choice:** customer number *n* orders the menu item at position *(n - 1) mod menu size*, so customers cycle Salad, Burger, Pasta, Salad, Burger, Pasta.
* **Seating:** a seated customer is placed at the lowest-numbered free table.
* **Display order:** the start-of-turn board is printed right after step 1 (arrivals) so the manager can see the customer who has just arrived.
