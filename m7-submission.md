# M7 Submission

## 1. Group
Christopher John Dela Cruz — solo.

## 2. Tools and models
I used ChatGPT to generate and review the views, IntelliJ IDEA to
edit the project, and Git to preserve the different runs. I built
and tested the project with Maven and JDK 25.

The models were Sol for run 1, Luna for run 2, and Astra for run 3.
Runs 4 and 5 continued with Astra.

## 3. Implemented views
SwingView displays a clickable chessboard, legal destination
highlights, promotion choices, game status, and an Undo button.

Launch it with:

java -cp target/classes edu.sfsu.csc413.chess.Main swing

RotatingTerminalView displays the board from the perspective of
the side to move, including rotated coordinates. It supports typed
moves, undo, moves, and quit through the existing controller.

Launch it with:

java -cp target/classes edu.sfsu.csc413.chess.Main rotate

## 4. Generation runs
I preserved five generation runs:

| Run | Commit | Main difference |
| --- | --- | --- |
| 1 | 60857a8 | Initial Swing and rotating terminal views |
| 2 | 1496ec8 | Promotion choice dialog |
| 3 | 4906aa0 | Legal destination highlighting |
| 4 | 0d1d6a0 | Scrollable controller-message history |
| 5 | 57b8822 | Legal destination dots |

The requests shared instructions to implement BoardView, preserve
console and emoji options, use the existing controller, and leave
protected files unchanged.

The early runs changed both the model and parts of the request,
so they were not controlled comparisons of just one variable.
Runs 3–5 used Astra while changing the requested view behavior.

This section summarizes the requests; it does not reproduce the
complete generation prompts.

## 5. AGENTS.md instructions
One important instruction was to read state through Game and send
actions through GameController.handle() using PlayerAction.
This keeps the view responsible for presentation and input while
the existing engine remains responsible for chess rules.

The protected-file instructions also helped keep the generated
work focused on the new views instead of changing the existing
model, controller, interfaces, or tests.

## 6. Code review
These review locations are in
src/main/java/edu/sfsu/csc413/chess/view/SwingView.java.

### clickSquare()
I reviewed how the first click selects a piece and the second click
builds move notation. Promotion offers queen, rook, bishop, or knight.
The view sends a PlayerAction.Move instead of changing the board.

### refresh()
I reviewed how the view gets legal destinations from
game.legalMoves(). I kept this approach because the engine already
determines legality, so the view does not need its own chess rules.

### send()
I reviewed the call to controller.handle(action). Moves and Undo
go through the controller, preserving the existing MVC structure.

### refreshMoveHistory()
The added move-history display reads game.history(). This avoids
maintaining a separate list that could become inconsistent after
a rejected move or Undo.

These references identify methods rather than exact line numbers.

## 7. Comparison and selected run
I selected run 3 as the starting point for my final version. Its
piece labels were readable, it supported promotion choices, and
its destination highlighting made move selection clearer.

Run 4 added a useful scrollable controller-message history.
Run 5 used destination dots, but I observed some black pieces
becoming invisible on particular squares. This also happened
during move and Undo checks. I also observed an attempted illegal
destination that produced no message.

I did not determine the exact cause of the rendering problem.
Because of these observations, I retained run 3 instead of run 5.

## 8. Idea applied from another run
I adapted run 4's scrollable-history idea into a scrollable move
history panel in the selected version.

In SwingView.buildWindow(), the move-history text area is placed
inside a JScrollPane. refreshMoveHistory() rebuilds its contents
from game.history().

This is a move list, not the controller-message history from
run 4. Controller messages still appear separately and show the
most recent message. I used ChatGPT to review this adaptation.

## 9. Testing and debugging
The selected run-3 version passed all 83 existing tests with
0 failures and 0 errors using Maven and JDK 25.

I also launched the Swing application and manually checked its
behavior while comparing versions. These manual checks helped
me identify the piece-visibility problem in run 5.

I did not complete the IntelliJ debugger portion of the assignment.
I did not collect a breakpoint trace or debugger screenshots, and
I am not claiming that normal GUI testing replaces that requirement.

The expected move path is from SwingView.clickSquare() to send()
and then GameController.handle(). For e2–e4, I would expect the
controller to receive a PlayerAction.Move containing "e2e4",
update the board, change the side to move to Black, and add the
move to history. These are expectations from reading the code,
not results observed in the debugger.

## 10. By-hand change
Commit: b1080ce8783534341af726cebe9ea1b0d3777162

Commit message:
M7 by hand: change legal move highlight color

I changed SwingView's DESTINATION color from RGB (37, 116, 56)
to RGB (37, 56, 116). This changed legal destination borders from
green to blue.

I read the border-selection code in refresh() to understand how
DESTINATION is applied. The change affects presentation without
changing which moves are legal.

The larger view implementations were AI-generated. My separate
by-hand change was the destination highlight color.