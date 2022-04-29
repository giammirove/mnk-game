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
	ROBA DA SAPERE

	ci sono due tipi di comportamento
	uno è aggressivo, ovvero avanzare in maniera decisa verso la vittoria
	uno è difensivo, ovvero impedire all'avversario di arrivare alla vittoria

	solitamente chi inizio per primo avrà un comportamento aggressivo, 
	bisogna stare attenti però perchè durante la partita la situazione 
	può cambiare e posso ritrovarmi a dover cambiare tattica passando
	ad un comportamento difensivo
	se iniziamo per secondi, la scelta migliore è assumere un comportamento
	difensivo, e passare a quello aggressivo solo in caso di mossa sbagliata
	dell'avversario

	possiamo verificare chi si trova in vantaggio tramite una tabella dei punteggi
	o tramite un singolo valore di punteggio che incrementiamo ogni volta che 
	ottieniamo una sequenza

	più la sequenza è lunga maggiori sono i punti che ottengo, ma se ad esempio ho 
	una sequenza lunga 3 e l'obiettivo è farla di 4, e quest'ultima è bloccata, sarà
	considerata come nulla

*/

package mnkgame;

import java.util.Random;

/**
 * Totally random software player.
 */
public class MontagnolaPlayer2 implements MNKPlayer {
	private Random rand;
	private int TIMEOUT;
	protected MNKCellState[][] B;
	private int M, N, K;

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
	public MontagnolaPlayer2() {
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
		if (MC.length == 0) {
			MNKCell c = FC[rand.nextInt(FC.length)];
			B[c.i][c.j] = mioStato;
			return c;
		}

		if (MC.length > 0) {
			// prendo la mossa precedente
			B[MC[MC.length - 1].i][MC[MC.length - 1].j] = nemicoStato;
		}

		int[][] map = mappa_vittorie();
		// print("Mio Punteggio : " + mioPunteggio + "\n");
		// print("Nem Punteggio : " + nemicoPunteggio + "\n");

		MNKCell cell = FC[0];
		int val = map[FC[0].i][FC[0].j];
		int dist = 10000;
		for (int k = 1; k < FC.length; k++) {
			if (map[FC[k].i][FC[k].j] == val) {
				// vuol dire che non è la mia prima mossa
				if (MC.length >= 2) {
					if (distanza(FC[k], MC[MC.length - 2]) < distanza(cell, MC[MC.length - 2])) {
						val = map[FC[k].i][FC[k].j];
						cell = FC[k];
					}
				}
			}
			if (map[FC[k].i][FC[k].j] > val) {
				val = map[FC[k].i][FC[k].j];
				cell = FC[k];
			}
		}

		// scelta casuale
		// MNKCell cell = FC[rand.nextInt(FC.length)];
		B[cell.i][cell.j] = mioStato;

		if (isWinningCell(cell.i, cell.j)) {
			show_mappa_vittorie(map);
			showMap();
			print("Mio Punteggio : " + mioPunteggio + "\n");
			print("Nem Punteggio : " + nemicoPunteggio + "\n");

		}

		// showMap();

		return cell;
	}

	double distanza(MNKCell c1, MNKCell c2) {
		return Math.sqrt((double) (Math.pow(c1.i - c2.i, 2) + Math.pow(c1.j - c2.j, 2)));
	}

	int[][] mappa_vittorie() {
		int[][] mappa = new int[M][N];
		for (int i = 0; i < M; i++) {
			for (int j = 0; j < N; j++) {
				if (B[i][j] == MNKCellState.FREE)
					mappa[i][j] = count(i, j);
				else {
					// mappa[i][j] = count(i, j);
					mappa[i][j] = 0;
					int c = count(i, j);
					if (B[i][j] == mioStato) {
						mioPunteggio += c;
					} else if (B[i][j] == nemicoStato) {
						nemicoPunteggio += c;
					}
				}
				// print("|" + mappa[i][j] + "| ");
			}
			// print("\n");
		}
		// print("\n");
		return mappa;
	}

	void show_mappa_vittorie(int[][] mappa) {
		for (int i = 0; i < M; i++) {
			for (int j = 0; j < N; j++) {
				print("|" + mappa[i][j] + "| ");
			}
			print("\n");
		}
		print("\n");
	}

	int count(int i, int j) {
		for (int k = 0; k < K + 1; k++) {
			win_table_mia[k] = false;
			win_table_nemica[k] = false;
		}
		// countMieMosse(i, j) + countMosseAvversarie(i, j);
		if (isWinningCell(i, j, mioStato) || isWinningCell(i, j, nemicoStato))
			return 100000;

		int c = 0;
		double perc_mio = 2 / 3;
		double perc_nemico = 1 / 3;
		if (mioPunteggio <= nemicoPunteggio) {
			perc_mio = 1 / 3;
			perc_mio = 2 / 3;
		}

		c = (int) (countMieMosse(i, j) * perc_mio) + (int) (countMosseAvversarie(i, j) * perc_nemico);

		int inc_mio = 10;
		int inc_nemico = 5;
		if (primoAgiocare == false || mioPunteggio <= nemicoPunteggio)
			inc_nemico = inc_mio + 1;
		Boolean done = false;
		for (int k = K - 1; k > 1; k--) {
			if (win_table_mia[k]) {
				c += Math.pow(inc_mio, k);
				done = true;
			}
			if (win_table_nemica[k]) {
				c += Math.pow(inc_nemico, k);
				done = true;
			}
		}
		return c;
	}

	int countGenerico(int i, int j, MNKCellState stato) {

		int c = 0;
		int n = 0;

		// Diagonal check
		n = 1;
		for (int k = 1; i - k >= 0 && j - k >= 0
				&& (B[i - k][j - k] == stato || B[i - k][j - k] == MNKCellState.FREE); k++)
			n++; // backward check
		for (int k = 1; i + k < M && j + k < N
				&& (B[i + k][j + k] == stato || B[i + k][j + k] == MNKCellState.FREE); k++)
			n++; // forward check
		if (n >= K) {
			c += (int) (n / (K - 1));
		}

		// Anti-diagonal check
		n = 1;
		for (int k = 1; i - k >= 0 && j + k < N
				&& (B[i - k][j + k] == stato || B[i - k][j + k] == MNKCellState.FREE); k++)
			n++; // backward check
		for (int k = 1; i + k < N && j - k >= 0
				&& (B[i + k][j - k] == stato || B[i + k][j - k] == MNKCellState.FREE); k++)
			n++; // backward check
		if (n >= K) {
			c += (int) (n / (K - 1));
		}

		// Horizontal check
		n = 1;
		for (int k = 1; j - k >= 0 && (B[i][j - k] == stato || B[i][j - k] == MNKCellState.FREE); k++)
			n++; // backward check
		for (int k = 1; j + k < N && (B[i][j + k] == stato || B[i][j + k] == MNKCellState.FREE); k++)
			n++; // forward check
		if (n >= K) {
			c += (int) (n / (K - 1));
		}

		// Vertical check
		n = 1;
		for (int k = 1; i - k >= 0 && (B[i - k][j] == stato || B[i - k][j] == MNKCellState.FREE); k++)
			n++; // backward check
		for (int k = 1; i + k < M && (B[i + k][j] == stato || B[i + k][j] == MNKCellState.FREE); k++)
			n++; // forward check
		if (n >= K) {
			c += (int) (n / (K - 1));
		}

		return c;
	}

	int countMieMosse(int i, int j) {

		int c = 0;
		int n = 0;

		// Diagonal check
		n = 1;
		for (int k = 1; i - k >= 0 && j - k >= 0 && B[i - k][j - k] != nemicoStato; k++)
			n++; // backward check
		for (int k = 1; i + k < M && j + k < N && (B[i + k][j + k] != nemicoStato); k++)
			n++; // forward check
		if (n >= K) {
			c += (int) (n / (K - 1));
		}

		// Anti-diagonal check
		n = 1;
		for (int k = 1; i - k >= 0 && j + k < N && (B[i - k][j + k] != nemicoStato); k++)
			n++; // backward check
		for (int k = 1; i + k < N && j - k >= 0 && (B[i + k][j - k] != nemicoStato); k++)
			n++; // backward check
		if (n >= K) {
			c += (int) (n / (K - 1));
		}

		// Horizontal check
		n = 1;
		for (int k = 1; j - k >= 0 && (B[i][j - k] != nemicoStato); k++)
			n++; // backward check
		for (int k = 1; j + k < N && (B[i][j + k] != nemicoStato); k++)
			n++; // forward check
		if (n >= K) {
			c += (int) (n / (K - 1));
		}

		// Vertical check
		n = 1;
		for (int k = 1; i - k >= 0 && (B[i - k][j] != nemicoStato); k++)
			n++; // backward check
		for (int k = 1; i + k < M && (B[i + k][j] != nemicoStato); k++)
			n++; // forward check
		if (n >= K) {
			c += (int) (n / (K - 1));
		}

		return c;
	}

	int countMosseAvversarie(int i, int j) {

		int c = 0;
		int n = 0;

		// Diagonal check
		n = 1;
		for (int k = 1; i - k >= 0 && j - k >= 0 && B[i - k][j - k] != mioStato; k++)
			n++; // backward check
		for (int k = 1; i + k < M && j + k < N && (B[i + k][j + k] != mioStato); k++)
			n++; // forward check
		if (n >= K) {
			c += (int) (n / (K - 1));
		}

		// Anti-diagonal check
		n = 1;
		for (int k = 1; i - k >= 0 && j + k < N && (B[i - k][j + k] != mioStato); k++)
			n++; // backward check
		for (int k = 1; i + k < N && j - k >= 0 && (B[i + k][j - k] != mioStato); k++)
			n++; // backward check
		if (n >= K) {
			c += (int) (n / (K - 1));
		}

		// Horizontal check
		n = 1;
		for (int k = 1; j - k >= 0 && (B[i][j - k] != mioStato); k++)
			n++; // backward check
		for (int k = 1; j + k < N && (B[i][j + k] != mioStato); k++)
			n++; // forward check
		if (n >= K) {
			c += (int) (n / (K - 1));
		}

		// Vertical check
		n = 1;
		for (int k = 1; i - k >= 0 && (B[i - k][j] != mioStato); k++)
			n++; // backward check
		for (int k = 1; i + k < M && (B[i + k][j] != mioStato); k++)
			n++; // forward check
		if (n >= K) {
			c += (int) (n / (K - 1));
		}

		return c;
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
		return "MontagnolaPlayer2";
	}
}
