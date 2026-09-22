package edu.sfsu.csc413.chess.model;

public enum PieceType {
    PAWN('P'),
    ROOK('R'),
    KNIGHT('N'),
    BISHOP('B'),
    QUEEN('Q'),
    KING('K');

    private final char symbol;

    private PieceType(char symbol) {
        this.symbol = symbol;
    }

    public char symbol() {
        return symbol;
    }

    public static PieceType fromSymbol(char symbol) {
        char upperLetter = Character.toUpperCase(symbol);

        for (PieceType pieceType : PieceType.values()) {
            if (pieceType.symbol == upperLetter) {
                return pieceType;
            }
        }
        throw new IllegalArgumentException("Invalid piece symbol: " + symbol);
    }
}