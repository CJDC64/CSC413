package edu.sfsu.csc413.chess.view;

import edu.sfsu.csc413.chess.engine.Game;
import edu.sfsu.csc413.chess.engine.GameStatus;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class RotatingTerminalViewTest {
    @Test
    void playsThroughControllerIncludingIllegalMoveUndoAndMate() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        RotatingTerminalView view = new RotatingTerminalView(new PrintStream(output),
                new Scanner("e2e5\ne2e4\nundo\nf2f3\ne7e5\ng2g4\nd8h4\n"));
        Game game = new Game();
        new GameController(game, view).start();
        String text = output.toString();
        assertEquals(GameStatus.CHECKMATE, game.status());
        assertEquals(4, game.history().size());
        assertTrue(text.contains("Not a legal move: 'e2e5'."));
        assertTrue(text.contains("Facing WHITE"));
        assertTrue(text.contains("Facing BLACK"));
        assertTrue(text.contains("h    g    f    e    d    c    b    a"));
        assertTrue(text.contains("Checkmate. BLACK wins."));
    }

    @Test
    void endOfInputQuits() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        new GameController(new Game(), new RotatingTerminalView(new PrintStream(output), new Scanner(""))).start();
        assertTrue(output.toString().contains("Goodbye."));
    }
}
