# Multi-Threaded-Card-Game-Simulation

JDK 17
JUnit 5

To run tests:
Compile the game:
javac --release 17 -d out src/*.java

Run the game:
java -cp out CardGame

Compile the tests (Windows PowerShell):
javac -cp "lib/junit-platform-console-standalone-1.11.4.jar;out" -d out test/*.java

use a : instead of ; if on mac

Run the tests:
java -jar lib/junit-platform-console-standalone-1.11.4.jar execute --class-path out --scan-class-path
