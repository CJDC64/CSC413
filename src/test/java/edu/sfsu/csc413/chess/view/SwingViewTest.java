package edu.sfsu.csc413.chess.view;

import edu.sfsu.csc413.chess.engine.Game;
import edu.sfsu.csc413.chess.engine.GameStatus;
import edu.sfsu.csc413.chess.factory.BoardFactory;
import edu.sfsu.csc413.chess.model.Color;
import edu.sfsu.csc413.chess.model.PieceType;
import edu.sfsu.csc413.chess.model.Position;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Headless component tests: these do not open or visually inspect a window. */
class SwingViewTest {
    private JPanel create(SwingView view, Game game) {
        return view.createContent(new GameController(game, view));
    }

    private static List<Component> descendants(Container parent) {
        List<Component> found = new ArrayList<>();
        for (Component child : parent.getComponents()) {
            found.add(child);
            if (child instanceof Container container) found.addAll(descendants(container));
        }
        return found;
    }

    private static JButton button(JPanel panel, String name) {
        return descendants(panel).stream().filter(JButton.class::isInstance)
                .map(JButton.class::cast)
                .filter(b -> name.equals(b.getName()) || name.equals(b.getText()))
                .findFirst().orElseThrow();
    }

    private static String messages(JPanel panel) {
        return descendants(panel).stream().filter(JTextArea.class::isInstance)
                .map(JTextArea.class::cast).findFirst().orElseThrow().getText();
    }

    private static void move(JPanel panel, String notation) {
        button(panel, notation.substring(0, 2)).doClick(0);
        button(panel, notation.substring(2, 4)).doClick(0);
    }

    @Test
    void retainsEarlyCallsFromOutsideEdt() throws Exception {
        SwingView view = new SwingView();
        Game game = new Game();
        view.showMessage("Welcome to Chess!");
        view.render(game);
        SwingUtilities.invokeAndWait(() -> {
            JPanel panel = create(view, game);
            assertTrue(messages(panel).contains("Welcome to Chess!"));
            assertEquals("e2 WHITE PAWN", button(panel, "e2").getToolTipText());
            assertEquals(64, descendants(panel).stream().filter(JButton.class::isInstance)
                    .filter(c -> c.getName() != null && c.getName().matches("[a-h][1-8]")).count());
        });
    }

    @Test
    void illegalMessageSurvivesRenderAndUndoRestoresPosition() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            SwingView view = new SwingView();
            Game game = new Game();
            JPanel panel = create(view, game);
            move(panel, "e2e5");
            assertTrue(game.history().isEmpty());
            assertTrue(messages(panel).contains("Not a legal move: 'e2e5'."));
            view.render(game);
            assertTrue(messages(panel).contains("Not a legal move"));
            move(panel, "e2e4");
            assertEquals(Color.BLACK, game.sideToMove());
            assertEquals(1, game.history().size());
            button(panel, "Undo").doClick(0);
            assertEquals(Color.WHITE, game.sideToMove());
            assertTrue(game.history().isEmpty());
            assertEquals(BoardFactory.START_FEN.split(" ")[0], game.board().toString());
        });
    }

    @Test
    void foolsMateKeepsResultAndDisablesPlay() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            SwingView view = new SwingView();
            Game game = new Game();
            JPanel panel = create(view, game);
            for (String notation : List.of("f2f3", "e7e5", "g2g4", "d8h4")) move(panel, notation);
            assertEquals(GameStatus.CHECKMATE, game.status());
            assertTrue(messages(panel).contains("Checkmate. BLACK wins."));
            assertFalse(button(panel, "Undo").isEnabled());
            assertFalse(button(panel, "h4").isEnabled());
            assertNotNull(button(panel, "h4").getDisabledIcon());
            assertTrue(button(panel, "Close").isEnabled());
            move(panel, "a2a3");
            assertEquals(4, game.history().size());
        });
    }

    @Test
    void clickingPromotionDefaultsToQueen() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Game game = new Game(BoardFactory.fromFen("7k/P7/8/8/8/8/8/7K"), Color.WHITE);
            JPanel panel = create(new SwingView(), game);
            move(panel, "a7a8");
            assertEquals(PieceType.QUEEN, game.board().pieceAt(Position.parse("a8")).type());
            assertEquals("a7a8q", game.history().getFirst().toString());
            button(panel, "Undo").doClick(0);
            assertEquals(PieceType.PAWN, game.board().pieceAt(Position.parse("a7")).type());
        });
    }

    @Test
    void stalemateKeepsDrawMessage() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Game game = new Game(BoardFactory.fromFen("k7/8/1QK5/8/8/8/8/8"), Color.WHITE);
            JPanel panel = create(new SwingView(), game);
            move(panel, "b6c7");
            assertEquals(GameStatus.STALEMATE, game.status());
            assertTrue(messages(panel).contains("Stalemate. The game is a draw."));
            assertFalse(button(panel, "Undo").isEnabled());
            assertTrue(button(panel, "Close").isEnabled());
        });
    }
}
