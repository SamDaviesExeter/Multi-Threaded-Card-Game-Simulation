
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CardGameTest {

    // Temporary directory for test pack files
    @TempDir
    Path tempDir;

    // Checks that a valid pack is loaded correctly
    @Test
    void testValidPack() throws IOException {
        Path file = tempDir.resolve("valid.txt");

        List<String> values = new ArrayList<>();
        for (int i = 1; i <= 16; i++) {
            values.add(String.valueOf(i));
        }

        Files.write(file, values);

        List<Card> pack = CardGame.loadPack(file.toString(), 2);

        assertEquals(16, pack.size());
        assertEquals(1, pack.get(0).getValue());
        assertEquals(16, pack.get(15).getValue());
    }

    // Rejects packs with the wrong number of cards
    @Test
    void testIncorrectPackSize() throws IOException {
        Path file = tempDir.resolve("short.txt");
        Files.writeString(file, "1\n2\n3\n");

        assertThrows(IllegalArgumentException.class,
                () -> CardGame.loadPack(file.toString(), 2));
    }

    // Rejects negative card values
    @Test
    void testNegativeCardValue() throws IOException {
        Path file = tempDir.resolve("negative.txt");

        List<String> values = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            values.add(i == 5 ? "-1" : "2");
        }

        Files.write(file, values);

        assertThrows(IllegalArgumentException.class,
                () -> CardGame.loadPack(file.toString(), 2));
    }

    // Rejects card values that are not integers
    @Test
    void testNonIntegerCardValue() throws IOException {
        Path file = tempDir.resolve("invalid.txt");

        List<String> values = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            values.add(i == 5 ? "abc" : "2");
        }

        Files.write(file, values);

        assertThrows(IllegalArgumentException.class,
                () -> CardGame.loadPack(file.toString(), 2));
    }

    // Checks that the number of players must be positive
    @Test
    void testInvalidPlayerCount() {
        assertThrows(IllegalArgumentException.class,
                () -> CardGame.loadPack("pack.txt", 0));
    }

    // Checks that cards are dealt in the correct round-robin order
    @Test
    void testDealing() {
        List<Card> pack = new ArrayList<>();

        for (int i = 1; i <= 16; i++) {
            pack.add(new Card(i));
        }

        List<List<Card>> hands = new ArrayList<>();
        List<CardDeck> decks = new ArrayList<>();

        for (int i = 0; i < 2; i++) {
            hands.add(new ArrayList<>());
            decks.add(new CardDeck(i + 1));
        }

        CardGame.dealCards(pack, hands, decks);

        // Checks the number and order of cards dealt
        assertEquals(4, hands.get(0).size());
        assertEquals(4, hands.get(1).size());
        assertEquals(4, decks.get(0).size());
        assertEquals(4, decks.get(1).size());

        assertEquals(1, hands.get(0).get(0).getValue());
        assertEquals(3, hands.get(0).get(1).getValue());
        assertEquals(2, hands.get(1).get(0).getValue());

        assertEquals("9 11 13 15", decks.get(0).contents());
        assertEquals("10 12 14 16", decks.get(1).contents());
    }

    // Ensures cards cannot be dealt into a non-empty hand
    @Test
    void testDealingIntoNonEmptyHand() {
        List<Card> pack = new ArrayList<>();

        for (int i = 1; i <= 16; i++) {
            pack.add(new Card(i));
        }

        List<List<Card>> hands = new ArrayList<>();
        hands.add(new ArrayList<>());
        hands.add(new ArrayList<>());
        hands.get(0).add(new Card(99));

        List<CardDeck> decks = List.of(new CardDeck(1), new CardDeck(2));

        assertThrows(IllegalArgumentException.class,
                () -> CardGame.dealCards(pack, hands, decks));
    }

    // Checks that the program asks again after an invalid pack filename
    @Test
    void testInvalidPackRetry() throws IOException {
        Path file = tempDir.resolve("valid.txt");

        List<String> values = new ArrayList<>();

        // Gives player 1 an immediate winning hand
        int[] cardValues = {
            1, 2, 1, 3,
            1, 2, 1, 3,
            4, 5, 4, 5,
            4, 5, 4, 5
        };

        for (int value : cardValues) {
            values.add(String.valueOf(value));
        }

        Files.write(file, values);

        String input = "2\nmissing.txt\n" + file + "\n";

        InputStream originalIn = System.in;
        PrintStream originalOut = System.out;

        // Simulates user input and captures console output
        ByteArrayInputStream testInput = new ByteArrayInputStream(
                input.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream testOutput = new ByteArrayOutputStream();

        try {
            System.setIn(testInput);
            System.setOut(new PrintStream(testOutput));

            CardGame.main(new String[0]);

            String output = testOutput.toString(StandardCharsets.UTF_8);

            assertTrue(output.contains("Invalid pack:"));
            assertTrue(output.contains("Please try another file."));
            assertTrue(output.contains("Pack loaded: 16 cards"));

        } finally {
            // Restores the original console input and output
            System.setIn(originalIn);
            System.setOut(originalOut);
        }
    }
}
