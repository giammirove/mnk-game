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

package mnkgame;

import java.lang.System;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ListIterator;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleToLongFunction;

import mnkgame.MNKCell;
import mnkgame.MNKCellState;

class HashStruct {

	double lowerbound;
	double upperbound;
	MNKCell cell;

	HashStruct() {
		lowerbound = Double.NEGATIVE_INFINITY;
		upperbound = Double.POSITIVE_INFINITY;
		cell = null;
	}
}

public class mtdPlayer implements MNKPlayer {
	private Random rand;
	private int TIMEOUT;
	private int MAX_DEPTH = 15;

	private int M, N, K;
	private int currentPlayer;
	private final MNKCellState[] Player = { MNKCellState.P1, MNKCellState.P2 };

	private MNKGameState vincoIo;
	private MNKGameState vinceNemico;
	private MNKCellState mioStato;
	private MNKCellState nemicoStato;

	private MNKCell selectedCell;
	protected MNKCellState[][] B;

	private HashMap<MNKCellState[][], HashStruct> alphaBetaMemory;

	/**
	 * Default empty constructor
	 */
	public mtdPlayer() {
	}

	public void initPlayer(int M, int N, int K, boolean first, int timeout_in_secs) {
		// New random seed for each game
		rand = new Random(System.currentTimeMillis());
		// Save the timeout for testing purposes
		TIMEOUT = timeout_in_secs;

		this.M = M;
		this.N = N;
		this.K = K;
		B = new MNKCellState[M][N];

		alphaBetaMemory = new HashMap<MNKCellState[][], HashStruct>((int) Math.pow(M * N, 1));

		currentPlayer = 0;

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
			// MNKCell c = FC[rand.nextInt(FC.length)];
			// B[c.i][c.j] = mioStato;
			// return c;
		}

		if (MC.length > 0) {
			// prendo la mossa precedente
			B[MC[MC.length - 1].i][MC[MC.length - 1].j] = nemicoStato;
		}

		// print("FREE: ");
		HashSet<MNKCell> free = new HashSet<MNKCell>((int) Math.ceil((FC.length) / 0.75));
		for (int i = 0; i < FC.length; i++) {
			free.add(FC[i]);
			// print(" (" + FC[i].i + "," + FC[i].j + ")");
			if (isWinningCell(FC[i].i, FC[i].j)) {
				// print("INSTANT WIN : " + FC[i].i + "," + FC[i].j);
				B[FC[i].i][FC[i].j] = mioStato;
				return FC[i];
			}
		}
		// print("\n");

		HashSet<MNKCell> marked = new HashSet<MNKCell>((int) Math.ceil((MC.length) / 0.75));
		for (int i = 0; i < MC.length; i++) {
			marked.add(MC[i]);
		}

		// showMap();
		// print("NUOVO TURNO\n");
		selectedCell = null;
		print("QUAA\n");
		// mtdf(0, MAX_DEPTH, free, marked);
		minimax(free, marked, null, MAX_DEPTH, true, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
		// print("SCORE : " + s + "\n");
		// print("CELL : " + selectedCell.i + " , " + selectedCell.j + "\n\n");

		B[selectedCell.i][selectedCell.j] = mioStato;
		return selectedCell;
	}

	public double iterivative_deeping(HashSet<MNKCell> free, HashSet<MNKCell> marked) {
		double firstguess = 0;
		for (int d = 1; d < MAX_DEPTH; d++) {
			firstguess = mtdf(firstguess, d, free, marked);
		}
		return firstguess;
	}

	public double mtdf(double f, int d, HashSet<MNKCell> free, HashSet<MNKCell> marked) {
		double g = f;
		double lowerbound = Double.NEGATIVE_INFINITY;
		double upperbound = Double.POSITIVE_INFINITY;
		double beta;

		do {
			if (g == lowerbound)
				beta = g + 1;
			else
				beta = g;

			g = minimax(free, marked, null, d, true, beta - 1, beta);

			if (g < beta) {
				upperbound = g;
			} else {
				lowerbound = g;
			}
		} while (lowerbound >= upperbound);

		return g;
	}

	public double minimax(HashSet<MNKCell> free, HashSet<MNKCell> marked, MNKCell lastMarked, int depth,
			Boolean mioturno, double alpha, double beta) {

		if (alphaBetaMemory.containsKey(B)) {
			HashStruct s = alphaBetaMemory.get(B);

			if (s.lowerbound >= beta && selectedCell != null) {
				print("RETURN : " + s.lowerbound + "\n");
				return s.lowerbound;
			}
			if (s.upperbound <= alpha && selectedCell != null) {
				print("RETURN : " + s.upperbound + "\n");
				return s.upperbound;
			}
			alpha = Math.max(alpha, s.lowerbound);
			beta = Math.min(beta, s.upperbound);
		}

		double g = 0;

		if (lastMarked != null) {
			// controllo che la mossa precedente sia la vincente
			if (isWinningCell(lastMarked.i, lastMarked.j)) {
				double e = eval(lastMarked, depth);
				B[lastMarked.i][lastMarked.j] = MNKCellState.FREE;
				return e * (free.size()+1);
				/*
				 * if (!mioturno) { return 100 + depth; } else { return -100 - depth; }
				 */
			}
		}

		MNKCell selectedCellThisRun = null;

		if (free.size() == 1 || depth == 0) {
			if (depth == 0) {
				// eseguo un eval
				return eval(lastMarked, depth);
			} else if (free.iterator().hasNext()) {
				MNKCell cell = free.iterator().next();
				if (mioturno)
					B[cell.i][cell.j] = mioStato;
				else
					B[cell.i][cell.j] = nemicoStato;
				if (isWinningCell(cell.i, cell.j)) {
					double e = eval(cell, depth) * (free.size()+1);
					B[cell.i][cell.j] = MNKCellState.FREE;
					// return eval(lastMarked, depth);
					if (mioturno) {
						return e;
						// return 100 + depth;
						// return eval(cell, depth);
					} else {
						return e;
						// return -100 - depth;
						// return eval(cell, depth);
					}
				} else {
					B[cell.i][cell.j] = MNKCellState.FREE;
					// return eval(lastMarked, depth);
					return 0;
				}
			} else {
				print("NO NEXT");
			}
		} else {
			if (mioturno) {
				g = Double.NEGATIVE_INFINITY;
				double a = alpha;
				MNKCell[] cells = free.toArray(new MNKCell[free.size()]);
				for (int i = 0; i < cells.length; i++) {
					MNKCell cell = cells[i];
					free.remove(cell);
					cell = new MNKCell(cell.i, cell.j, mioStato);
					marked.add(cell);
					B[cell.i][cell.j] = mioStato;

					double res = minimax(free, marked, cell, depth - 1, false, a, beta);
					if (depth == MAX_DEPTH) {
						print("RES : " + res + "\n");
						showMap();
					}
					if (res > g) {
						g = res;
						selectedCellThisRun = cell;
						if (depth == MAX_DEPTH) {
							selectedCell = cell;
						}
					}
					a = Math.max(a, g);
					free.add(cells[i]);
					marked.remove(cell);
					B[cell.i][cell.j] = MNKCellState.FREE;
					if (g < beta)
						break;
				}
			} else {
				g = Double.POSITIVE_INFINITY;
				double b = beta;
				MNKCell[] cells = free.toArray(new MNKCell[free.size()]);
				for (int i = 0; i < cells.length; i++) {
					MNKCell cell = cells[i];
					free.remove(cell);
					cell = new MNKCell(cell.i, cell.j, nemicoStato);
					marked.add(cell);
					B[cell.i][cell.j] = nemicoStato;

					double res = minimax(free, marked, cell, depth - 1, true, alpha, b);
					if (depth == MAX_DEPTH) {
						print("RES : " + res + "\n");
						showMap();
					}
					if (res < g) {
						g = res;
						selectedCellThisRun = cell;
						if (depth == MAX_DEPTH) {
							selectedCell = cell;
						}
					}
					b = Math.min(b, g);
					free.add(cells[i]);
					marked.remove(cell);
					B[cell.i][cell.j] = MNKCellState.FREE;
					if (g > alpha)
						break;
				}
			}
		}

		if (g <= alpha) {
			HashStruct n = new HashStruct();
			n.upperbound = g;
			n.cell = selectedCellThisRun;
			if (mioturno) {
				B[selectedCellThisRun.i][selectedCellThisRun.j] = mioStato;
			} else {
				B[selectedCellThisRun.i][selectedCellThisRun.j] = nemicoStato;
			}
			//print("SAVE : " + g + "\n");
			//showMap();
			alphaBetaMemory.put(B, n);
			B[selectedCellThisRun.i][selectedCellThisRun.j] = MNKCellState.FREE;
		}

		if (g > alpha && g < beta) {
			HashStruct n = new HashStruct();
			n.upperbound = g;
			n.lowerbound = g;
			n.cell = selectedCellThisRun;
			if (mioturno) {
				B[selectedCellThisRun.i][selectedCellThisRun.j] = mioStato;
			} else {
				B[selectedCellThisRun.i][selectedCellThisRun.j] = nemicoStato;
			}
			//print("SAVE : " + g + "\n");
			//showMap();
			alphaBetaMemory.put(B, n);
			B[selectedCellThisRun.i][selectedCellThisRun.j] = MNKCellState.FREE;
		}

		if (g >= beta) {
			HashStruct n = new HashStruct();
			n.lowerbound = g;
			n.cell = selectedCellThisRun;
			if (mioturno) {
				B[selectedCellThisRun.i][selectedCellThisRun.j] = mioStato;
			} else {
				B[selectedCellThisRun.i][selectedCellThisRun.j] = nemicoStato;
			}
			//print("SAVE : " + g + "\n");
			//showMap();
			alphaBetaMemory.put(B, n);
			B[selectedCellThisRun.i][selectedCellThisRun.j] = MNKCellState.FREE;
		}

		return g;
	}

	private int eval(MNKCell last, int depth) {
		// se utilizzo l'eval vuol dire che ho raggiunto il massimo della profondita
		// ora è compito delle euristiche scegliere la mossa migliore

		// se ho n elementi in filo ottengo n punti (minimo di 3)
		// se il nemico ha n elementi in fila perdo n/2 punti
		// se il nemico vince perdo 1000 punti
		// gli spazi vuoti adiacenti all'ultima mossa valgono 1 punto caduno

		// piu la casella scelta è vicina alla mossa precedente più ottiene punti

		// si valuta in base all'ultima mossa effettuata ovvero MC[MC.length-1]

		int score = 0;

		// se la mossa che sto per fare è nella posizione vincente del nemico devo farla
		if (B[last.i][last.j] == mioStato) {
			if (isWinningCell(last.i, last.j, K, mioStato))
				score += 100;
		} else if (B[last.i][last.j] == nemicoStato) {
			if (isWinningCell(last.i, last.j, K, nemicoStato))
				score -= 100;
		}

		// score /= depth;

		return score;
	}

	private boolean isWinningCell(int i, int j, int K_TEMP, MNKCellState player) {
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

	private boolean isWinningCell(int i, int j, int K_TEMP) {
		MNKCellState s = B[i][j];
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
		return "mtdPlayer";
	}
}
