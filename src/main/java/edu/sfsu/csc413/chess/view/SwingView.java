package edu.sfsu.csc413.chess.view;

import edu.sfsu.csc413.chess.engine.Game;
import edu.sfsu.csc413.chess.model.Color;
import edu.sfsu.csc413.chess.model.Move;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Click-driven board; all view state and Swing work are confined to the EDT. */
public final class SwingView implements BoardView {
    private final List<String> messages = new ArrayList<>();
    private final JButton[][] squares = new JButton[8][8];
    private Game game;
    private GameController controller;
    private JFrame frame;
    private JLabel status;
    private JTextArea messageHistory;
    private JButton undo;
    private Position selected;
    private boolean closed;

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
            if (frame != null) {
                refreshBoard();
            }
        });
    }

    @Override
    public void showMessage(String message) {
        onEdt(() -> {
            messages.add(message);
            if (messageHistory != null) {
                messageHistory.append(message + "\n");
                messageHistory.setCaretPosition(messageHistory.getDocument().getLength());
            }
        });
    }

    @Override
    public void start(GameController controller) {
        onEdt(() -> {
            if (frame != null || closed) {
                return;
            }
            this.controller = controller;
            frame = new JFrame("CSC413 Chess");
            frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent event) {
                    if (closed) {
                        frame.dispose();
                    } else {
                        controller.handle(new PlayerAction.Quit());
                    }
                }
            });

            status = new JLabel(" ");
            JPanel board = new JPanel(new GridLayout(9, 9, 2, 2));
            board.setPreferredSize(new Dimension(630, 630));
            for (int rank = 7; rank >= 0; rank--) {
                board.add(new JLabel(Integer.toString(rank + 1), SwingConstants.CENTER));
                for (int file = 0; file < 8; file++) {
                    Position position = new Position(file, rank);
                    JButton button = new JButton();
                    button.setFont(new Font(Font.MONOSPACED, Font.BOLD, 22));
                    button.setMargin(new java.awt.Insets(0, 0, 0, 0));
                    button.setOpaque(true);
                    button.setContentAreaFilled(true);
                    button.addActionListener(event -> clickSquare(position));
                    squares[file][rank] = button;
                    board.add(button);
                }
            }
            board.add(new JLabel());
            for (char file = 'a'; file <= 'h'; file++) {
                board.add(new JLabel(Character.toString(file), SwingConstants.CENTER));
            }

            undo = new JButton("Undo");
            undo.addActionListener(event -> controller.handle(new PlayerAction.Undo()));
            JPanel controls = new JPanel(new BorderLayout(10, 0));
            controls.add(undo, BorderLayout.WEST);
            controls.add(new JLabel("w = White, b = Black; N = Knight. Green = legal destination."),
                    BorderLayout.CENTER);
            messageHistory = new JTextArea(6, 50);
            messageHistory.setEditable(false);
            messageHistory.setLineWrap(true);
            messageHistory.setWrapStyleWord(true);
            messages.forEach(message -> messageHistory.append(message + "\n"));
            messageHistory.setCaretPosition(messageHistory.getDocument().getLength());
            JScrollPane scroll = new JScrollPane(messageHistory);
            scroll.setBorder(BorderFactory.createTitledBorder("Controller messages"));
            JPanel bottom = new JPanel(new BorderLayout());
            bottom.add(controls, BorderLayout.NORTH);
            bottom.add(scroll, BorderLayout.CENTER);
            frame.add(status, BorderLayout.NORTH);
            frame.add(board, BorderLayout.CENTER);
            frame.add(bottom, BorderLayout.SOUTH);
            if (game != null) {
                refreshBoard();
            }
            frame.pack();
            frame.setLocationByPlatform(true);
            frame.setVisible(true);
        });
    }

    private void clickSquare(Position position) {
        if (closed || game == null || game.status().isGameOver()) {
            return;
        }
        Piece piece = game.board().pieceAt(position);
        if (position.equals(selected)) {
            selected = null;
            refreshBoard();
        } else if (piece != null && piece.color() == game.sideToMove()) {
            selected = position;
            refreshBoard();
        } else if (selected != null) {
            Position from = selected;
            String notation = from.toString() + position;
            boolean promotion = game.legalMoves().stream().anyMatch(move ->
                    move.from().equals(from) && move.to().equals(position) && move.isPromotion());
            if (promotion) {
                PieceType[] choices = {PieceType.QUEEN, PieceType.ROOK,
                        PieceType.BISHOP, PieceType.KNIGHT};
                int choice = JOptionPane.showOptionDialog(frame, "Promote pawn to:", "Promotion",
                        JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                        new String[]{"Queen", "Rook", "Bishop", "Knight"}, "Queen");
                if (choice < 0) {
                    return;
                }
                notation += Character.toLowerCase(choices[choice].symbol());
            }
            controller.handle(new PlayerAction.Move(notation));
        }
    }

    private void refreshBoard() {
        Set<Position> destinations = new HashSet<>();
        if (selected != null) {
            for (Move move : game.legalMoves()) {
                if (move.from().equals(selected)) {
                    destinations.add(move.to());
                }
            }
        }
        boolean finished = game.status().isGameOver();
        for (int file = 0; file < 8; file++) {
            for (int rank = 0; rank < 8; rank++) {
                Position position = new Position(file, rank);
                Piece piece = game.board().pieceAt(position);
                JButton button = squares[file][rank];
                // Explicit side letters and black text avoid font/glyph and colour ambiguity.
                button.setText(piece == null ? "" :
                        (piece.color() == Color.WHITE ? "w" : "b") + piece.type().symbol());
                button.setForeground(java.awt.Color.BLACK);
                button.setBackground(destinations.contains(position)
                        ? new java.awt.Color(150, 220, 155)
                        : (file + rank) % 2 == 0
                        ? new java.awt.Color(165, 182, 200) : new java.awt.Color(245, 239, 220));
                button.setBorder(BorderFactory.createLineBorder(position.equals(selected)
                        ? new java.awt.Color(190, 100, 0) : java.awt.Color.GRAY,
                        position.equals(selected) ? 4 : 1));
                String description = position + (piece == null ? " empty" :
                        " " + piece.color() + " " + piece.type());
                button.setToolTipText(description);
                button.getAccessibleContext().setAccessibleName(description);
                // Keep pieces fully contrasted at game end; the click handler blocks input.
            }
        }
        status.setText(switch (game.status()) {
            case IN_PROGRESS -> game.sideToMove() + " to move";
            case CHECK -> game.sideToMove() + " to move — CHECK";
            case CHECKMATE -> "CHECKMATE — " + game.sideToMove().opposite() + " wins";
            case STALEMATE -> "STALEMATE — Draw";
        });
        undo.setEnabled(!closed && !finished);
    }

    @Override
    public void close() {
        onEdt(() -> {
            closed = true;
            selected = null;
            if (frame == null) {
                return;
            }
            if (game != null && game.status().isGameOver()) {
                refreshBoard();
                frame.setTitle("CSC413 Chess — Game over (close window when finished)");
            } else {
                frame.dispose();
            }
        });
    }
}
