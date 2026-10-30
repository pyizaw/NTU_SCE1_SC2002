# Restaurant Rush (SC2002 group assignment)

Turn-based restaurant management game for the command line, written in Java 17 (no external libraries, works offline).

**Current stage: 3 - new role and menu policy** (Stage 2 plus the Host, ComboMeal and Happy Hour).

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
java -cp out restaurantrush.Main runs/stage3-victory.txt
```

```bash
java -cp out restaurantrush.Main runs/stage3-defeat.txt
```

Each script only works at its own tag (`runs/stage1-*` at `stage-1`, `runs/stage2-*` at `stage-2`), because later stages add prompts (Host, Happy Hour).

## How to choose

Each staff member's possible tasks are listed as numbered options. Type the **option number** or the **command**:

| Staff   | Commands                                       |
|---------|------------------------------------------------|
| Happy Hour | `y` / `n` (Enter = no) at the start of each turn until it has been used |
| Host    | `seat n`, `wait` - acts before the Waiter      |
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

* **Combo choice:** the Stage 3 menu adds Salad + Burger Combo $21.60 (2 units), Salad + Pasta Combo $24.30 (3), Burger + Pasta Combo $29.70 (3) after the three dishes. Combos are ordinary menu items, so the food-choice rule above picks them: customers #4, #5, #6 order the three combos, and the Critic's rule picks the Salad + Pasta Combo. Price = 90% of the two components rounded to the nearest cent; preparation units = sum of the components.
* **Host:** seats at most one queued customer per turn, before the Waiter acts; the Waiter can still seat customers.
* **Happy Hour:** offered at the start of each turn until used (once per game). Lasts that turn and the next; the remaining duration is shown. Orders taken while it is active get 20% off, applied after the VIP discount; the price is locked when the order is taken. At the start of each active turn, customers waiting for a table or for food recover 15 satisfaction (max 100).
* **Seating:** a seated customer is placed at the lowest-numbered free table.
* **Display order:** the start-of-turn board is printed right after step 1 (arrivals) so the manager can see the customer who has just arrived.
