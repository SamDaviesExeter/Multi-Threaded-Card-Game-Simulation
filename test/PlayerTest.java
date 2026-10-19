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

class PlayerTest {

    @Test
    void fourMatchingCardsIsAWinningHand() {
        assertTrue(Player.isWinningHand(hand(5, 5, 5, 5)));
    }

    @Test
    void mixedHandIsNotAWinningHand() {
        assertFalse(Player.isWinningHand(hand(5, 5, 5, 6)));
    }

    @Test
    void handOfWrongSizeIsNotAWinningHand() {
        assertFalse(Player.isWinningHand(hand(5, 5, 5)));
        assertFalse(Player.isWinningHand(hand(5, 5, 5, 5, 5)));
        assertFalse(Player.isWinningHand(hand()));
    }

    @Test
    void getIdReturnsConstructorId() {
        Player player = new Player(3, hand(1, 2, 3, 4), new CardDeck(3),
                new CardDeck(4), new GameState());
        assertEquals(3, player.getId());
    }

    @Test
    @Timeout(10)
    void winningInitialHandWinsWithoutTakingATurn(@TempDir Path dir) throws IOException {
        GameState state = new GameState();
        CardDeck drawDeck = deck(1, 9);
        CardDeck discardDeck = new CardDeck(2);

        new Player(1, hand(4, 4, 4, 4), drawDeck, discardDeck, state, dir).run();

        assertEquals(1, state.getWinner());
        //no cards should have moved
        assertEquals(1, drawDeck.size());
        assertEquals(0, discardDeck.size());
        assertEquals(List.of(
                "player 1 initial hand 4 4 4 4",
                "player 1 wins",
                "player 1 exits",
                "player 1 final hand: 4 4 4 4"
        ), readOutput(dir, 1));
    }

    @Test
    @Timeout(10)
    void playerDrawsDiscardsAndWins(@TempDir Path dir) throws IOException {
        GameState state = new GameState();
        CardDeck drawDeck = deck(1, 1);
        CardDeck discardDeck = new CardDeck(2);
        List<Card> hand = hand(1, 1, 1, 2);

        new Player(1, hand, drawDeck, discardDeck, state, dir).run();

        assertEquals(1, state.getWinner());
        assertTrue(Player.isWinningHand(hand));
        assertEquals(0, drawDeck.size());
        assertEquals("2", discardDeck.contents());
        assertEquals(List.of(
                "player 1 initial hand 1 1 1 2",
                "player 1 draws a 1 from deck 1",
                "player 1 discards a 2 to deck 2",
                "player 1 current hand is 1 1 1 1",
                "player 1 wins",
                "player 1 exits",
                "player 1 final hand: 1 1 1 1"
        ), readOutput(dir, 1));
    }

    @Test
    @Timeout(10)
    void playerNeverDiscardsPreferredCards(@TempDir Path dir) throws IOException {
        GameState state = new GameState();
        CardDeck drawDeck = deck(2, 2, 2, 2);
        CardDeck discardDeck = new CardDeck(3);
        List<Card> hand = hand(7, 2, 8, 9);

        new Player(2, hand, drawDeck, discardDeck, state, dir).run();

        assertEquals(2, state.getWinner());
        assertEquals("2 2 2 2", handString(hand));
        assertEquals("7 8 9", discardDeck.contents());
    }

    @Test
    @Timeout(10)
    void losingPlayerIsInformedOfWinnerAndExits(@TempDir Path dir) throws IOException {
        GameState state = new GameState();
        state.tryDeclareWinner(2);
        CardDeck drawDeck = deck(1, 3);
        CardDeck discardDeck = new CardDeck(2);

        new Player(1, hand(1, 2, 3, 4), drawDeck, discardDeck, state, dir).run();

        assertEquals(2, state.getWinner());
        assertEquals(1, drawDeck.size());
        assertEquals(List.of(
                "player 1 initial hand 1 2 3 4",
                "player 2 has informed player 1 that player 2 has won",
                "player 1 exits",
                "player 1 hand: 1 2 3 4"
        ), readOutput(dir, 1));
    }

    @Test
    @Timeout(10)
    void twoPlayerGameHasOneWinnerAndConservesCards(@TempDir Path dir)
            throws InterruptedException, IOException {
        GameState state = new GameState();
        CardDeck deck1 = deck(1, 3, 3, 4, 4);
        CardDeck deck2 = deck(2, 3, 4, 3, 4);
        List<Card> hand1 = hand(1, 1, 1, 2);
        List<Card> hand2 = hand(2, 2, 2, 1);

        Thread t1 = new Thread(new Player(1, hand1, deck1, deck2, state, dir));
        Thread t2 = new Thread(new Player(2, hand2, deck2, deck1, state, dir));
        t1.start();
        t2.start();
        t1.join();
        t2.join();

        int winner = state.getWinner();
        assertTrue(winner == 1 || winner == 2);
        assertTrue(Player.isWinningHand(winner == 1 ? hand1 : hand2));

        assertEquals(4, hand1.size());
        assertEquals(4, hand2.size());
        List<Integer> values = new ArrayList<>();
        for (Card c : hand1) values.add(c.getValue());
        for (Card c : hand2) values.add(c.getValue());
        drainInto(deck1, values);
        drainInto(deck2, values);
        Collections.sort(values);
        assertEquals(List.of(1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 4), values);

        int loser = winner == 1 ? 2 : 1;
        List<String> winnerLines = readOutput(dir, winner);
        List<String> loserLines = readOutput(dir, loser);
        assertTrue(winnerLines.contains("player " + winner + " wins"));
        assertTrue(loserLines.contains("player " + winner + " has informed player "
                + loser + " that player " + winner + " has won"));
        assertEquals("player " + winner + " exits",
                winnerLines.get(winnerLines.size() - 2));
        assertEquals("player " + loser + " exits",
                loserLines.get(loserLines.size() - 2));
    }

    private static List<Card> hand(int... values) {
        List<Card> cards = new ArrayList<>();
        for (int v : values) {
            cards.add(new Card(v));
        }
        return cards;
    }

    private static CardDeck deck(int id, int... values) {
        CardDeck deck = new CardDeck(id);
        for (int v : values) {
            deck.addToBottom(new Card(v));
        }
        return deck;
    }

    private static String handString(List<Card> hand) {
        List<String> parts = new ArrayList<>();
        for (Card c : hand) parts.add(c.toString());
        return String.join(" ", parts);
    }

    private static List<String> readOutput(Path dir, int playerId) throws IOException {
        return Files.readAllLines(dir.resolve("player" + playerId + "_output.txt"));
    }

    private static void drainInto(CardDeck deck, List<Integer> out) {
        Card c;
        while ((c = deck.drawFromTop()) != null) {
            out.add(c.getValue());
        }
    }
}
