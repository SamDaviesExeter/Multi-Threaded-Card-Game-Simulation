import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class GameStateTest {

    @Test
    void newGameHasNoWinner() {
        GameState state = new GameState();
        assertFalse(state.hasWinner());
        assertEquals(0, state.getWinner());
    }

    @Test
    void firstDeclarationSucceeds() {
        GameState state = new GameState();
        assertTrue(state.tryDeclareWinner(3));
        assertTrue(state.hasWinner());
        assertEquals(3, state.getWinner());
    }

    @Test
    void laterDeclarationsFailAndKeepFirstWinner() {
        GameState state = new GameState();
        state.tryDeclareWinner(2);

        assertFalse(state.tryDeclareWinner(4));
        assertFalse(state.tryDeclareWinner(2));
        assertEquals(2, state.getWinner());
    }

    @Test
    void zeroPlayerIdIsRejected() {
        GameState state = new GameState();
        assertThrows(IllegalArgumentException.class, () -> state.tryDeclareWinner(0));
        assertFalse(state.hasWinner());
    }

    @Test
    void negativePlayerIdIsRejected() {
        GameState state = new GameState();
        assertThrows(IllegalArgumentException.class, () -> state.tryDeclareWinner(-1));
        assertFalse(state.hasWinner());
    }

    @Test
    @Timeout(10)
    void onlyOneOfManyConcurrentDeclarationsSucceeds() throws InterruptedException {
        GameState state = new GameState();
        int playerCount = 50;
        AtomicInteger successes = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);
        List<Thread> threads = new ArrayList<>();
        for (int i = 1; i <= playerCount; i++) {
            final int id = i;
            Thread thread = new Thread(() -> {
                try {
                    start.await();
                } catch (InterruptedException e) {
                    return;
                }
                if (state.tryDeclareWinner(id)) {
                    successes.incrementAndGet();
                }
            });
            threads.add(thread);
            thread.start();
        }
        start.countDown();
        for (Thread thread : threads) {
            thread.join();
        }

        assertEquals(1, successes.get());
        assertTrue(state.getWinner() >= 1 && state.getWinner() <= playerCount);
    }
}
