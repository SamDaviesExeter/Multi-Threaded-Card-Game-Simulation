import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

//runs every test class in one go
@Suite
@SelectClasses({
        CardTest.class,
        CardDeckTest.class,
        GameStateTest.class,
        PlayerTest.class,
        CardGameTest.class
})
class CardsTestSuite {
}
