import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CardDeckTest {

    @Test
    void drawsInFifoOrder() {
        CardDeck deck = new CardDeck(1);
        deck.addToBottom(new Card(3));
        deck.addToBottom(new Card(7));
        deck.addToBottom(new Card(5));

        assertEquals(3, deck.drawFromTop().getValue());
        assertEquals(7, deck.drawFromTop().getValue());
        assertEquals(5, deck.drawFromTop().getValue());
    }

    @Test
    void drawFromEmptyDeckReturnsNull() {
        CardDeck deck = new CardDeck(1);
        assertNull(deck.drawFromTop());
    }

    @Test
    void sizeTracksAddsAndDraws() {
        CardDeck deck = new CardDeck(1);
        assertEquals(0, deck.size());

        deck.addToBottom(new Card(1));
        deck.addToBottom(new Card(2));
        assertEquals(2, deck.size());

        deck.drawFromTop();
        assertEquals(1, deck.size());
    }

    @Test
    void contentsAreSpaceSeparatedTopToBottom() {
        CardDeck deck = new CardDeck(2);
        for (int v : new int[]{1, 2, 3, 4}) {
            deck.addToBottom(new Card(v));
        }
        assertEquals("1 2 3 4", deck.contents());
    }

    @Test
    void contentsOfEmptyDeckIsEmptyString() {
        assertEquals("", new CardDeck(1).contents());
    }

    @Test
    void writesFinalContentsInSpecFormat(@TempDir Path dir) throws IOException {
        CardDeck deck = new CardDeck(2);
        for (int v : new int[]{1, 2, 3, 4}) {
            deck.addToBottom(new Card(v));
        }

        deck.writeFinalContents(dir);

        Path file = dir.resolve("deck2_output.txt");
        assertTrue(Files.exists(file));
        assertEquals("deck2 contents: 1 2 3 4", Files.readString(file).trim());
    }

    @Test
    @Timeout(10)
    void concurrentTransfersConserveCards() throws InterruptedException {
        CardDeck a = new CardDeck(1);
        CardDeck b = new CardDeck(2);
        int total = 100;
        for (int i = 0; i < total; i++) {
            a.addToBottom(new Card(i));
        }

        List<Thread> threads = new ArrayList<>();
        for (int t = 0; t < 8; t++) {
            final boolean aToB = (t % 2 == 0);
            Thread thread = new Thread(() -> {
                for (int i = 0; i < 2000; i++) {
                    if (aToB) move(a, b); else move(b, a);
                }
            });
            threads.add(thread);
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }

        assertEquals(total, a.size() + b.size());

        List<Integer> values = new ArrayList<>();
        drainInto(a, values);
        drainInto(b, values);
        Collections.sort(values);
        for (int i = 0; i < total; i++) {
            assertEquals(i, values.get(i));
        }
    }
    private static void move(CardDeck from, CardDeck to) {
        CardDeck first = from.getId() < to.getId() ? from : to;
        CardDeck second = (first == from) ? to : from;

        first.lock();
        try {
            second.lock();
            try {
                Card c = from.drawFromTop();
                if (c != null) {
                    to.addToBottom(c);
                }
            } finally {
                second.unlock();
            }
        } finally {
            first.unlock();
        }
    }

    private static void drainInto(CardDeck deck, List<Integer> out) {
        Card c;
        while ((c = deck.drawFromTop()) != null) {
            out.add(c.getValue());
        }
    }
}

   