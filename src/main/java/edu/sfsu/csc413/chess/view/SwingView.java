package edu.sfsu.csc413.chess.view;

import edu.sfsu.csc413.chess.engine.Game;
import edu.sfsu.csc413.chess.model.Piece;
import edu.sfsu.csc413.chess.model.PieceType;
import edu.sfsu.csc413.chess.model.Position;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashSet;
import java.util.Set;

/** An event-driven view; all view state and components belong to the EDT. */
public final class SwingView implements BoardView {
    private static final Color LIGHT = new Color(245, 236, 215);
    private static final Color DARK = new Color(151, 181, 197);
    private static final Color DESTINATION = new Color(37, 56, 116);
    private final JButton[][] squares = new JButton[8][8];
    private Game game;
    private GameController controller;
    private String message = "";
    private Position selected;
    private JFrame frame;
    private JLabel status;
    private JTextArea messages;
    private JButton undo;
    private JButton closeButton;
    private boolean finished;
    private boolean disposed;

    private static void onEdt(Runnable action) {
        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
        } else {
            SwingUtilities.invokeLater(action);
        }
    }

    @Override
    public void render(Game game) {
        onEdt(() -> {
            this.game = game;
            selected = null;
            refresh();
        });
    }

    @Override
    public void showMessage(String message) {
        onEdt(() -> {
            this.message = message;
            if (messages != null) {
                messages.setText(message);
                messages.setCaretPosition(0);
            }
        });
    }

    @Override
    public void start(GameController controller) {
        onEdt(() -> {
            if (frame != null || disposed) {
                return;
            }
            this.controller = controller;
            buildWindow();
        });
    }

    private void buildWindow() {
        frame = new JFrame("CSC413 Chess");
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                requestClose();
            }
        });

        JPanel board = new JPanel(new GridLayout(10, 10));
        board.setPreferredSize(new Dimension(620, 620));
        for (int row = 0; row < 10; row++) {
            for (int column = 0; column < 10; column++) {
                if (row == 0 || row == 9 || column == 0 || column == 9) {
                    String label = "";
                    if (column > 0 && column < 9) {
                        label = Character.toString((char) ('a' + column - 1));
                    } else if (row > 0 && row < 9) {
                        label = Integer.toString(9 - row);
                    }
                    board.add(new JLabel(label, SwingConstants.CENTER));
                } else {
                    Position position = new Position(column - 1, 8 - row);
                    JButton button = new JButton();
                    button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 23));
                    button.setMargin(new Insets(0, 0, 0, 0));
                    button.setForeground(Color.BLACK);
                    button.setOpaque(true);
                    button.setContentAreaFilled(true);
                    button.addActionListener(event -> clickSquare(position));
                    squares[position.file()][position.rank()] = button;
                    board.add(button);
                }
            }
        }

        status = new JLabel(" ", SwingConstants.CENTER);
        status.setBorder(BorderFactory.createEmptyBorder(10, 8, 10, 8));
        messages = new JTextArea(message, 3, 45);
        messages.setEditable(false);
        messages.setLineWrap(true);
        messages.setWrapStyleWord(true);
        undo = new JButton("Undo");
        undo.addActionListener(event -> send(new PlayerAction.Undo()));
        closeButton = new JButton("Quit");
        closeButton.addActionListener(event -> requestClose());
        JPanel buttons = new JPanel();
        buttons.add(undo);
        buttons.add(closeButton);
        JPanel footer = new JPanel(new BorderLayout());
        footer.add(new JLabel("w = White, b = Black | P pawn, R rook, N knight, "
                + "B bishop, Q queen, K king", SwingConstants.CENTER), BorderLayout.NORTH);
        footer.add(new JScrollPane(messages), BorderLayout.CENTER);
        footer.add(buttons, BorderLayout.SOUTH);
        frame.add(status, BorderLayout.NORTH);
        frame.add(board, BorderLayout.CENTER);
        frame.add(footer, BorderLayout.SOUTH);
        refresh();
        frame.pack();
        frame.setMinimumSize(frame.getSize());
        frame.setLocationByPlatform(true);
        frame.setVisible(true);
    }

    private void refresh() {
        if (frame == null || game == null) {
            return;
        }
        var gameStatus = game.status();
        finished = gameStatus.isGameOver();
        status.setText(switch (gameStatus) {
            case IN_PROGRESS -> game.sideToMove() + " to move";
            case CHECK -> game.sideToMove() + " to move — CHECK";
            case CHECKMATE -> "CHECKMATE — " + game.sideToMove().opposite() + " wins";
            case STALEMATE -> "STALEMATE — Draw";
        });
        Set<Position> destinations = new HashSet<>();
        if (selected != null && !finished) {
            game.legalMoves().stream()
                    .filter(move -> move.from().equals(selected))
                    .forEach(move -> destinations.add(move.to()));
        }
        for (int file = 0; file < 8; file++) {
            for (int rank = 0; rank < 8; rank++) {
                Position position = new Position(file, rank);
                Piece piece = game.board().pieceAt(position);
                JButton button = squares[file][rank];
                String token = piece == null ? ""
                        : (piece.color() == edu.sfsu.csc413.chess.model.Color.WHITE ? "w" : "b")
                        + piece.type().symbol();
                button.setText(token);
                button.setBackground((file + rank) % 2 == 0 ? DARK : LIGHT);
                Color border = position.equals(selected) ? new Color(173, 91, 0)
                        : destinations.contains(position) ? DESTINATION : Color.GRAY;
                button.setBorder(BorderFactory.createLineBorder(border,
                        position.equals(selected) || destinations.contains(position) ? 4 : 1));
                String description = position + ": " + (piece == null ? "empty"
                        : piece.color() + " " + piece.type())
                        + (destinations.contains(position) ? " (legal destination)" : "");
                button.setToolTipText(description);
                button.getAccessibleContext().setAccessibleName(description);
                // Keep normal piece contrast on the final board; the handler blocks play.
            }
        }
        undo.setEnabled(!finished);
        closeButton.setText(finished ? "Close" : "Quit");
    }

    private void clickSquare(Position position) {
        if (game == null || finished || disposed) {
            return;
        }
        Piece piece = game.board().pieceAt(position);
        if (position.equals(selected)) {
            selected = null;
            refresh();
        } else if (piece != null && piece.color() == game.sideToMove()) {
            selected = position;
            refresh();
        } else if (selected != null) {
            String notation = selected.toString() + position;
            boolean promotion = game.legalMoves().stream().anyMatch(move ->
                    move.from().equals(selected) && move.to().equals(position)
                            && move.isPromotion());
            if (promotion) {
                PieceType[] types = {PieceType.QUEEN, PieceType.ROOK,
                        PieceType.BISHOP, PieceType.KNIGHT};
                String[] names = {"Queen", "Rook", "Bishop", "Knight"};
                int choice = JOptionPane.showOptionDialog(frame, "Promote pawn to:",
                        "Promotion", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                        null, names, names[0]);
                if (choice < 0) {
                    return;
                }
                notation += Character.toLowerCase(types[choice].symbol());
            }
            send(new PlayerAction.Move(notation));
        }
    }

    private void send(PlayerAction action) {
        if (finished || disposed) {
            return;
        }
        selected = null;
        // Clear the previous message only when a new action begins, never in render.
        showMessage("");
        controller.handle(action);
    }

    private void requestClose() {
        if (finished) {
            disposed = true;
            frame.dispose();
        } else {
            send(new PlayerAction.Quit());
        }
    }

    @Override
    public void close() {
        onEdt(() -> {
            if (game != null && game.status().isGameOver()) {
                selected = null;
                refresh();
                // The result and final board stay visible until the player clicks Close.
            } else {
                disposed = true;
                if (frame != null) {
                    frame.dispose();
                }
            }
        });
    }
}
