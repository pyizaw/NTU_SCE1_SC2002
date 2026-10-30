# Restaurant Rush (SC2002 group assignment)

Turn-based restaurant management game for the command line, written in Java 17 (no external libraries, works offline).

**Current stage: 2 - customer behaviour** (Stage 1 plus VIP and Critic customers).

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
java -cp out restaurantrush.Main runs/stage2-victory.txt
```

```bash
java -cp out restaurantrush.Main runs/stage2-defeat.txt
```

The `runs/stage1-*` scripts belong to tag `stage-1` (all Regular customers).

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
* **Arrivals:** turn 4 is a **VIP** and turn 7 is a **Critic**; turns 1, 10, 13, 16 are Regular.

| Customer | Loss per unserved turn | Bill | Special behaviour |
|---|---|---|---|
| Regular | 8  | full price | - |
| VIP     | 5  | 10% off    | - |
| Critic  | 12 | full price | **Order preference:** the most demanding item (most preparation units, ties go to the cheaper one). **Reaction to service:** hates cold food - if the dish sat READY for more than one turn before being served, loses 20 satisfaction at once. |

* **Seating:** a seated customer is placed at the lowest-numbered free table.
* **Display order:** the start-of-turn board is printed right after step 1 (arrivals) so the manager can see the customer who has just arrived.
