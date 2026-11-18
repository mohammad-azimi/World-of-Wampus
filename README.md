# World of Wampus

World of Wampus is a Java Swing application for a knowledge-based cave agent. The hunter uses local percepts to infer safe rooms, collect the gold, and return to the entrance.

## Run in Windows CMD

Extract the ZIP. Open the extracted **WampusWorld** folder in VS Code and use a **Command Prompt** terminal. Java 17 or newer with desktop support must be on PATH. The project uses the Java standard library.

```cmd
java -jar WampusWorld.jar
```

You can also double-click **run.cmd** in File Explorer.

To rebuild after editing the source, use a JDK 17 or newer.

```cmd
build.cmd
java -jar WampusWorld.jar
```

## Demonstrate the assignment

1. Select **Lesson map**. The hunter starts at **(4,1)** facing east. The numbers are **(row, column)** and rows increase downward.
2. Press **Step** once to reach **(4,2)**. Breeze makes **(3,2)** and **(4,3)** pit candidates.
3. Press **Step** five more times. The turns and the walk via **(4,1)** bring the hunter to **(3,1)**. Open **Knowledge base** to see that **(3,2)** is safe, **(4,3)** is a pit, and the live Wumpus is at **(2,1)**.
4. At this point use **Shoot** to demonstrate the single arrow. The hunter is facing north, so the shot produces Scream. To show the default automatic gold run, select **Reset**, then **Auto run**.
5. Use **Reveal world** to display the physical layout. Click any room to inspect its hazard classification. The agent continues to use the same observations and rules while the layout is visible.

**Random map** takes the selected size from 2 to 10 and an integer seed. The same size and seed reproduce the layout. **Reset** restarts the current layout. The automatic policy stops at an unresolved frontier when it has neither a reachable safe room to explore nor a guaranteed shot. Manual controls let you continue the simulation.

| Key with the map focused | Manual action |
| --- | --- |
| Up arrow | Move forward |
| Left / right arrow | Turn 90 degrees |
| Space | Shoot the single arrow |
| G | Grab the gold in the current room |
| E | Exit from the entrance with the gold |

## Implementation

| File | Role |
| --- | --- |
| `Cave.java`, `Grid.java` | Cave placement, coordinates, neighbors, and arrow rays |
| `Game.java`, `Percept.java` | Actions, terminal states, and five sensor values |
| `KnowledgeBase.java` | Observation history, feasible Wumpus positions, and pit entailment |
| `Agent.java` | Breadth-first search through proven-safe rooms and action selection |
| `WampusApp.java`, `WorldPanel.java`, `CavePanel.java`, `Theme.java` | JFrame, controls, board, and presentation |
| `test/wampus/Verification.java` | Lesson and action checks, exhaustive inference comparison, and seeded policy runs |

Run the verification on Windows CMD with a JDK.

```cmd
if not exist build\verify mkdir build\verify
javac --release 17 -encoding UTF-8 -d build\verify src\wampus\*.java test\wampus\*.java
java -cp build\verify wampus.Verification
```

The reference map follows the diagrams in the uploaded **World of Wampus** assignment. Page 3 shows the hunter at (3,1) and the Wumpus at (2,1). Those coordinates are also consistent with the Stench observations on pages 2 and 3.
