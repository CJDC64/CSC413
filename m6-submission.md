1. Who decides, who shows. Checkmate is detected in one class and
   announced in another. Name both, and say which of session 16's
   three questions puts each one there.
    MoveGenerator.status() detects checkmate by checking 
    if the side to move is in check and has any legal moves
    GameController.handle() asks Game for the result and
    sends the announcement through view.showMessage()

2. The dependency rule. Which of your classes import
   edu.sfsu.csc413.chess.view? Which must not, and why?
   In my code, the imported classes from
   edu.sfsu.csc413.chess.view are to create the view and
   controller. GameController uses BoardView from its
   own package, so it does not need an import for it.
   Classes in engine, model, and factory must not import
   view classes because the chess rules and game data should
   work independently of how the game is displayed or controlled.

3. Test 2 on your engine. In M7 you add a Swing view.
   List the files you expect to change. If your list includes GameController
   or anything in engine, say what in your design makes that necessary.
   For M7, I expect to add SwingView.java and update Main.java to select the
   new view. SwingView will implement BoardView and send button actions
   to GameController.handle() instead of calling runLoop(). GameController
   and the engine should not need changes because the controller already
   communicates through BoardView and PlayerAction.