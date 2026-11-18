package edu.sfsu.csc413.chess.view;

import edu.sfsu.csc413.chess.engine.Game;
import edu.sfsu.csc413.chess.model.Color;
import edu.sfsu.csc413.chess.model.Position;

import java.io.PrintStream;
import java.util.Scanner;

/** A bordered terminal board that faces the player whose turn it is. */
public final class RotatingTerminalView implements BoardView {
    private final PrintStream out;
    private final Scanner in;

    public RotatingTerminalView() {
        this(System.out, new Scanner(System.in));
    }

    public RotatingTerminalView(PrintStream out, Scanner in) {
        this.out = out;
        this.in = in;
    }

    @Override
    public void render(Game game) {
        boolean white = game.sideToMove() == Color.WHITE;
        String border = "  +----+----+----+----+----+----+----+----+";
        out.println();
        out.println(border);
        for (int row = 0; row < 8; row++) {
            int rank = white ? 7 - row : row;
            out.print((rank + 1) + " |");
            for (int col = 0; col < 8; col++) {
                int file = white ? col : 7 - col;
                var piece = game.board().pieceAt(new Position(file, rank));
                String label = piece == null ? "  "
                        : (piece.color() == Color.WHITE ? "w" : "b") + piece.type().symbol();
                out.print(" " + label + " |");
            }
            out.println();
            out.println(border);
        }
        out.print("  ");
        for (int col = 0; col < 8; col++) {
            out.print("  " + (char) ('a' + (white ? col : 7 - col)) + "  ");
        }
        out.println();
        out.println("Facing " + game.sideToMove() + " | " + game.sideToMove()
                + " to move | " + game.status() + " | " + game.history().size() + " plies");
        out.println("w = white, b = black; N = knight. Coordinates stay standard when rotated.");
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
        out.print("Move (e2e4 / e7e8n), undo, moves, quit: ");
        out.flush();
        return PlayerAction.parse(in.hasNextLine() ? in.nextLine() : "quit");
    }
}
