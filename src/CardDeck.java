import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.StringJoiner;
import java.util.concurrent.locks.ReentrantLock;
//a thread safe fifo deck
//players draw from top and discard to bottom (front and back)
//deck N sits inbetween player N and N-1
public class CardDeck {
    private final int id;
    //using a deque as it has a O(1) for adding and removing from front/back
    private final Deque<Card> cards = new ArrayDeque<>();
    //reentrant lock instead of synchronized because Player needs to hold
    //a decks lock across multiple calls and from outside the class which synchronized cant do
    private final ReentrantLock lock = new ReentrantLock();

    public CardDeck(int id) {
        this.id = id;
    }
    public int getId() {
        return id;

    }
    //exposed so that Player can hold both decks at once which makes draw and discard a single atomic action

    public void lock() {lock.lock();}
    public void unlock() {lock.unlock();}
    //adds card to back of pile and locks internally so its safe when called without the outer lock like when dealing
    public void addToBottom(Card card) {
        lock.lock();
        try {
            cards.addLast(card);
        } finally {
            lock.unlock();
        }
    }
    //removes and returns a card from the front
    //checks if the deck is empty
    public Card drawFromTop() {
        lock.lock();
        try {
            return cards.pollFirst();
        } finally {
            lock.unlock();
        }
    }

    public int size() {
        lock.lock();
        try {
            return cards.size();
        } finally {
            lock.unlock();
        }
    }
    //returns the card as a space seperated format from top to bot
    public String contents() {
        lock.lock();
        try {
            StringJoiner sj = new StringJoiner(" ");
            for (Card c : cards) sj.add(c.toString());
            return sj.toString();
        } finally {
            lock.unlock();
        }
    }
    //called once after all player threads have finished so no other threads change the deck
    //writes a single line in the format of deckN and contents
    public void writeFinalContents(Path dir) throws IOException {
        Path file = dir.resolve("deck"+ id + "_output.txt");
        Files.writeString(file, "deck"+ id + " contents: " + contents()
                + System.lineSeparator());
    }
}