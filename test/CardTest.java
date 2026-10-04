import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CardTest {

    @Test
    void getValueReturnsConstructorValue() {
        assertEquals(7, new Card(7).getValue());
    }

    @Test
    void zeroIsAValidValue() {
        assertEquals(0, new Card(0).getValue());
    }

    @Test
    void toStringIsTheValueAsText() {
        assertEquals("12", new Card(12).toString());
    }
}