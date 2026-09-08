package model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class BoardTest {

    private Board board;

    @BeforeEach
    void setUp() {
        board = new Board();
    }

    /** Joue une suite de coups {ligne, colonne}, Board alterne lui-meme les joueurs. */
    private void play(int[]... moves) {
        for (int[] m : moves) {
            board.mark(m[0], m[1]);
        }
    }

    @Test
    @DisplayName("Nouvelle partie : X commence, pas de gagnant, partie en cours")
    void newBoardIsInProgressWithXToPlay() {
        assertEquals(Player.X, board.getCurrentTurn());
        assertNull(board.getWinner());
        assertTrue(board.isInProgressMode());
        assertFalse(board.isInFinishedMode());
    }

    @Test
    @DisplayName("restart() en cours de partie remet X, vide la grille et l'etat")
    void restartMidGameResetsEverything() {
        play(new int[]{0, 0}, new int[]{1, 1});          // X puis O
        assertEquals(Player.X, board.getCurrentTurn());

        board.restart();

        assertEquals(Player.X, board.getCurrentTurn());
        assertNull(board.getWinner());
        assertTrue(board.isInProgressMode());

        // la case (0,0) est de nouveau libre : le coup est accepte donc le tour passe a O
        board.mark(0, 0);
        assertEquals(Player.O, board.getCurrentTurn());
    }

    @Test
    @DisplayName("restart() apres une victoire efface le gagnant et l'etat FINISHED")
    void restartAfterWinClearsWinner() {
        play(new int[]{0, 0}, new int[]{1, 0}, new int[]{0, 1}, new int[]{1, 1}, new int[]{0, 2}); // X gagne ligne 0
        assertEquals(Player.X, board.getWinner());

        board.restart();

        assertNull(board.getWinner());
        assertTrue(board.isInProgressMode());
        assertEquals(Player.X, board.getCurrentTurn());
    }

    @Test
    @DisplayName("restart() remet X meme si le tour avait ete force a O")
    void restartResetsTurnToXEvenAfterSetCurrentTurn() {
        board.setCurrentTurn(Player.O);
        board.restart();
        assertEquals(Player.X, board.getCurrentTurn());
    }


    @Test
    @DisplayName("Apres un coup valide de X, c'est a O de jouer")
    void turnFlipsToOAfterFirstMove() {
        board.mark(0, 0);
        assertEquals(Player.O, board.getCurrentTurn());
    }

    @Test
    @DisplayName("Les joueurs alternent a chaque coup valide")
    void turnsAlternate() {
        board.mark(0, 0);
        assertEquals(Player.O, board.getCurrentTurn());
        board.mark(1, 1);
        assertEquals(Player.X, board.getCurrentTurn());
        board.mark(2, 2);
        assertEquals(Player.O, board.getCurrentTurn());
    }

    @Test
    @DisplayName("setCurrentTurn(O) permet a O de jouer le premier coup")
    void setCurrentTurnIsTakenIntoAccount() {
        board.setCurrentTurn(Player.O);
        board.mark(0, 0);
        assertEquals(Player.X, board.getCurrentTurn());
    }

    @ParameterizedTest(name = "mark({0}, 0) est refuse sans exception")
    @ValueSource(ints = {-1, 3, -100, 100})
    void rowOutOfBoundsIsIgnored(int row) {
        assertDoesNotThrow(() -> board.mark(row, 0));
        assertEquals(Player.X, board.getCurrentTurn());
        assertTrue(board.isInProgressMode());
        assertNull(board.getWinner());
    }

    @ParameterizedTest(name = "mark(0, {0}) est refuse sans exception")
    @ValueSource(ints = {-1, 3, -100, 100})
    void colOutOfBoundsIsIgnored(int col) {
        assertDoesNotThrow(() -> board.mark(0, col));
        assertEquals(Player.X, board.getCurrentTurn());
        assertTrue(board.isInProgressMode());
        assertNull(board.getWinner());
    }

    @Test
    @DisplayName("Ligne et colonne toutes deux hors grille : refuse")
    void bothIndicesOutOfBoundsIsIgnored() {
        assertDoesNotThrow(() -> board.mark(-1, 3));
        assertEquals(Player.X, board.getCurrentTurn());
    }

    @ParameterizedTest(name = "l''indice borne {0} est accepte")
    @ValueSource(ints = {0, 2})
    void boundaryIndicesAreAccepted(int idx) {
        board.mark(idx, idx);
        assertEquals(Player.O, board.getCurrentTurn());
    }

    @Test
    @DisplayName("Les 4 coins et le centre sont jouables")
    void cornersAndCenterArePlayable() {
        int[][] cells = {{0, 0}, {0, 2}, {2, 0}, {2, 2}, {1, 1}};
        Player expected = Player.O;
        for (int[] c : cells) {
            board.mark(c[0], c[1]);
            assertEquals(expected, board.getCurrentTurn(), "apres " + c[0] + "," + c[1]);
            expected = expected == Player.X ? Player.O : Player.X;
        }
    }


    @Test
    @DisplayName("Jouer sur une case occupee ne change pas le joueur actif")
    void markingOccupiedCellIsIgnored() {
        board.mark(1, 1);                       // X
        assertEquals(Player.O, board.getCurrentTurn());
        board.mark(1, 1);                       // O sur la meme case : refuse
        assertEquals(Player.O, board.getCurrentTurn());
        assertTrue(board.isInProgressMode());
    }

    @Test
    @DisplayName("La case occupee garde son symbole d'origine (verifie via une victoire en diagonale)")
    void occupiedCellKeepsOriginalValue() {
        board.mark(1, 1);   // X au centre
        board.mark(1, 1);   // O tente le centre : refuse, le centre doit rester X
        board.mark(2, 0);   // O
        board.mark(0, 0);   // X
        board.mark(2, 1);   // O
        board.mark(2, 2);   // X -> diagonale (0,0)(1,1)(2,2) seulement si (1,1) est toujours X
        assertEquals(Player.X, board.getWinner());
        assertTrue(board.isInFinishedMode());
    }

    @ParameterizedTest(name = "X gagne sur la ligne {0}")
    @ValueSource(ints = {0, 1, 2})
    void xWinsOnRow(int row) {
        int other = (row + 1) % 3;
        play(new int[]{row, 0}, new int[]{other, 0},
             new int[]{row, 1}, new int[]{other, 1},
             new int[]{row, 2});
        assertEquals(Player.X, board.getWinner());
        assertTrue(board.isInFinishedMode());
        assertFalse(board.isInProgressMode());
    }

    @ParameterizedTest(name = "X gagne sur la colonne {0}")
    @ValueSource(ints = {0, 1, 2})
    void xWinsOnColumn(int col) {
        int other = (col + 1) % 3;
        play(new int[]{0, col}, new int[]{0, other},
             new int[]{1, col}, new int[]{1, other},
             new int[]{2, col});
        assertEquals(Player.X, board.getWinner());
        assertTrue(board.isInFinishedMode());
    }

    @Test
    @DisplayName("X gagne sur la diagonale principale")
    void xWinsOnDiagonal() {
        play(new int[]{0, 0}, new int[]{0, 1},
             new int[]{1, 1}, new int[]{0, 2},
             new int[]{2, 2});
        assertEquals(Player.X, board.getWinner());
        assertTrue(board.isInFinishedMode());
    }

    @Test
    @DisplayName("X gagne sur l'anti-diagonale")
    void xWinsOnAntiDiagonal() {
        play(new int[]{0, 2}, new int[]{0, 0},
             new int[]{1, 1}, new int[]{0, 1},
             new int[]{2, 0});
        assertEquals(Player.X, board.getWinner());
        assertTrue(board.isInFinishedMode());
    }

    @Test
    @DisplayName("O gagne sur une colonne")
    void oWinsOnColumn() {
        play(new int[]{0, 0}, new int[]{0, 1},
             new int[]{1, 0}, new int[]{1, 1},
             new int[]{2, 2}, new int[]{2, 1});   // O : colonne 1
        assertEquals(Player.O, board.getWinner());
        assertTrue(board.isInFinishedMode());
    }

    @Test
    @DisplayName("O gagne sur la diagonale principale")
    void oWinsOnDiagonal() {
        play(new int[]{0, 1}, new int[]{0, 0},
             new int[]{0, 2}, new int[]{1, 1},
             new int[]{1, 0}, new int[]{2, 2});   // O : (0,0)(1,1)(2,2)
        assertEquals(Player.O, board.getWinner());
    }

    @Test
    @DisplayName("Victoire au 5eme coup (la plus rapide possible)")
    void winOnFifthMove() {
        play(new int[]{0, 0}, new int[]{1, 0},
             new int[]{0, 1}, new int[]{1, 1});
        assertTrue(board.isInProgressMode());
        assertNull(board.getWinner());
        board.mark(0, 2);
        assertEquals(Player.X, board.getWinner());
    }

    @Test
    @DisplayName("Victoire au 9eme coup : grille pleine avec alignement = victoire, pas nul")
    void winOnLastMove() {
        play(new int[]{0, 0}, new int[]{0, 1},
             new int[]{0, 2}, new int[]{1, 0},
             new int[]{1, 1}, new int[]{1, 2},
             new int[]{2, 1}, new int[]{2, 0});
        assertTrue(board.isInProgressMode());
        assertNull(board.getWinner());
        board.mark(2, 2);   // X complete la diagonale (0,0)(1,1)(2,2)
        assertEquals(Player.X, board.getWinner());
        assertTrue(board.isInFinishedMode());
    }

    @Test
    @DisplayName("Trois symboles differents alignes (X O X) ne font pas de victoire")
    void mixedLineIsNotAWin() {
        play(new int[]{0, 0}, new int[]{0, 1}, new int[]{0, 2});   // X O X sur la ligne 0
        assertNull(board.getWinner());
        assertTrue(board.isInProgressMode());
    }

    @Test
    @DisplayName("Deux symboles alignes seulement ne font pas de victoire")
    void twoInARowIsNotAWin() {
        play(new int[]{0, 0}, new int[]{1, 0}, new int[]{0, 1});
        assertNull(board.getWinner());
        assertTrue(board.isInProgressMode());
    }

    @Test
    @DisplayName("Apres la victoire, le tour ne change pas : le joueur actif reste le gagnant")
    void turnIsNotFlippedAfterWin() {
        play(new int[]{0, 0}, new int[]{1, 0}, new int[]{0, 1}, new int[]{1, 1}, new int[]{0, 2});
        assertEquals(Player.X, board.getCurrentTurn());
    }


    @Test
    @DisplayName("Un coup apres la fin de partie est ignore")
    void markAfterGameOverIsIgnored() {
        play(new int[]{0, 0}, new int[]{1, 0}, new int[]{0, 1}, new int[]{1, 1}, new int[]{0, 2});
        assertEquals(Player.X, board.getWinner());

        board.mark(2, 2);   // case libre, mais partie finie

        assertEquals(Player.X, board.getWinner());
        assertEquals(Player.X, board.getCurrentTurn());
        assertTrue(board.isInFinishedMode());
    }

    @Test
    @DisplayName("Le perdant ne peut pas prendre la victoire apres la fin")
    void loserCannotWinAfterGameOver() {
        // X gagne ligne 0 ; O a (1,0) et (1,1), il lui manque (1,2)
        play(new int[]{0, 0}, new int[]{1, 0}, new int[]{0, 1}, new int[]{1, 1}, new int[]{0, 2});
        board.mark(1, 2);   // O tenterait de completer la ligne 1
        assertEquals(Player.X, board.getWinner());
    }


    @Test
    @DisplayName("Grille pleine sans alignement : pas de gagnant")
    void fullBoardWithoutLineHasNoWinner() {
        play(new int[]{0, 0}, new int[]{0, 1}, new int[]{0, 2},
             new int[]{1, 1}, new int[]{1, 0}, new int[]{1, 2},
             new int[]{2, 1}, new int[]{2, 0}, new int[]{2, 2});
        // grille : X O X / X O O / O X X
        assertNull(board.getWinner());
    }

    @Test
    @DisplayName("Grille pleine sans alignement : la partie devrait etre terminee (match nul)")
    void fullBoardWithoutLineShouldBeFinished() {
        play(new int[]{0, 0}, new int[]{0, 1}, new int[]{0, 2},
             new int[]{1, 1}, new int[]{1, 0}, new int[]{1, 2},
             new int[]{2, 1}, new int[]{2, 0}, new int[]{2, 2});
        // Match nul : grille pleine sans alignement -> partie terminee, winner = null.
        // Echouait avec le code Moodle d'origine (pas de detection du nul),
        // passe depuis l'ajout de isBoardFull() dans Board.mark().
        assertTrue(board.isInFinishedMode());
    }
}
