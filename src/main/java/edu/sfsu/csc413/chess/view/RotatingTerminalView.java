package edu.sfsu.csc413.chess.view;

import edu.sfsu.csc413.chess.engine.Game;
import edu.sfsu.csc413.chess.model.Color;
import edu.sfsu.csc413.chess.model.Piece;
import edu.sfsu.csc413.chess.model.Position;

import java.io.PrintStream;
import java.util.Scanner;
import java.util.stream.Collectors;

/** Terminal view that rotates the board so the side to move is at the bottom. */
public final class RotatingTerminalView implements BoardView {
    private final PrintStream out;
    private final Scanner input;

    public RotatingTerminalView() {
        this(System.out, new Scanner(System.in));
    }

    /** Injectable streams make the terminal interaction straightforward to test. */
    public RotatingTerminalView(PrintStream out, Scanner input) {
        this.out = out;
        this.input = input;
    }

    @Override
    public void render(Game game) {
        boolean whitePerspective = game.sideToMove() == Color.WHITE;
        out.println();
        out.println("       +----+----+----+----+----+----+----+----+");
        for (int row = 0; row < 8; row++) {
            int rank = whitePerspective ? 7 - row : row;
            out.printf("   %d   |", rank + 1);
            for (int column = 0; column < 8; column++) {
                int file = whitePerspective ? column : 7 - column;
                Piece piece = game.board().pieceAt(new Position(file, rank));
                String symbol = piece == null ? " " : Character.toString(piece.symbol());
                out.printf(" %s  |", symbol);
            }
            out.println();
            out.println("       +----+----+----+----+----+----+----+----+");
        }
        out.print("         ");
        for (int column = 0; column < 8; column++) {
            int file = whitePerspective ? column : 7 - column;
            out.printf(" %c   ", (char) ('a' + file));
        }
        out.println();
        out.println(game.sideToMove() + " to move | " + game.status());
        if (game.history().isEmpty()) {
            out.println("History: (no moves yet)");
        } else {
            out.println("History: " + game.history().stream()
                    .map(Object::toString).collect(Collectors.joining(" ")));
        }
    }

    @Override
    public void showMessage(String message) {
        out.println(message);
    }

    @Override
    public void start(GameController controller) {
        controller.runLoop();
    }

    @Override
    public PlayerAction nextAction(Game game) {
        out.print("Move (e2e4), undo, moves, quit: ");
        out.flush();
        return PlayerAction.parse(input.hasNextLine() ? input.nextLine() : "quit");
    }
}
