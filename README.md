# World of Wampus

A Java Swing desktop simulator for Wumpus World. A knowledge-based agent explores a hidden cave, infers which rooms are safe from local clues, and uses breadth-first search (BFS) to navigate. The goal is to collect the gold and return safely to the entrance.

The interface shows the current sensor readings, the agent’s deductions, and the reason behind each automatic action.

## Run on Windows

Install **JDK 17 or newer** and make sure `java`, `javac`, and `jar` are on `PATH`. The project uses the Java standard library.

Clone the repository:

```cmd
git clone https://github.com/mohammad-azimi/World-of-Wampus.git
```

Open the cloned `World-of-Wampus` folder in VS Code. Select **Command Prompt** as the terminal profile and run:

```cmd
build.cmd
run.cmd
```

`build.cmd` compiles the source and creates `WampusWorld.jar`. `run.cmd` opens the Swing application.

## Try the Lesson Map

1. Select **Lesson map**. The hunter starts at `(4,1)` facing east. Coordinates are written as `(row, column)`.
2. Press **Step** once to enter `(4,2)`. A breeze makes `(3,2)` and `(4,3)` possible pit locations.
3. Press **Step** five more times. At `(3,1)`, open **Knowledge base**. The agent has deduced that `(3,2)` is safe, `(4,3)` contains a pit, and the Wumpus is at `(2,1)`.
4. At this point, press **Shoot** to demonstrate the single arrow. The hunter faces north, so the shot hits the Wumpus and produces a **Scream**.
5. Select **Reset** and then **Auto run** to watch the agent collect the gold and return to the entrance.

Use **Reveal world** to inspect the actual cave layout alongside the agent’s deductions. To generate another cave, select **Random map** and enter a grid size from **2 to 10** and a seed. The same size and seed reproduce the same layout.

## How the Agent Works

The hunter receives five percepts:

| Percept | Meaning |
| --- | --- |
| Stench | The live Wumpus is in an adjacent room. |
| Breeze | At least one adjacent room contains a pit. |
| Glitter | The gold is in the current room. |
| Bump | The last forward move hit a wall. |
| Scream | The arrow hit the Wumpus. |

**Logical inference.** The knowledge base combines observations from different rooms using propositional logic. For example, a breeze requires a pit in at least one adjacent room. A later observation can eliminate one of the candidates and confirm the remaining location. A room is safe when the agent can rule out both a pit and a live Wumpus there.

**BFS path planning.** The agent searches the graph of proven-safe rooms to find the nearest reachable unvisited room. After collecting the gold, it searches the same graph for a route back to the entrance. BFS minimizes the number of room-to-room moves, and turns count as separate actions. When safe exploration stalls, the agent can take a guaranteed shot if all remaining Wumpus candidates lie on one arrow ray. Otherwise, the automatic run stops at the unresolved frontier.

## Manual Controls

Click the map to focus it before using the keyboard.

| Key | Action |
| --- | --- |
| Up arrow | Move forward |
| Left or right arrow | Turn 90 degrees |
| Space | Shoot the single arrow |
| G | Pick up the gold in the current room |
| E | Exit from the entrance with the gold |

## Verification

The agent completed the lesson map in **22 actions**, visiting **6 distinct rooms**. The verification program also checked basic actions and matched the knowledge base against exhaustive hazard assignments in **90 comparisons** on 3 × 3 grids.

The automatic policy was evaluated on 500 random caves, using seeds 0 through 99 for each grid size.

| Grid | Escaped with gold | Stopped at frontier | Hunter lost |
| --- | ---: | ---: | ---: |
| 2 × 2 | 0 | 100 | 0 |
| 4 × 4 | 54 | 46 | 0 |
| 6 × 6 | 31 | 69 | 0 |
| 8 × 8 | 25 | 75 | 0 |
| 10 × 10 | 24 | 76 | 0 |
| **Total** | **134** | **366** | **0** |

Run the verification from the repository root in CMD:

```cmd
if not exist build\verify mkdir build\verify
javac --release 17 -encoding UTF-8 -d build\verify src\wampus\*.java test\wampus\*.java
java -cp build\verify wampus.Verification
```

## Project Structure

| Component | Responsibility |
| --- | --- |
| `src/wampus/Cave.java`, `Grid.java` | Cave generation, coordinates, neighbors, and arrow rays |
| `src/wampus/Game.java`, `Percept.java` | Game rules, actions, and sensor readings |
| `src/wampus/KnowledgeBase.java` | Observation history and hazard inference |
| `src/wampus/Agent.java` | Action selection and BFS path planning |
| `src/wampus/WampusApp.java`, `WorldPanel.java`, `CavePanel.java` | Swing window, controls, and cave visualization |
| `test/wampus/Verification.java` | Lesson, action, inference, and random-cave checks |

Developed by [Mohammad Azimi](https://github.com/mohammad-azimi).