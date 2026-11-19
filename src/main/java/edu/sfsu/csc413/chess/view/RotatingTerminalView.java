package edu.sfsu.csc413.chess.view;

import edu.sfsu.csc413.chess.engine.Game;
import edu.sfsu.csc413.chess.model.Color;
import edu.sfsu.csc413.chess.model.Piece;
import edu.sfsu.csc413.chess.model.Position;

import java.io.PrintStream;
import java.util.Scanner;

/** A bordered board that turns to face the player whose turn it is. */
public final class RotatingTerminalView implements BoardView {
    private static final String BORDER = "  +----+----+----+----+----+----+----+----+";
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
        StringBuilder files = new StringBuilder("  ");
        for (int column = 0; column < 8; column++) {
            int file = white ? column : 7 - column;
            files.append("   ").append((char) ('a' + file)).append(' ');
        }
        out.println();
        out.println("Viewing from " + game.sideToMove() + "'s side");
        out.println(files);
        out.println(BORDER);
        for (int row = 0; row < 8; row++) {
            int rank = white ? 7 - row : row;
            out.print((rank + 1) + " |");
            for (int column = 0; column < 8; column++) {
                int file = white ? column : 7 - column;
                Piece piece = game.board().pieceAt(new Position(file, rank));
                String token = piece == null ? ".."
                        : (piece.color() == Color.WHITE ? "w" : "b")
                        + piece.type().symbol();
                out.print(" " + token + " |");
            }
            out.println(" " + (rank + 1));
            out.println(BORDER);
        }
        out.println(files);
        out.println("w = White, b = Black; P pawn, R rook, N knight, B bishop, Q queen, K king");
        out.println(game.sideToMove() + " to move | " + game.status());
        var history = game.history();
        if (!history.isEmpty()) {
            out.println("Last move: " + history.get(history.size() - 1));
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
        out.print(game.sideToMove()
                + " move (e2e4, e7e8q/r/b/n, undo, moves, quit): ");
        out.flush();
        return PlayerAction.parse(in.hasNextLine() ? in.nextLine() : "quit");
    }
}
