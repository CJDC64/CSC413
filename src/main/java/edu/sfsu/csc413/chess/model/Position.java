package edu.sfsu.csc413.chess.model;

public record Position(int file, int rank) {

    /** Files and ranks both run 0..7. */
    public static final int BOARD_SIZE = 8;

    /** True when these raw coordinates name a real square. */
    public static boolean isOnBoard(int file, int rank) {
        return file >= 0 && file < BOARD_SIZE && rank >= 0 && rank < BOARD_SIZE;
    }

    public Position {
        if (!isOnBoard(file, rank)) {
            throw new IllegalArgumentException(
                    "Position off board: file=" + file + ", rank=" + rank);
        }
    }

    public static Position parse(String algebraic) {
        // Takes notation and returns a Position object.
        if (algebraic == null || algebraic.length() != 2) {
            throw new IllegalArgumentException("Invalid position: " + algebraic);
        }

        // Convert algebraic notation to file and rank
        char fileLetter = algebraic.charAt(0);
        char rankNumber = algebraic.charAt(1);

        // Turns fileLetter and rankNumber into Position coordinates
        int file = fileLetter - 'a';
        int rank = rankNumber - '1';

        // Validate the coordinates
        return new Position(file, rank);
    }   

    public Position offsetOrNull(int fileDelta, int rankDelta) {
        // Returns a new Position offset by the given deltas, or null if it is off the board.
        int newFile = file + fileDelta;
        int newRank = rank + rankDelta;

        // Check if the new position is on the board
        if (isOnBoard(newFile, newRank)) {
            return new Position(newFile, newRank);
        }

        return null;
    }
    
    @Override
    public String toString() {
        return "" + (char) ('a' + file) + (char) ('1' + rank);
    }
}
