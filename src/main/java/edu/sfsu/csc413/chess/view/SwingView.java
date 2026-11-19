package edu.sfsu.csc413.chess.view;

import edu.sfsu.csc413.chess.engine.Game;
import edu.sfsu.csc413.chess.engine.GameStatus;
import edu.sfsu.csc413.chess.model.Color;
import edu.sfsu.csc413.chess.model.Move;
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
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Click-to-move Swing view. All game changes are delegated to the controller. */
public final class SwingView implements BoardView {
    private static final java.awt.Color LIGHT = new java.awt.Color(238, 238, 210);
    private static final java.awt.Color DARK = new java.awt.Color(118, 150, 86);
    private static final java.awt.Color SELECTED = new java.awt.Color(246, 190, 55);

    private final SquareButton[][] squares = new SquareButton[8][8];
    private final List<String> pendingMessages = new ArrayList<>();
    private Game game;
    private GameController controller;
    private JFrame frame;
    private JLabel statusLabel;
    private JTextArea messageArea;
    private Position selected;
    private Set<Position> destinations = Set.of();

    @Override
    public void render(Game currentGame) {
        game = currentGame;
        onEdt(() -> {
            if (frame != null) {
                refreshBoard();
            }
        });
    }

    @Override
    public void showMessage(String message) {
        onEdt(() -> {
            if (messageArea == null) {
                pendingMessages.add(message);
            } else {
                appendMessage(message);
            }
        });
    }

    @Override
    public void start(GameController gameController) {
        controller = gameController;
        onEdt(() -> {
            frame = new JFrame("CSC413 Chess");
            frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            JPanel root = new JPanel(new BorderLayout(12, 12));
            root.setBorder(new EmptyBorder(12, 12, 12, 12));

            JPanel board = new JPanel(new GridLayout(8, 8));
            for (int displayRank = 7; displayRank >= 0; displayRank--) {
                for (int file = 0; file < 8; file++) {
                    Position position = new Position(file, displayRank);
                    SquareButton square = new SquareButton();
                    square.setFont(new Font("DejaVu Sans", Font.PLAIN, 38));
                    square.setMargin(new Insets(0, 0, 0, 0));
                    square.setFocusPainted(false);
                    square.setOpaque(true);
                    square.setBorderPainted(true);
                    square.setPreferredSize(new Dimension(68, 68));
                    square.addActionListener(event -> selectOrMove(position));
                    squares[file][displayRank] = square;
                    board.add(square);
                }
            }
            root.add(board, BorderLayout.CENTER);

            JPanel side = new JPanel(new BorderLayout(8, 8));
            statusLabel = new JLabel(" ", SwingConstants.CENTER);
            statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD, 14f));
            side.add(statusLabel, BorderLayout.NORTH);

            messageArea = new JTextArea(12, 25);
            messageArea.setEditable(false);
            messageArea.setLineWrap(true);
            messageArea.setWrapStyleWord(true);
            side.add(new JScrollPane(messageArea), BorderLayout.CENTER);

            JPanel controls = new JPanel(new BorderLayout(6, 6));
            JButton undo = new JButton("Undo");
            undo.addActionListener(event -> controller.handle(new PlayerAction.Undo()));
            controls.add(undo, BorderLayout.CENTER);
            side.add(controls, BorderLayout.SOUTH);
            side.setPreferredSize(new Dimension(270, 520));
            root.add(side, BorderLayout.EAST);

            frame.setContentPane(root);
            frame.setMinimumSize(new Dimension(850, 610));
            frame.pack();
            refreshBoard();
            for (String message : pendingMessages) {
                appendMessage(message);
            }
            pendingMessages.clear();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    private void selectOrMove(Position position) {
        if (game == null || game.isGameOver()) {
            return;
        }
        Piece piece = game.board().pieceAt(position);
        if (selected != null && destinations.contains(position)) {
            String notation = selected + position.toString();
            selected = null;
            destinations = Set.of();
            controller.handle(new PlayerAction.Move(notation));
            return;
        }
        if (piece != null && piece.color() == game.sideToMove()) {
            selected = position;
            Set<Position> legalDestinations = new HashSet<>();
            for (Move move : game.legalMoves()) {
                if (move.from().equals(position)) {
                    legalDestinations.add(move.to());
                }
            }
            destinations = Set.copyOf(legalDestinations);
        } else {
            selected = null;
            destinations = Set.of();
        }
        refreshBoard();
    }

    private void refreshBoard() {
        if (game == null || statusLabel == null) {
            return;
        }
        for (int rank = 0; rank < 8; rank++) {
            for (int file = 0; file < 8; file++) {
                Position position = new Position(file, rank);
                SquareButton square = squares[file][rank];
                Piece piece = game.board().pieceAt(position);
                square.setBackground(((file + rank) & 1) == 0 ? LIGHT : DARK);
                square.setText(piece == null ? "" : glyph(piece));
                boolean darkSquare = ((file + rank) & 1) != 0;
                // Outline glyphs for White and filled glyphs for Black remain
                // high-contrast on either board colour.
                square.setForeground(piece == null || piece.color() == Color.WHITE
                        || !darkSquare
                        ? new java.awt.Color(25, 25, 25)
                        : new java.awt.Color(250, 250, 250));
                square.setSelectedSquare(position.equals(selected));
                square.setMarked(destinations.contains(position));
                square.setToolTipText(position.toString());
            }
        }
        GameStatus currentStatus = game.status();
        String message = switch (currentStatus) {
            case CHECK -> " — CHECK";
            case CHECKMATE -> " — CHECKMATE";
            case STALEMATE -> " — STALEMATE";
            case IN_PROGRESS -> "";
        };
        statusLabel.setText(game.sideToMove() + " to move" + message);
    }

    private String glyph(Piece piece) {
        boolean white = piece.color() == Color.WHITE;
        return switch (piece.type()) {
            case KING -> white ? "♔" : "♚";
            case QUEEN -> white ? "♕" : "♛";
            case ROOK -> white ? "♖" : "♜";
            case BISHOP -> white ? "♗" : "♝";
            case KNIGHT -> white ? "♘" : "♞";
            case PAWN -> white ? "♙" : "♟";
        };
    }

    private void appendMessage(String message) {
        messageArea.append(message + "\n");
        messageArea.setCaretPosition(messageArea.getDocument().getLength());
    }

    @Override
    public PieceType promotionChoice(Game currentGame) {
        if (!SwingUtilities.isEventDispatchThread()) {
            final PieceType[] answer = new PieceType[1];
            try {
                SwingUtilities.invokeAndWait(() -> answer[0] = askPromotion());
            } catch (Exception exception) {
                return PieceType.QUEEN;
            }
            return answer[0];
        }
        return askPromotion();
    }

    private PieceType askPromotion() {
        Object choice = JOptionPane.showInputDialog(frame,
                "Choose a promotion piece:", "Pawn promotion",
                JOptionPane.QUESTION_MESSAGE, null,
                new String[]{"Queen", "Rook", "Bishop", "Knight"}, "Queen");
        if (choice == null) {
            return PieceType.QUEEN;
        }
        return switch (choice.toString()) {
            case "Rook" -> PieceType.ROOK;
            case "Bishop" -> PieceType.BISHOP;
            case "Knight" -> PieceType.KNIGHT;
            default -> PieceType.QUEEN;
        };
    }

    @Override
    public void close() {
        onEdt(() -> {
            if (frame != null) {
                // Keep the final position and result visible until the player closes the window.
                frame.setTitle("CSC413 Chess — game over");
                if (statusLabel != null) {
                    statusLabel.setText("Game finished — see the final result above.");
                }
            }
        });
    }

    private void onEdt(Runnable task) {
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
        } else {
            SwingUtilities.invokeLater(task);
        }
    }

    private static final class SquareButton extends JButton {
        private boolean marked;
        private boolean selectedSquare;

        void setMarked(boolean value) { marked = value; repaint(); }
        void setSelectedSquare(boolean value) { selectedSquare = value; repaint(); }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (marked) {
                graphics.setColor(new java.awt.Color(35, 35, 35, 175));
                int diameter = 11;
                graphics.fillOval(getWidth() - diameter - 8,
                        getHeight() - diameter - 8, diameter, diameter);
            }
            if (selectedSquare) {
                graphics.setColor(SELECTED);
                graphics.drawRect(3, 3, getWidth() - 7, getHeight() - 7);
                graphics.drawRect(4, 4, getWidth() - 9, getHeight() - 9);
            }
        }
    }
}
