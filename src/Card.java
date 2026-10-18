//a single playing card with a non negative int face value
//class is immutable so it is thread safe and can be shared between players
//and decks without needing locks
public class Card {
    private final int value;

    public Card(int value) {
        this.value = value;
    }
    public int getValue() {
        return value;

    }
    @Override
    public String toString() {
        return String.valueOf(value);
    }
}