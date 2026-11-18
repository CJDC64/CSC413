# Instructions for coding agents

This is a CSC413 chess engine built with MVC.
The model and controller are finished and were written by hand.
Your job is to add views for M7.

## Do

- Add new view classes in
  `src/main/java/edu/sfsu/csc413/chess/view/`
  or a package under it.
- Implement BoardView. Read BoardView, ConsoleView, PlayerAction,
  GameController, and Main before writing code.
- Read game state through Game: board(), sideToMove(), legalMoves(),
  status(), and history(). Treat the returned board as read-only.
- Send Swing actions through controller.handle(), using PlayerAction.
  For example: controller.handle(new PlayerAction.Move("e2e4")).
- The Undo button must send new PlayerAction.Undo().
- A terminal view may return PlayerAction objects from nextAction()
  and let controller.runLoop() handle them.
- Update the existing view-selection if/else in Main.main().
  There is no Main.chooseView() method.
- Preserve the existing console default and the "emoji" option.
- Make Swing available through the command-line word "swing".
- Use only the JDK.

## Do not

- Do not change files in model/, engine/, or factory/.
- Do not change BoardView, PlayerAction, GameController,
  ConsoleView, EmojiView, PieceGlyphs, or TextBoardRenderer.
- Do not change pom.xml or change or delete existing tests.
- Do not call game.play(), game.undoLastMove(), or mutate the board.
  Only the controller changes the game.
- Do not implement chess rules in a view.
  Legal moves come from game.legalMoves().
  Check, checkmate, and stalemate come from game.status().
- Do not wait for the next move inside a window.
  Swing start() builds or schedules the window and returns.
  It must never call controller.runLoop().
  A modal dialog asking one question, such as a promotion choice,
  is allowed.
- Do not add castling, en passant, or an AI opponent.

## Fit this project's lifecycle

- GameController.start() calls showMessage() and render(game)
  before view.start(controller). Handle those early calls safely,
  retaining the game and message until the window is ready.
- Create and update Swing components on the event dispatch thread.
- Keep status and controller messages readable. A render must not
  immediately erase an illegal-move message.
- The controller calls view.close() after checkmate or stalemate.
  Ensure the player can read the final result before disposal.
- Promotion must work. The default queen choice in BoardView is allowed.
- Every piece must be easy to distinguish on both square colours.

## Check your work

- Run ./mvnw test. All 83 existing tests must still pass.
  Additional view tests are allowed.
- Run git diff --stat submit-m6 HEAD and inspect uncommitted changes
  with git diff --stat and git status --short.
- Allowed M7 changes are new view files, new view tests, Main.java,
  AGENTS.md, optional CLAUDE.md, m7-submission.md, and
  docs/milestones/m7.md.
- Search new views for game.play and undoLastMove: no matches.
- Search the Swing view for runLoop: no matches.
- Report what you actually tested. Do not claim to have opened or
  played a GUI unless you did.
- Manual checks include Fool's Mate, an illegal move, Undo,
  promotion, piece visibility, and final-result visibility.

## Stop and ask

If a protected file seems to need changing, stop and explain why.
Do not modify it.