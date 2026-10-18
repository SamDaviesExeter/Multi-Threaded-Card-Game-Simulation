import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;


//each player runs of its own thread
//then it draws from left and discards from right deck
//finally logs to playerN output text file
public class Player implements Runnable {
    private final int id;
    private final List<Card> hand;
    //drawDeck is the left deck
    private final CardDeck drawDeck;
    //discardDeck is the right deck
    private final CardDeck discardDeck;
    private final GameState gameState;
    //defaults the output dir to current folder
    private final Path outputDirectory;

    public Player(int id, List<Card> hand, CardDeck drawDeck,
                  CardDeck discardDeck, GameState gameState) {
        this(id, hand, drawDeck, discardDeck, gameState, Path.of("."));
    }

    public Player(int id, List<Card> hand, CardDeck drawDeck,
                  CardDeck discardDeck, GameState gameState,
                  Path outputDirectory) {
        this.id = id;
        this.hand = hand;
        this.drawDeck = drawDeck;
        this.discardDeck = discardDeck;
        this.gameState = gameState;
        this.outputDirectory = outputDirectory;
    }

    public int getId() {
        return id;
    }

    @Override
    public void run() {
        //creates the player output file
        Path file = outputDirectory.resolve("player" + id + "_output.txt");

        try (BufferedWriter out = Files.newBufferedWriter(file)) {
            write(out, "player " + id + " initial hand " + handString());

            //if you start with a winning hand then end the game before any turns
            if (isWinningHand(hand)) {
                gameState.tryDeclareWinner(id);
            }
            if (gameState.getWinner() == id) {
                announceWin(out);
            }
            //keep playing while no player has declared a win
            while (!gameState.hasWinner()) {
                //if the draw deck is empty then just wait and another player should refill it
                if (!takeTurn(out)) {
                    Thread.yield();
                    continue;
                }
                //tryDeclareWinner is atomic so one player announces a win even if multiple finish together
                if (isWinningHand(hand) && gameState.tryDeclareWinner(id)) {
                    announceWin(out);
                }
            }
            //players who didnt win are informed of the winner
            int winner = gameState.getWinner();
            if (winner != id) {
                write(out, "player " + winner + " has informed player " + id
                        + " that player " + winner + " has won");
            }

            write(out, "player " + id + " exits");
            String label = winner == id ? " final hand: " : " hand: ";
            write(out, "player " + id + label + handString());

        } catch (IOException e) {
            System.err.println("Player " + id + " could not write output: "
                    + e.getMessage());
        }
    }

    //draws and discards in one atomic step
    //return false if the draw deck is empty
    private boolean takeTurn(BufferedWriter out) throws IOException {
        //locks in id order stops two adjacent players deadlocking each other
        CardDeck first = drawDeck.getId() <= discardDeck.getId()
                ? drawDeck : discardDeck;
        CardDeck second = first == drawDeck ? discardDeck : drawDeck;

        first.lock();
        try {
            second.lock();
            try {
                Card drawn = drawDeck.drawFromTop();
                if (drawn == null) {
                    return false;
                }
                hand.add(drawn);

                Card discarded = chooseDiscard();
                hand.remove(discarded);
                discardDeck.addToBottom(discarded);
                //logs while the locks are held so the files order matches the actual actions
                write(out, "player " + id + " draws a " + drawn
                        + " from deck " + drawDeck.getId());
                write(out, "player " + id + " discards a " + discarded
                        + " to deck " + discardDeck.getId());
                write(out, "player " + id + " current hand is "
                        + handString());
                return true;
            } finally {
                second.unlock();
            }
        } finally {
            first.unlock();
        }
    }

    //discards a non preferred card
    //takes a winning discard it exists
    private Card chooseDiscard() {
        for (Card card : hand) {
            if (card.getValue() != id && winsWithout(card)) {
                return card;
            }
        }
        for (Card card : hand) {
            if (card.getValue() != id) {
                return card;
            }
        }
        //incase for some reason it breaks and a non preferred card does not exist
        return hand.get(0);
    }
    //checks if removing this card would leave you with a winning hand
    //without actually changing the real hand
    private boolean winsWithout(Card removed) {
        List<Card> rest = new ArrayList<>(hand);
        rest.remove(removed);
        return isWinningHand(rest);
    }
    //checks for 4 of the same cards
    //static so that cardGame can use for starting checks
    public static boolean isWinningHand(List<Card> hand) {
        if (hand.size() != 4) {
            return false;
        }
        int value = hand.get(0).getValue();
        for (Card card : hand) {
            if (card.getValue() != value) {
                return false;
            }
        }
        return true;
    }
    //prints to the terminal and records the win in the players own file
    private void announceWin(BufferedWriter out) throws IOException {
        System.out.println("player " + id + " wins");
        write(out, "player " + id + " wins");
    }

    private String handString() {
        StringJoiner sj = new StringJoiner(" ");
        for (Card card : hand) {
            sj.add(card.toString());
        }
        return sj.toString();
    }

    private void write(BufferedWriter out, String line) throws IOException {
        out.write(line);
        out.newLine();
    }
}
