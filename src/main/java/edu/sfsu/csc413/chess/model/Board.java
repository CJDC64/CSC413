package edu.sfsu.csc413.chess.model;

import java.util.ArrayList;
import java.util.List;
import edu.sfsu.csc413.chess.model.Position;

public class Board {
    private final Piece[][] squares;

    public Board() {
        squares = new Piece[8][8];
    }

    public void apply(Move move) {// lift the piece off `from`, set it down on `to`
        Piece destinationPiece = move.moved();

        if (move.isPromotion()) {
            destinationPiece = createPromoted(
                    move.promotesTo(), move.moved().color());
        }

        place(move.from(), null);
        place(move.to(), destinationPiece);
    }

    private static Piece createPromoted(PieceType type, Color color) {
        return switch (type) {
            case QUEEN -> new Queen(color);
            case ROOK -> new Rook(color);
            case BISHOP -> new Bishop(color);
            case KNIGHT -> new Knight(color);
            default -> throw new IllegalArgumentException(
                    "Invalid promotion type: " + type);
        };
    }

    public void undo(Move move) {// put `moved` back on `from`; put `captured` (or null) back on `to`
        place(move.from(), move.moved());
        place(move.to(), move.captured());
    }

    public Piece pieceAt(Position position) {
        return squares[position.file()][position.rank()];
    }

    public boolean isEmpty(Position position) {
        return pieceAt(position) == null;
    }

    public void place(Position position, Piece piece) {
        squares[position.file()][position.rank()] = piece;
    }

    public List<Position> positionsOf(Color color) {
        List<Position> positions = new ArrayList<>();

        for (int file = 0; file < 8; file++) {
            for (int rank = 0; rank < 8; rank++) {
                Piece piece = squares[file][rank];

                if (piece != null && piece.color() == color) {
                    positions.add(new Position(file, rank));
                }
            }
        }

        return positions;
    }

    @Override
    public String toString() {
        String result = "";

        for (int rank = 7; rank >= 0; rank--) {
            int emptyCount = 0;

            for (int file = 0; file < 8; file++) {
                Piece piece = squares[file][rank];

                if (piece == null) {
                    emptyCount++;
                } else {
                    if (emptyCount > 0) {
                        result += emptyCount;
                        emptyCount = 0;
                    }

                    result += piece.symbol();
                }
            }

            if (emptyCount > 0) {
                result += emptyCount;
            }

            if (rank > 0) {
                result += "/";
            }
        }

        return result;
    }

    //Returns the position of the given color's king
    public Position kingPosition(Color color) {
        for (Position position : positionsOf(color)) {
            Piece piece = pieceAt(position);

            if (piece.type() == PieceType.KING) {
                return position;
            }
        }

        return null;
    }

}