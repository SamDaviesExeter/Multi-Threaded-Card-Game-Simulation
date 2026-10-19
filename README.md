# Multi-Threaded-Card-Game-Simulation

## Requirements
- JDK 17 (tested with Eclipse Temurin 17.0.18)
- JUnit 5 via the JUnit Platform Console Standalone 1.11.4 jar
  (JUnit Jupiter 5.11.4 + JUnit Platform Suite 1.11.4).
  The jar is included at `lib/junit-platform-console-standalone-1.11.4.jar`,
  so no other downloads are needed.

## Contents
- `src/`  game source files (Card, CardDeck, CardGame, GameState, Player)
- `test/` test classes (CardTest, CardDeckTest, CardGameTest, GameStateTest, PlayerTest)
  and the suite CardsTestSuite, which runs them all
- `bin/`  the compiled .class files of `src/` and `test/`
- `lib/`  the JUnit standalone jar

All commands below are run from the folder that contains this README.

## Running the pre-compiled tests
```
java -jar lib/junit-platform-console-standalone-1.11.4.jar execute --class-path bin --select-class CardsTestSuite
```

## Compiling from source
```
javac --release 17 -d bin -cp lib/junit-platform-console-standalone-1.11.4.jar src/*.java test/*.java
```
Then run the suite with the command above.

## Running the game
```
java -jar cards.jar
```

