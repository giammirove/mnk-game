/*
 *  Copyright (C) 2021 Pietro Di Lena
 *  
 *  This file is part of the MNKGame v2.0 software developed for the
 *  students of the course "Algoritmi e Strutture di Dati" first 
 *  cycle degree/bachelor in Computer Science, University of Bologna
 *  A.Y. 2020-2021.
 *
 *  MNKGame is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This  is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this file.  If not, see <https://www.gnu.org/licenses/>.
 */

/*
	Applicazione dell'algoritmo descritto qua
	http://www.micsymposium.org/mics2016/Papers/MICS_2016_paper_28.pdf
*/

/*


*/

package mnkgame;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;

/**
 * Totally random software player.
 */
public class MctsPlayer implements MNKPlayer {
	private Random rand;
	private int TIMEOUT;
	protected MNKCellState[][] B;
	private int M, N, K;

	private Node mcts;

	private MNKGameState vincoIo;
	private MNKGameState vinceNemico;
	private MNKCellState mioStato;
	private MNKCellState nemicoStato;

	private int mioPunteggio;
	private int nemicoPunteggio;
	private Boolean[] win_table_mia;
	private Boolean[] win_table_nemica;

	private Boolean primoAgiocare;

	/**
	 * Default empty constructor
	 */
	public MctsPlayer() {
	}

	public void initPlayer(int M, int N, int K, boolean first, int timeout_in_secs) {
		// New random seed for each game
		rand = new Random(System.currentTimeMillis());
		// Save the timeout for testing purposes
		TIMEOUT = timeout_in_secs;

		B = new MNKCellState[M][N];
		this.M = M;
		this.N = N;
		this.K = K;

		primoAgiocare = first;

		mioPunteggio = 0;
		nemicoPunteggio = 0;
		win_table_mia = new Boolean[K + 1];
		win_table_nemica = new Boolean[K + 1];
		for (int i = 0; i < K + 1; i++) {
			win_table_mia[i] = false;
			win_table_nemica[i] = false;
		}
		for (int i = 0; i < M; i++) {
			for (int j = 0; j < N; j++) {
				B[i][j] = MNKCellState.FREE;
			}
		}

		vincoIo = first ? MNKGameState.WINP1 : MNKGameState.WINP2;
		vinceNemico = first ? MNKGameState.WINP2 : MNKGameState.WINP1;

		mioStato = first ? MNKCellState.P1 : MNKCellState.P2;
		nemicoStato = first ? MNKCellState.P2 : MNKCellState.P1;

		// Uncomment to chech the initialization timeout
		/*
		 * try { Thread.sleep(1000*2*TIMEOUT); } catch(Exception e) { }
		 */
	}

	/**
	 * Selects a random cell in <code>FC</code>
	 */
	public MNKCell selectCell(MNKCell[] FC, MNKCell[] MC) {
		// Uncomment to chech the move timeout
		/*
		 * try { Thread.sleep(1000*2*TIMEOUT); } catch(Exception e) { }
		 */

		// se è disponibile solo una mossa allora sicuramente farò quella
		if (FC.length == 1)
			return FC[0];

		// prima mossa causale
		/*
		if (MC.length == 0) {
			MNKCell c = FC[rand.nextInt(FC.length)];
			B[c.i][c.j] = mioStato;
			return c;
		}
		*/

		if (MC.length > 0) {
			// prendo la mossa precedente
			B[MC[MC.length - 1].i][MC[MC.length - 1].j] = nemicoStato;
		}

		mcts = new Node(M, N, K, mioStato, FC, MC, B);

		// showMap();
		int prev = -1;
		while (mcts.getSimulationCount() < 15000 && prev != mcts.getSimulationCount()) {
			prev = mcts.getSimulationCount();
			mcts.Selection();
		}

		MNKCell cell = mcts.BestMove();
		B[cell.i][cell.j] = mioStato;

		return cell;
	}

	private boolean isWinningCell(int i, int j, MNKCellState player) {
		MNKCellState s = player;
		int n;

		// Useless pedantic check
		if (s == MNKCellState.FREE)
			return false;

		// Horizontal check
		n = 1;
		for (int k = 1; j - k >= 0 && B[i][j - k] == s; k++)
			n++; // backward check
		for (int k = 1; j + k < N && B[i][j + k] == s; k++)
			n++; // forward check
		if (n >= 2) {
			if (player == mioStato) {
				for (int k = 2; k <= K; k++) {
					if (n >= k)
						win_table_mia[k] = true;
					else
						win_table_mia[k] = false;
				}
			} else {
				for (int k = 2; k <= K; k++) {
					if (n >= k)
						win_table_nemica[k] = true;
					else
						win_table_nemica[k] = false;
				}
			}
			if (n >= K)
				return true;
		}

		// Vertical check
		n = 1;
		for (int k = 1; i - k >= 0 && B[i - k][j] == s; k++)
			n++; // backward check
		for (int k = 1; i + k < M && B[i + k][j] == s; k++)
			n++; // forward check
		if (n >= 2) {
			if (player == mioStato) {
				for (int k = 2; k <= K; k++) {
					if (n >= k)
						win_table_mia[k] = true;
					else
						win_table_mia[k] = false;
				}
			} else {
				for (int k = 2; k <= K; k++) {
					if (n >= k)
						win_table_nemica[k] = true;
					else
						win_table_nemica[k] = false;
				}
			}
			if (n >= K)
				return true;
		}

		// Diagonal check
		n = 1;
		for (int k = 1; i - k >= 0 && j - k >= 0 && B[i - k][j - k] == s; k++)
			n++; // backward check
		for (int k = 1; i + k < M && j + k < N && B[i + k][j + k] == s; k++)
			n++; // forward check
		if (n >= 2) {
			if (player == mioStato) {
				for (int k = 2; k <= K; k++) {
					if (n >= k)
						win_table_mia[k] = true;
					else
						win_table_mia[k] = false;
				}
			} else {
				for (int k = 2; k <= K; k++) {
					if (n >= k)
						win_table_nemica[k] = true;
					else
						win_table_nemica[k] = false;
				}
			}
			if (n >= K)
				return true;
		}

		// Anti-diagonal check
		n = 1;
		for (int k = 1; i - k >= 0 && j + k < N && B[i - k][j + k] == s; k++)
			n++; // backward check
		for (int k = 1; i + k < N && j - k >= 0 && B[i + k][j - k] == s; k++)
			n++; // backward check
		if (n >= 2) {
			if (player == mioStato) {
				for (int k = 2; k <= K; k++) {
					if (n >= k)
						win_table_mia[k] = true;
					else
						win_table_mia[k] = false;
				}
			} else {
				for (int k = 2; k <= K; k++) {
					if (n >= k)
						win_table_nemica[k] = true;
					else
						win_table_nemica[k] = false;
				}
			}
			if (n >= K)
				return true;
		}

		win_table_mia[K] = false;
		win_table_nemica[K] = false;
		return false;
	}

	private boolean isWinningCell(int i, int j, MNKCellState player, int K_TEMP) {
		MNKCellState s = player;
		int n;

		// Useless pedantic check
		if (s == MNKCellState.FREE)
			return false;

		// Horizontal check
		n = 1;
		for (int k = 1; j - k >= 0 && B[i][j - k] == s; k++)
			n++; // backward check
		for (int k = 1; j + k < N && B[i][j + k] == s; k++)
			n++; // forward check
		if (n >= K_TEMP)
			return true;

		// Vertical check
		n = 1;
		for (int k = 1; i - k >= 0 && B[i - k][j] == s; k++)
			n++; // backward check
		for (int k = 1; i + k < M && B[i + k][j] == s; k++)
			n++; // forward check
		if (n >= K_TEMP)
			return true;

		// Diagonal check
		n = 1;
		for (int k = 1; i - k >= 0 && j - k >= 0 && B[i - k][j - k] == s; k++)
			n++; // backward check
		for (int k = 1; i + k < M && j + k < N && B[i + k][j + k] == s; k++)
			n++; // forward check
		if (n >= K_TEMP)
			return true;

		// Anti-diagonal check
		n = 1;
		for (int k = 1; i - k >= 0 && j + k < N && B[i - k][j + k] == s; k++)
			n++; // backward check
		for (int k = 1; i + k < N && j - k >= 0 && B[i + k][j - k] == s; k++)
			n++; // backward check
		if (n >= K_TEMP)
			return true;

		return false;
	}

	private boolean isWinningCell(int i, int j) {
		MNKCellState s = B[i][j];
		int n;

		// Useless pedantic check
		if (s == MNKCellState.FREE || s == null)
			return false;

		// Horizontal check
		n = 1;
		for (int k = 1; j - k >= 0 && B[i][j - k] == s; k++)
			n++; // backward check
		for (int k = 1; j + k < N && B[i][j + k] == s; k++)
			n++; // forward check
		if (n >= K)
			return true;

		// Vertical check
		n = 1;
		for (int k = 1; i - k >= 0 && B[i - k][j] == s; k++)
			n++; // backward check
		for (int k = 1; i + k < M && B[i + k][j] == s; k++)
			n++; // forward check
		if (n >= K)
			return true;

		// Diagonal check
		n = 1;
		for (int k = 1; i - k >= 0 && j - k >= 0 && B[i - k][j - k] == s; k++)
			n++; // backward check
		for (int k = 1; i + k < M && j + k < N && B[i + k][j + k] == s; k++)
			n++; // forward check
		if (n >= K)
			return true;

		// Anti-diagonal check
		n = 1;
		for (int k = 1; i - k >= 0 && j + k < N && B[i - k][j + k] == s; k++)
			n++; // backward check
		for (int k = 1; i + k < M && j - k >= 0 && B[i + k][j - k] == s; k++)
			n++; // backward check
		if (n >= K)
			return true;

		return false;
	}

	private void print(String msg) {
		System.out.print(msg);
	}

	private void showMap() {
		for (int i = 0; i < M; i++) {
			for (int j = 0; j < N; j++) {
				System.out.print("[");
				if (B[i][j] == MNKCellState.P1)
					System.out.print("X");
				else if (B[i][j] == MNKCellState.P2)
					System.out.print("O");
				else
					System.out.print(" ");
				System.out.print("] ");
			}
			System.out.print("\n");
		}
		System.out.print("\n");
	}

	public String playerName() {
		return "MctsPlayer";
	}
}
