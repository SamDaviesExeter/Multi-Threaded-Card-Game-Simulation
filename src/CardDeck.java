import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.StringJoiner;
import java.util.concurrent.locks.ReentrantLock;

public class CardDeck {
    private final int id;
    private final Deque<Card> cards = new ArrayDeque<>();
    private final ReentrantLock lock = new ReentrantLock();

    public CardDeck(int id) {
        this.id = id;
    }
    public int getId() {
        return id;

    }

    public void lock() {lock.lock();}
    public void unlock() {lock.unlock();}

    public void addToBottom(Card card) {
        lock.lock();
        try {
            cards.addLast(card);
        } finally {
            lock.unlock();
        }
    }

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

    public void writeFinalContents(Path dir) throws IOException {
        Path file = dir.resolve("deck"+ id + "_output.txt");
        Files.writeString(file, "deck"+ id + " contents: " + contents()
                + System.lineSeparator());
    }
}