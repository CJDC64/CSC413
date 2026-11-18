package edu.sfsu.csc413.chess.view;

import edu.sfsu.csc413.chess.engine.Game;
import edu.sfsu.csc413.chess.engine.GameStatus;
import edu.sfsu.csc413.chess.model.Color;
import edu.sfsu.csc413.chess.model.Piece;
import edu.sfsu.csc413.chess.model.PieceType;
import edu.sfsu.csc413.chess.model.Position;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/** Click a source and destination; all game changes go through the controller. */
public final class SwingView implements BoardView {
    // View state and components are confined to the event dispatch thread.
    private Game game;
    private GameController controller;
    private String message = "";
    private Position selected;
    private boolean finished;
    private boolean disposed;
    private JFrame frame;
    private JLabel status;
    private JLabel selection;
    private JTextArea messages;
    private JButton undo;
    private JButton quit;
    private final JButton[][] squares = new JButton[8][8];

    private static void onEdt(Runnable task) {
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
        } else {
            SwingUtilities.invokeLater(task);
        }
    }

    @Override
    public void showMessage(String text) {
        onEdt(() -> {
            message = text;
            if (messages != null) {
                messages.setText(message);
                messages.setCaretPosition(0);
            }
        });
    }

    @Override
    public void render(Game currentGame) {
        onEdt(() -> {
            game = currentGame;
            selected = null;
            refresh();
        });
    }

    @Override
    public void start(GameController currentController) {
        onEdt(() -> {
            if (frame != null || disposed) {
                return;
            }
            controller = currentController;
            game = controller.game();
            frame = new JFrame("CSC413 Chess");
            frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent event) {
                    requestQuit();
                }
            });
            frame.setContentPane(buildContent());
            refresh();
            frame.pack();
            frame.setMinimumSize(frame.getSize());
            frame.setLocationByPlatform(true);
            frame.setVisible(true);
        });
    }

    private JPanel buildContent() {
        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        status = new JLabel();
        status.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        content.add(status, BorderLayout.NORTH);
        JPanel board = new JPanel(new GridLayout(9, 9));
        board.setPreferredSize(new Dimension(630, 630));
        for (int rank = 7; rank >= 0; rank--) {
            board.add(new JLabel(Integer.toString(rank + 1), SwingConstants.CENTER));
            for (int file = 0; file < 8; file++) {
                Position position = new Position(file, rank);
                JButton square = new JButton();
                square.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 25));
                square.setForeground(java.awt.Color.BLACK);
                square.setBackground((file + rank) % 2 == 0
                        ? new java.awt.Color(185, 156, 117)
                        : new java.awt.Color(250, 240, 215));
                square.setOpaque(true);
                square.setContentAreaFilled(true);
                square.setFocusPainted(false);
                square.setMargin(new java.awt.Insets(0, 0, 0, 0));
                square.addActionListener(event -> clickSquare(position));
                squares[file][rank] = square;
                board.add(square);
            }
        }
        board.add(new JLabel());
        for (char file = 'a'; file <= 'h'; file++) {
            board.add(new JLabel(String.valueOf(file), SwingConstants.CENTER));
        }
        content.add(board, BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout(4, 4));
        selection = new JLabel();
        bottom.add(selection, BorderLayout.NORTH);
        messages = new JTextArea(message, 3, 40);
        messages.setEditable(false);
        messages.setLineWrap(true);
        messages.setWrapStyleWord(true);
        bottom.add(new JScrollPane(messages), BorderLayout.CENTER);
        JPanel controls = new JPanel();
        controls.add(new JLabel("w=White b=Black | P pawn R rook N knight B bishop Q queen K king"));
        undo = new JButton("Undo");
        undo.addActionListener(event -> send(new PlayerAction.Undo()));
        controls.add(undo);
        quit = new JButton("Quit");
        quit.addActionListener(event -> requestQuit());
        controls.add(quit);
        bottom.add(controls, BorderLayout.SOUTH);
        content.add(bottom, BorderLayout.SOUTH);
        return content;
    }

    private void clickSquare(Position destination) {
        if (finished || disposed || game == null) {
            return;
        }
        if (selected == null) {
            Piece piece = game.board().pieceAt(destination);
            if (piece == null || piece.color() != game.sideToMove()) {
                showMessage("Select a " + game.sideToMove() + " piece first.");
                return;
            }
            selected = destination;
            refresh();
            return;
        }
        if (selected.equals(destination)) {
            selected = null;
            refresh();
            return;
        }
        Position source = selected;
        String notation = source.toString() + destination;
        // Consult engine moves, rather than reproducing pawn/promotion rules.
        boolean promotion = game.legalMoves().stream().anyMatch(move ->
                move.from().equals(source) && move.to().equals(destination)
                        && move.isPromotion());
        if (promotion) {
            PieceType[] types = {PieceType.QUEEN, PieceType.ROOK,
                    PieceType.BISHOP, PieceType.KNIGHT};
            int choice = JOptionPane.showOptionDialog(frame,
                    "Promote the pawn to:", "Promotion",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                    null, new String[]{"Queen", "Rook", "Bishop", "Knight"}, "Queen");
            if (choice < 0) {
                return; // Cancelling preserves the selection and the position.
            }
            notation += Character.toLowerCase(types[choice].symbol());
        }
        send(new PlayerAction.Move(notation));
    }

    private void send(PlayerAction action) {
        if (finished || disposed) {
            return;
        }
        selected = null;
        showMessage(""); // Clear old feedback before, never after, handling input.
        controller.handle(action);
    }

    private void refresh() {
        if (status == null || game == null) {
            return;
        }
        GameStatus state = game.status();
        finished = state.isGameOver();
        status.setText(switch (state) {
            case IN_PROGRESS -> game.sideToMove() + " to move";
            case CHECK -> game.sideToMove() + " to move - CHECK";
            case CHECKMATE -> "Checkmate. " + game.sideToMove().opposite() + " wins.";
            case STALEMATE -> "Stalemate. The game is a draw.";
        });
        selection.setText(finished ? "Game over. Close the window when ready."
                : selected == null ? "Click a piece, then its destination."
                : "Selected " + selected + ". Click its destination, or click it again to cancel.");
        undo.setEnabled(!finished);
        quit.setText(finished ? "Close" : "Quit");
        for (int rank = 0; rank < 8; rank++) {
            for (int file = 0; file < 8; file++) {
                Position position = new Position(file, rank);
                Piece piece = game.board().pieceAt(position);
                JButton square = squares[file][rank];
                square.setText(piece == null ? "" :
                        (piece.color() == Color.WHITE ? "w" : "b") + piece.type().symbol());
                String description = position + ": " + (piece == null ? "empty"
                        : piece.color() + " " + piece.type());
                square.setToolTipText(description);
                square.getAccessibleContext().setAccessibleName(description);
                square.setBorder(BorderFactory.createLineBorder(
                        position.equals(selected) ? new java.awt.Color(0, 75, 190)
                                : java.awt.Color.DARK_GRAY,
                        position.equals(selected) ? 4 : 1));
            }
        }
    }

    private void requestQuit() {
        if (finished) {
            disposed = true;
            frame.dispose();
        } else {
            controller.handle(new PlayerAction.Quit());
        }
    }

    @Override
    public void close() {
        onEdt(() -> {
            if (game != null && game.status().isGameOver()) {
                // The controller closes after announcing a result. Keep it visible
                // until the player explicitly closes the window.
                finished = true;
                refresh();
            } else {
                disposed = true;
                if (frame != null) {
                    frame.dispose();
                }
            }
        });
    }
}
