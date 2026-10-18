import java.util.concurrent.atomic.AtomicInteger;

//a shared game state between all player threads to record who wins
public class GameState {
    //use 0 as place holder for nobody wins value
    private static final int NO_WINNER = 0;

    //AtomicInterger lets multiple threads read and set the winner without
    //locks or synchronized
    private final AtomicInteger winner = new AtomicInteger(NO_WINNER);

    //returns true only for the first player say they won
    public boolean tryDeclareWinner(int playerId) {
        //check the player id makes sense and avoids no winner
        if (playerId <= 0) {
            throw new IllegalArgumentException("Player id must be positive");
        }
        //sets winner to player id only if it is still no winner
        //does this in one step as multiple could let two players both pass the check
        return winner.compareAndSet(NO_WINNER, playerId);
    }
    //players check this at start so they know when to stop
    public boolean hasWinner() {
        return winner.get() != NO_WINNER;
    }

    //returns the player id of the winner or 0 if no winner
    public int getWinner() {
        return winner.get();
    }
}
