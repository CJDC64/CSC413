package edu.sfsu.csc413.chess.view;

import edu.sfsu.csc413.chess.engine.Game;
import edu.sfsu.csc413.chess.model.Color;
import edu.sfsu.csc413.chess.model.Piece;
import edu.sfsu.csc413.chess.model.Position;

import java.io.PrintStream;
import java.util.Scanner;

/** A bordered board that turns to face the player whose turn it is. */
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
        StringBuilder files = new StringBuilder("  ");
        for (int column = 0; column < 8; column++) {
            int file = white ? column : 7 - column;
            files.append("   ").append((char) ('a' + file)).append(' ');
        }
        out.println();
        out.println("View from " + game.sideToMove() + "'s side");
        out.println(files);
        out.println(border);
        for (int row = 0; row < 8; row++) {
            int rank = white ? 7 - row : row;
            StringBuilder line = new StringBuilder((rank + 1) + " |");
            for (int column = 0; column < 8; column++) {
                int file = white ? column : 7 - column;
                Piece piece = game.board().pieceAt(new Position(file, rank));
                String label = piece == null ? ".."
                        : (piece.color() == Color.WHITE ? "w" : "b")
                                + piece.type().symbol();
                line.append(' ').append(label).append(" |");
            }
            out.println(line.append(' ').append(rank + 1));
            out.println(border);
        }
        out.println(files);
        out.println("w = White, b = Black; P pawn, R rook, N knight, B bishop, Q queen, K king");
        out.println(game.sideToMove() + " to move | " + game.status());
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
                + " move (e2e4, promotion e7e8q/r/b/n, undo, moves, quit): ");
        out.flush();
        return PlayerAction.parse(in.hasNextLine() ? in.nextLine() : "quit");
    }
}
