package edu.sfsu.csc413.chess.view;

import edu.sfsu.csc413.chess.engine.Game;
import edu.sfsu.csc413.chess.model.Move;
import edu.sfsu.csc413.chess.model.Piece;
import edu.sfsu.csc413.chess.model.Position;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

/** Click a piece, then its destination. Only the controller changes the game. */
public final class SwingView implements BoardView {
    private static final Color LIGHT = new Color(237, 225, 201);
    private static final Color DARK = new Color(128, 151, 130);
    private static final Color SELECTED = new Color(180, 95, 0);
    private static final Color LEGAL = new Color(0, 80, 150);

    // All state below is confined to the event dispatch thread, including early calls.
    private Game game;
    private GameController controller;
    private JFrame frame;
    private JPanel content;
    private final JButton[][] squares = new JButton[8][8];
    private JLabel status;
    private JLabel selection;
    private JTextArea messages;
    private JButton undo;
    private JButton moves;
    private JButton quit;
    private final StringBuilder pendingMessages = new StringBuilder();
    private Position selected;
    private List<Move> legalMoves = List.of();
    private boolean closed;
    private boolean quitting;

    private static void onEdt(Runnable work) {
        if (SwingUtilities.isEventDispatchThread()) {
            work.run();
        } else {
            SwingUtilities.invokeLater(work);
        }
    }

    @Override
    public void render(Game game) {
        onEdt(() -> {
            this.game = game;
            selected = null;
            if (content != null) refresh();
        });
    }

    @Override
    public void showMessage(String message) {
        onEdt(() -> {
            pendingMessages.append(message).append('\n');
            if (messages != null) refreshMessages();
        });
    }

    @Override
    public void start(GameController controller) {
        onEdt(() -> {
            if (frame != null || quitting) return;
            frame = new JFrame("CSC413 Chess");
            frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent event) {
                    requestQuit();
                }
            });
            frame.setContentPane(createContent(controller));
            frame.pack();
            frame.setMinimumSize(frame.getSize());
            frame.setLocationByPlatform(true);
            frame.setVisible(true);
        });
    }

    /** Package access allows headless tests to exercise the real buttons on the EDT. */
    JPanel createContent(GameController controller) {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("Build Swing components on the EDT.");
        }
        this.controller = controller;
        if (game == null) game = controller.game();
        content = new JPanel(new BorderLayout(8, 8));
        content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        JPanel heading = new JPanel(new GridLayout(0, 1, 0, 4));
        status = new JLabel();
        status.setFont(status.getFont().deriveFont(Font.BOLD, 16f));
        selection = new JLabel("Click a piece, then a destination.");
        heading.add(status);
        heading.add(selection);
        heading.add(new JLabel("K King · Q Queen · R Rook · B Bishop · N Knight · P Pawn"));
        heading.add(new JLabel("White / black discs identify sides. Promotion automatically chooses queen."));
        content.add(heading, BorderLayout.NORTH);

        JPanel board = new JPanel(new GridLayout(9, 9));
        for (int rank = 7; rank >= 0; rank--) {
            board.add(new JLabel(Integer.toString(rank + 1), SwingConstants.CENTER));
            for (int file = 0; file < 8; file++) {
                Position position = new Position(file, rank);
                JButton square = new JButton();
                square.setName(position.toString());
                square.setPreferredSize(new Dimension(64, 60));
                square.setOpaque(true);
                square.setContentAreaFilled(true);
                square.setBackground((file + rank) % 2 == 0 ? DARK : LIGHT);
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

        JPanel footer = new JPanel(new BorderLayout(4, 4));
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        undo = new JButton("Undo");
        undo.addActionListener(event -> send(new PlayerAction.Undo()));
        moves = new JButton("Legal moves");
        moves.addActionListener(event -> send(new PlayerAction.ShowMoves()));
        quit = new JButton("Quit");
        quit.addActionListener(event -> requestQuit());
        buttons.add(undo);
        buttons.add(moves);
        buttons.add(quit);
        footer.add(buttons, BorderLayout.NORTH);
        messages = new JTextArea(4, 45);
        messages.setEditable(false);
        messages.setLineWrap(true);
        messages.setWrapStyleWord(true);
        messages.getAccessibleContext().setAccessibleName("Controller messages");
        footer.add(new JScrollPane(messages), BorderLayout.CENTER);
        content.add(footer, BorderLayout.SOUTH);
        refreshMessages();
        refresh();
        return content;
    }

    private void clickSquare(Position position) {
        if (closed) return;
        if (position.equals(selected)) {
            selected = null;
        } else {
            Piece piece = game.board().pieceAt(position);
            if (piece != null && piece.color() == game.sideToMove()) {
                selected = position;
            } else if (selected != null) {
                String notation = selected.toString() + position;
                selected = null;
                send(new PlayerAction.Move(notation));
                return;
            }
        }
        refresh();
    }

    private void send(PlayerAction action) {
        if (closed) return;
        selected = null;
        controller.handle(action);
    }

    private void requestQuit() {
        if (closed) {
            if (frame != null) frame.dispose();
        } else {
            quitting = true;
            controller.handle(new PlayerAction.Quit());
        }
    }

    private void refreshMessages() {
        messages.setText(pendingMessages.toString());
        messages.setCaretPosition(messages.getDocument().getLength());
    }

    private void refresh() {
        var gameStatus = game.status();
        boolean finished = closed || gameStatus.isGameOver();
        legalMoves = finished ? List.of() : game.legalMoves();
        status.setText(game.sideToMove() + " to move | " + gameStatus
                + " | " + game.history().size() + " plies");
        selection.setText(finished ? "Game ended. Read the result below, then close the window."
                : selected == null ? "Click a piece, then a destination."
                : "Selected " + selected + "; blue borders show legal destinations. Click again to cancel.");
        for (int file = 0; file < 8; file++) {
            for (int rank = 0; rank < 8; rank++) {
                Position position = new Position(file, rank);
                Piece piece = game.board().pieceAt(position);
                JButton square = squares[file][rank];
                Icon icon = piece == null ? null : new PieceIcon(piece);
                square.setIcon(icon);
                // Preserve contrast when the final position is disabled.
                square.setDisabledIcon(icon);
                String description = position + (piece == null ? " empty"
                        : " " + piece.color() + " " + piece.type());
                square.setToolTipText(description);
                square.getAccessibleContext().setAccessibleName(description);
                boolean destination = selected != null && legalMoves.stream()
                        .anyMatch(move -> move.from().equals(selected) && move.to().equals(position));
                square.setBorder(position.equals(selected) ? BorderFactory.createLineBorder(SELECTED, 4)
                        : destination ? BorderFactory.createLineBorder(LEGAL, 4)
                        : BorderFactory.createEmptyBorder(4, 4, 4, 4));
                square.setEnabled(!finished);
            }
        }
        undo.setEnabled(!finished);
        moves.setEnabled(!finished);
        quit.setText(finished ? "Close" : "Quit");
        // Deliberately leave the controller's message log untouched by rendering.
    }

    @Override
    public void close() {
        onEdt(() -> {
            closed = true;
            selected = null;
            if (quitting) {
                if (frame != null) frame.dispose();
            } else if (content != null) {
                // Checkmate/stalemate leaves a read-only board until the user closes it.
                refresh();
            }
        });
    }

    /** JDK-drawn discs and letters avoid platform-dependent chess glyphs. */
    private record PieceIcon(Piece piece) implements Icon {
        @Override public int getIconWidth() { return 42; }
        @Override public int getIconHeight() { return 42; }

        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean white = piece.color() == edu.sfsu.csc413.chess.model.Color.WHITE;
                g.setColor(white ? Color.WHITE : new Color(25, 25, 25));
                g.fillOval(x + 1, y + 1, 39, 39);
                g.setColor(white ? Color.BLACK : Color.WHITE);
                g.setStroke(new BasicStroke(2));
                g.drawOval(x + 1, y + 1, 39, 39);
                g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
                String label = String.valueOf(piece.type().symbol());
                FontMetrics metrics = g.getFontMetrics();
                g.drawString(label, x + (42 - metrics.stringWidth(label)) / 2,
                        y + (42 - metrics.getHeight()) / 2 + metrics.getAscent());
            } finally {
                g.dispose();
            }
        }
    }
}
