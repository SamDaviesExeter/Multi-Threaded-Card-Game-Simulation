import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class CardGame {
    // Loads the pack and checks that all card values are valid
    public static List<Card> loadPack(String filename, int playerCount)
            throws IOException {

        if (playerCount <= 0) {
            throw new IllegalArgumentException("Player count must be positive");
        }

        List<String> lines = Files.readAllLines(Path.of(filename));

        if (lines.size() != playerCount * 8) {
            throw new IllegalArgumentException(
                "Pack must contain exactly " + (playerCount * 8) + " cards"
            );
        }

        List<Card> cards = new ArrayList<>();

        for (String line : lines) {
            int value;

            try {
                value = Integer.parseInt(line.trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                    "Invalid card value: " + line, e
                );
            }

            if (value < 0) {
                throw new IllegalArgumentException(
                    "Card values cannot be negative"
                );
            }

            cards.add(new Card(value));
        }

        return cards;
    }

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {

            int playerCount;

            // Keeps asking until a valid player count is entered
            while (true) {
                System.out.print("Enter the number of players: ");

                try {
                    playerCount = Integer.parseInt(scanner.nextLine().trim());

                    if (playerCount > 0) {
                        break;
                    }

                    System.out.println("Player count must be positive.");

                } catch (NumberFormatException e) {
                    System.out.println("Please enter a valid integer.");
                }
            }

            List<Card> pack;

            // Keeps asking until a valid pack file is provided
            while (true) {
                System.out.print("Enter the pack filename: ");
                String filename = scanner.nextLine().trim();

                try {
                    pack = loadPack(filename, playerCount);
                    break;

                } catch (IOException | IllegalArgumentException e) {
                    System.out.println("Invalid pack: " + e.getMessage());
                    System.out.println("Please try another file.");
                }
            }

            System.out.println("Pack loaded: " + pack.size() + " cards");

            // Creates an empty hand and deck for each player
            List<List<Card>> hands = new ArrayList<>();
            List<CardDeck> decks = new ArrayList<>();

            for (int i = 0; i < playerCount; i++) {
                hands.add(new ArrayList<>());
                decks.add(new CardDeck(i + 1));
            }

            dealCards(pack, hands, decks);

            // Starts the game once the cards have been dealt
            try {
                runGame(hands, decks, Path.of("."));
            } catch (IOException e) {
                System.err.println("Error writing output files: "
                        + e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Game interrupted.");
            }
        }
    }

    // Deals cards to players first, then fills the decks
    public static void dealCards(
            List<Card> pack,
            List<List<Card>> hands,
            List<CardDeck> decks) {

        int playerCount = hands.size();

        if (playerCount == 0 || decks.size() != playerCount
                || pack.size() != playerCount * 8) {
            throw new IllegalArgumentException("Invalid dealing setup");
        }

        // Checks that all hands and decks are empty before dealing
        for (List<Card> hand : hands) {
            if (!hand.isEmpty()) {
                throw new IllegalArgumentException("Hands must be empty");
            }
        }

        for (CardDeck deck : decks) {
            if (deck.size() != 0) {
                throw new IllegalArgumentException("Decks must be empty");
            }
        }

        // Distributes four cards to each player in round-robin order
        for (int i = 0; i < playerCount * 4; i++) {
            hands.get(i % playerCount).add(pack.get(i));
        }

        // Distributes the remaining cards between the decks
        for (int i = 0; i < playerCount * 4; i++) {
            decks.get(i % playerCount)
                    .addToBottom(pack.get(playerCount * 4 + i));
        }
    }

    // Creates the player threads and manages the game
    public static void runGame(
            List<List<Card>> hands,
            List<CardDeck> decks,
            Path outputDirectory)
            throws IOException, InterruptedException {

        int playerCount = hands.size();

        if (playerCount == 0 || decks.size() != playerCount) {
            throw new IllegalArgumentException("Invalid game setup");
        }

        Files.createDirectories(outputDirectory);

        // For Sam (note), need to verify the integration and test with partner's Player and GameState classes, still 'placeholder'
        GameState gameState = new GameState();
        List<Thread> threads = new ArrayList<>();

        // Sets up each player with their hand and neighbouring decks
        for (int i = 0; i < playerCount; i++) {
            int playerId = i + 1;

            CardDeck leftDeck = decks.get(i);
            CardDeck rightDeck = decks.get((i + 1) % playerCount);

            Player player = new Player(
                    playerId,
                    hands.get(i),
                    leftDeck,
                    rightDeck,
                    gameState
            );

            Thread thread = new Thread(player, "Player-" + playerId);
            threads.add(thread);
        }

        // Starts all player threads
        for (Thread thread : threads) {
            thread.start();
        }

        // Waits for all players to finish before saving the decks
        for (Thread thread : threads) {
            thread.join();
        }

        // Writes the final contents of each deck to its output file
        for (CardDeck deck : decks) {
            deck.writeFinalContents(outputDirectory);
        }
    }
}

