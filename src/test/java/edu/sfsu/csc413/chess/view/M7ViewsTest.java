package edu.sfsu.csc413.chess.view;

import edu.sfsu.csc413.chess.engine.Game;
import edu.sfsu.csc413.chess.engine.GameStatus;
import edu.sfsu.csc413.chess.factory.BoardFactory;
import edu.sfsu.csc413.chess.model.Color;
import edu.sfsu.csc413.chess.model.PieceType;
import edu.sfsu.csc413.chess.model.Position;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

/** Exercises the real views/controller; Swing components run without a window. */
class M7ViewsTest {
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();

    private RotatingTerminalView terminal(String input) {
        return new RotatingTerminalView(new PrintStream(output), new Scanner(input));
    }

    private String text() {
        return output.toString(StandardCharsets.UTF_8);
    }

    @Test
    void rotationFollowsTurnAndUndoWithBothCoordinatesAndPieces() {
        Game game = new Game();
        RotatingTerminalView view = terminal("");
        GameController controller = new GameController(game, view);
        view.render(game);
        assertTrue(text().contains("a    b    c    d    e    f    g    h"));
        assertTrue(text().contains("8 | bR | bN | bB | bQ | bK | bB | bN | bR | 8"));
        output.reset();
        controller.handle(new PlayerAction.Move("e2e4"));
        assertTrue(text().contains("h    g    f    e    d    c    b    a"));
        assertTrue(text().contains("1 | wR | wN | wB | wK | wQ | wB | wN | wR | 1"));
        output.reset();
        controller.handle(new PlayerAction.Undo());
        assertTrue(text().contains("View from WHITE's side"));
        assertTrue(text().contains("a    b    c    d    e    f    g    h"));
        assertTrue(game.history().isEmpty());
    }

    @Test
    void terminalHandlesErrorsCommandsAndFoolsMate() {
        Game game = new Game();
        RotatingTerminalView view = terminal(
                "undo\ne2e5\nmoves\ne2e4\nundo\nf2f3\ne7e5\ng2g4\nd8h4\ne2e4\n");
        new GameController(game, view).start();
        assertEquals(GameStatus.CHECKMATE, game.status());
        assertEquals(4, game.history().size());
        assertTrue(text().contains("Nothing to undo."));
        assertTrue(text().contains("Not a legal move: 'e2e5'."));
        assertTrue(text().contains("20 legal moves:"));
        assertTrue(text().endsWith("Checkmate. BLACK wins." + System.lineSeparator()));
    }

    @Test
    void terminalQuitAndEndOfInputStopWithoutChangingTheBoard() {
        for (String input : new String[]{"quit\ne2e4\n", ""}) {
            output.reset();
            Game game = new Game();
            new GameController(game, terminal(input)).start();
            assertTrue(game.history().isEmpty());
            assertTrue(text().contains("Goodbye."));
        }
    }

    @Test
    void terminalSupportsAllFourPromotionsForBothColors() {
        for (Color color : Color.values()) {
            for (PieceType type : new PieceType[]{PieceType.QUEEN, PieceType.ROOK,
                    PieceType.BISHOP, PieceType.KNIGHT}) {
                boolean white = color == Color.WHITE;
                Game game = new Game(BoardFactory.fromFen(white
                        ? "k7/4P3/8/8/8/8/8/4K3"
                        : "4k3/8/8/8/8/8/4p3/K7"), color);
                String move = (white ? "e7e8" : "e2e1")
                        + Character.toLowerCase(type.symbol());
                new GameController(game, terminal(move + "\nquit\n")).start();
                assertEquals(type, game.board().pieceAt(
                        Position.parse(white ? "e8" : "e1")).type());
            }
        }
    }

    @Test
    void terminalAnnouncesStalemate() {
        Game game = new Game(BoardFactory.fromFen("k7/8/1QK5/8/8/8/8/8"), Color.WHITE);
        new GameController(game, terminal("b6c7\n")).start();
        assertEquals(GameStatus.STALEMATE, game.status());
        assertTrue(text().contains("Stalemate. The game is a draw."));
    }

    // Reflection keeps headless test hooks out of the production view API.
    private static Object field(SwingView view, String name) throws Exception {
        Field field = SwingView.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(view);
    }

    private static void prepareComponents(SwingView view, Game game) throws Exception {
        Field controller = SwingView.class.getDeclaredField("controller");
        controller.setAccessible(true);
        controller.set(view, new GameController(game, view));
        Method build = SwingView.class.getDeclaredMethod("buildContent");
        build.setAccessible(true);
        build.invoke(view);
        view.render(game);
    }

    private static void click(SwingView view, String square) throws Exception {
        Position position = Position.parse(square);
        JButton[][] buttons = (JButton[][]) field(view, "squares");
        buttons[position.file()][position.rank()].doClick(0);
    }

    private static void move(SwingView view, String notation) throws Exception {
        click(view, notation.substring(0, 2));
        click(view, notation.substring(2, 4));
    }

    @Test
    void swingRetainsEarlyMessagesAndRoutesClicksErrorsAndUndo() throws Exception {
        SwingView view = new SwingView();
        Game game = new Game();
        view.showMessage("Welcome to Chess!");
        view.render(game);
        SwingUtilities.invokeAndWait(() -> {
            try {
                prepareComponents(view, game);
                JTextArea messages = (JTextArea) field(view, "messages");
                assertEquals("Welcome to Chess!", messages.getText());
                move(view, "e2e5");
                assertTrue(messages.getText().contains("Not a legal move"));
                view.render(game);
                assertTrue(messages.getText().contains("Not a legal move"));
                assertTrue(game.history().isEmpty());
                move(view, "e2e4");
                assertEquals("e2e4", game.history().getFirst().toString());
                assertEquals("", messages.getText());
                ((JButton) field(view, "undo")).doClick(0);
                assertTrue(game.history().isEmpty());
                assertEquals(Color.WHITE, game.sideToMove());
            } catch (Exception exception) {
                throw new AssertionError(exception);
            }
        });
    }

    @Test
    void swingRetainsMateResultAndStopsAcceptingMoves() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                Game game = new Game();
                SwingView view = new SwingView();
                prepareComponents(view, game);
                for (String notation : new String[]{"f2f3", "e7e5", "g2g4", "d8h4"}) {
                    move(view, notation);
                }
                assertEquals(GameStatus.CHECKMATE, game.status());
                assertEquals("Checkmate. BLACK wins.",
                        ((JTextArea) field(view, "messages")).getText());
                assertEquals("Checkmate. BLACK wins.",
                        ((JLabel) field(view, "status")).getText());
                assertFalse((boolean) field(view, "disposed"));
                assertFalse(((JButton) field(view, "undo")).isEnabled());
                assertEquals("Close", ((JButton) field(view, "quit")).getText());
                move(view, "e2e4");
                assertEquals(4, game.history().size());
            } catch (Exception exception) {
                throw new AssertionError(exception);
            }
        });
    }
}
