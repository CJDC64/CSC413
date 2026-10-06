package edu.sfsu.csc413.chess;

import edu.sfsu.csc413.chess.model.Board;
import edu.sfsu.csc413.chess.view.PieceGlyphs;
import edu.sfsu.csc413.chess.view.TextBoardRenderer;
import edu.sfsu.csc413.chess.factory.BoardFactory;
import edu.sfsu.csc413.chess.engine.Game;

import java.util.List;

/**
 * Entry point.
 *
 * <p>At M0 this does nothing but prove the toolchain works. It grows into the
 * real launcher as the engine appears underneath it.
 */
public final class Main {

    public static void main(String[] args) {
//        System.out.println("CSC 413 Chess — environment OK.");
//
//        Board board = BoardFactory.standard();
//
//        System.out.println(new TextBoardRenderer(PieceGlyphs.LETTERS).render(board));

        Game game = new Game();
        TextBoardRenderer renderer = new TextBoardRenderer(PieceGlyphs.LETTERS);
        System.out.println(renderer.render(game.board()));

        for (String notation : List.of("e2e4", "e7e5")) {
            game.play(game.findLegalMove(notation).orElseThrow());
        }
        System.out.println(renderer.render(game.board()));

        game.undoLastMove();
        System.out.println(renderer.render(game.board()));
    }

    private Main() {
    }
}
