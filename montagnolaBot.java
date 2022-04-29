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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.*;
import java.text.DecimalFormat;
import java.util.Timer;
/*
 * 
 * Una idea sarebbe quella di eseguire all'inizia la mappature della migliori
 * mosse in base alle possibili mosse vincenti, e ordinate la lista free in
 * ordine crescente di punteggio per cella cosi che poi il minimax possa potare
 * l'albero quasi immediatamente o comunque con più probabilità
 * 
 */

/**
 * Totally random software player.
 */
public class montagnolaBot implements MNKPlayer {
    private Random rand;
    private int TIMEOUT;

    private MNKCellState B[][];

    private int M, N, K;
    private MNKCellState mioStato, nemicoStato, freeStato;
    private MNKCell selectedCell;
    private boolean TEMPO_SCADUTO;

    private int MAX_DEPTH;
    private int WIN = 1000;
    private int LOSS = -1000;
    private int SECURE_WIN = 800;
    private int SECURE_LOSS = -800;
    private int ALMOST_WIN = 50;
    private int ALMOST_LOSS = -50;

    private int mioPunteggio;
    private int nemicoPunteggio;
    private int mie_celle_raggiungibili;
    private int[] win_table_mia;
    private int[] win_table_nemica;
    private Boolean secure_win_mia, secure_win_nemica;
    private int almost_secure_win_mia, almost_secure_win_nemica;
    private MNKGameState vincoIo;
    private MNKGameState vinceNemico;

    private Boolean primoAgiocare;

    Boolean DEBUG = false;

    Boolean test = false;

    int C;

    /**
     * Default empty constructor
     */
    public montagnolaBot() {
    }

    public void initPlayer(int M, int N, int K, boolean first, int timeout_in_secs) {
        // New random seed for each game
        rand = new Random(System.currentTimeMillis());
        // Save the timeout for testing purposes
        TIMEOUT = timeout_in_secs;

        this.M = M;
        this.N = N;
        this.K = K;
        // 9 va ancora bene per un 4x4x4

        // alphabeta 5+1 va bene per un 6x5x4
        // negamax 6+1 va bene fino a 7x6x4
        // negamax 5+1 va bene fino a 7x6x5
        // negascout funziona su 5+1 a 7x7x5
        // dispari per massimizzare
        this.MAX_DEPTH = 5;

        freeStato = MNKCellState.FREE;
        if (first) {
            mioStato = MNKCellState.P1;
            nemicoStato = MNKCellState.P2;
        } else {
            mioStato = MNKCellState.P2;
            nemicoStato = MNKCellState.P1;
        }
        vincoIo = first ? MNKGameState.WINP1 : MNKGameState.WINP2;
        vinceNemico = first ? MNKGameState.WINP2 : MNKGameState.WINP1;

        try {
            B = new MNKCellState[this.M][];
            for (int i = 0; i < M; i++) {
                B[i] = new MNKCellState[N];
                for (int j = 0; j < N; j++) {
                    B[i][j] = freeStato;
                }
            }
        } catch (Exception e) {
            System.out.println("ERROR init : " + e.toString());
        }

        primoAgiocare = first;

        mioPunteggio = 0;
        nemicoPunteggio = 0;
        win_table_mia = new int[K + 1];
        win_table_nemica = new int[K + 1];
        for (int i = 0; i < K + 1; i++) {
            win_table_mia[i] = 0;
            win_table_nemica[i] = 0;
        }
    }

    /**
     * Selects a random cell in <code>FC</code>
     */
    public MNKCell selectCell(MNKCell[] FC, MNKCell[] MC) {

        TEMPO_SCADUTO = false;

        if (FC.length == 1) {
            B[FC[0].i][FC[0].j] = mioStato;
            return FC[0];
        }

        if (MC.length > 0) {
            B[MC[MC.length - 1].i][MC[MC.length - 1].j] = nemicoStato;
        }

        if (MC.length == 0) {
            if ((M >= K + 2 && N >= K + 1) || ((N >= K + 2 && M >= K + 1)) && K >= 4) {
                B[2][2] = mioStato;
                for (int i = 0; i < FC.length; i++) {
                    if (FC[i].i == 2 && FC[i].j == 2) {
                        return FC[i];
                    }
                }
            }
        }

        Timer timer = new Timer();

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                // Your database code here
                System.out.println("TEMPO FINITO");
                TEMPO_SCADUTO = true;
            }
        }, (TIMEOUT) * 950);

        float[][] map = mappa_vittorie();

        List<MNKCell> free = new ArrayList<>();
        // COSTO O((m*n)log(m*n))
        PriorityQueue<Map.Entry<MNKCell, Float>> pQueue = new PriorityQueue<>(
                Map.Entry.comparingByValue(Comparator.reverseOrder()));

        MNKCell secure_cell_mia = null, secure_cell_nemica = null;
        float eval_secure_mia = 0, eval_secure_nemica = 0;
        MNKCell immediata_nemico = null;
        // COSTO O(m*n)
        for (int i = 0; i < FC.length; i++) {
            free.add(FC[i]);
            pQueue.offer(new AbstractMap.SimpleEntry<>(FC[i], map[FC[i].i][FC[i].j]));
            if (isWinningCell(FC[i].i, FC[i].j, mioStato)) {
                B[FC[i].i][FC[i].j] = mioStato;
                return FC[i];
            }

            if (isWinningCell(FC[i].i, FC[i].j, nemicoStato)) {
                immediata_nemico = FC[i];
            }

            float e = eval(FC[i], true);
            if (secure_win_mia) {
                if (e > eval_secure_mia) {
                    secure_cell_mia = FC[i];
                    eval_secure_mia = e;
                }
            }
            if (secure_win_nemica) {
                if (e > eval_secure_nemica) {
                    secure_cell_nemica = FC[i];
                    eval_secure_nemica = e;
                }
            }
        }

        if (immediata_nemico != null) {
            B[immediata_nemico.i][immediata_nemico.j] = mioStato;
            return immediata_nemico;
        }

        if (secure_cell_mia != null) {
            B[secure_cell_mia.i][secure_cell_mia.j] = mioStato;
            return secure_cell_mia;
        }

        if (secure_cell_nemica != null) {
            B[secure_cell_nemica.i][secure_cell_nemica.j] = mioStato;
            return secure_cell_nemica;
        }

        selectedCell = null;
        C = 0;

        if (M <= 9 && N <= 9)
            MAX_DEPTH = 5;
        else
            MAX_DEPTH = 3;

        if (M <= 15 && N <= 15) {
            float e = Float.NEGATIVE_INFINITY;
            
            for (int i = 0; i < FC.length; i++) {
                AbstractMap.Entry<MNKCell, Float> en = pQueue.peek();
                pQueue.remove(en);
                MNKCell cell = en.getKey();

                free.remove(cell);
                B[cell.i][cell.j] = mioStato;
                float r = en.getValue() * (float) (1.0 / 10);

                r += alphabeta(free, cell, false, true, 1, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);

                B[cell.i][cell.j] = freeStato;
                free.add(cell);
                if (r > e) {
                    e = r;
                    selectedCell = cell;
                }
            }

        } else {
            selectedCell = pQueue.peek().getKey();
            pQueue.remove();
            for (int i = 0; i < 10; i++) {
                MNKCell c = pQueue.peek().getKey();
                pQueue.remove();
            }
        }

        timer.cancel();
        B[selectedCell.i][selectedCell.j] = mioStato;
        return selectedCell;
    }

    float alphabeta(List<MNKCell> free, MNKCell last, Boolean mioTurno, Boolean first, int depth, float alpha,
            float beta) {

        float e = 0;

        C++;

        if (last != null && (depth >= MAX_DEPTH || isWinningCell(last.i, last.j) || TEMPO_SCADUTO)) {
            e = eval(last, !mioTurno);
            B[last.i][last.j] = freeStato;
            return e / depth;
        }

        if (free.size() == 1) {
            MNKCell mnkCell = free.get(0);
            if (mioTurno) {
                B[mnkCell.i][mnkCell.j] = mioStato;
            } else {
                B[mnkCell.i][mnkCell.j] = nemicoStato;
            }

            if (isWinningCell(mnkCell.i, mnkCell.j)) {
                if (B[last.i][last.j] == mioStato) {
                    e = WIN;
                } else {
                    e = LOSS;
                }
            }

            B[mnkCell.i][mnkCell.j] = freeStato;
            return e / depth;
        } else {
            if (mioTurno) {
                e = Float.NEGATIVE_INFINITY;
                MNKCell[] b = free.toArray(new MNKCell[free.size()]);
                try {
                    for (int i = 0; i < b.length; i++) {
                        MNKCell mnkCell = b[i];
                        B[mnkCell.i][mnkCell.j] = mioStato;
                        free.remove(mnkCell);
                        float r = alphabeta(free, mnkCell, !mioTurno, false, depth + 1, alpha, beta);
                        e = Math.max(r, e);
                        alpha = Math.max(e, alpha);
                        free.add(mnkCell);
                        B[mnkCell.i][mnkCell.j] = freeStato;
                        if (beta <= alpha)
                            break;
                    }
                } catch (Exception ex) {
                    System.out.println("Error : " + ex.toString());
                }
            } else {
                e = Float.POSITIVE_INFINITY;
                MNKCell[] b = free.toArray(new MNKCell[free.size()]);
                try {
                    for (int i = 0; i < b.length; i++) {
                        MNKCell mnkCell = b[i];
                        B[mnkCell.i][mnkCell.j] = nemicoStato;
                        free.remove(mnkCell);
                        float r = alphabeta(free, mnkCell, !mioTurno, false, depth + 1, alpha, beta);
                        e = Math.min(r, e);
                        beta = Math.min(e, beta);
                        free.add(mnkCell);
                        B[mnkCell.i][mnkCell.j] = freeStato;
                        if (beta <= alpha)
                            break;
                    }
                } catch (Exception ex) {
                    System.out.println("Error : " + ex.toString());
                }
            }
        }

        return e;
    }

    private float eval(MNKCell last, Boolean mioTurno) {

        for (int j = 0; j < K + 1; j++) {
            win_table_mia[j] = 0;
            win_table_nemica[j] = 0;
        }
        Boolean winMio = isWinningCell(last.i, last.j, mioStato);
        Boolean winNemico = isWinningCell(last.i, last.j, nemicoStato);
        float e = 0;
        if (winMio || winNemico) {
            if (mioTurno)
                return WIN;
            else
                return LOSS;
        }
        if (secure_win_mia || secure_win_nemica) {
            if (mioTurno)
                e += SECURE_WIN;
            else
                e += SECURE_LOSS;
        }

        if (mioTurno)
            e += ALMOST_WIN * almost_secure_win_mia;
        else
            e += ALMOST_LOSS * almost_secure_win_nemica;

        for (int i = 2; i < K; i++) {
            if (win_table_mia[i] > 0) {
                if (mioTurno) {
                    e += i * i * win_table_mia[i];
                } else {
                    e -= i * i * win_table_mia[i];
                }
            }
            if (win_table_nemica[i] > 0) {
                if (mioTurno) {
                    e += i * i * win_table_nemica[i];
                } else {
                    e -= i * i * win_table_nemica[i];
                }
            }
        }

        return e;
    }

    /*
     * METODO : mappa_vittorie FUNZIONE : calcola per ogni cella sulla mappa il suo
     * numero di possibili vittorie, pesate tramite "mioPunteggio" e
     * "nemicoPunteggio", e le varie combinazioni ottenibili
     */
    float[][] mappa_vittorie() {
        float[][] mappa = new float[M][N];
        try {
            for (int i = 0; i < M; i++) {
                for (int j = 0; j < N; j++) {
                    if (B[i][j] == MNKCellState.FREE)
                        mappa[i][j] = count(i, j);
                    else {
                        mappa[i][j] = 0;
                        float c = count(i, j);
                        if (B[i][j] == mioStato) {
                            mioPunteggio += c;
                        } else if (B[i][j] == nemicoStato) {
                            nemicoPunteggio += c;
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Errore mappa vittorie : " + e.toString());
        }
        return mappa;
    }

    float count(int i, int j) {
        float c = 0;
        for (int k = 0; k < K + 1; k++) {
            win_table_mia[k] = 0;
            win_table_nemica[k] = 0;
        }
        almost_secure_win_mia = 0;
        almost_secure_win_nemica = 0;

        try {
            if (isWinningCell(i, j, mioStato) || isWinningCell(i, j, nemicoStato))
                return Float.POSITIVE_INFINITY;

            int countMie = countMieMosse(i, j);
            c += mie_celle_raggiungibili;
            int countNemiche = countMosseAvversarie(i, j);
            c += countMie + countNemiche;

            int inc_mio = 10;
            int inc_nemico = 5;

            if (secure_win_mia || secure_win_nemica) {
                c += SECURE_WIN;
            }

            // due almost secure valgono come una secure
            if (almost_secure_win_mia >= 2)
                c += SECURE_WIN;
            else {
                c += ALMOST_WIN * almost_secure_win_mia;
            }
            if (almost_secure_win_nemica >= 2)
                c += SECURE_WIN;
            else {
                c += ALMOST_WIN * almost_secure_win_nemica;
            }
            if (!primoAgiocare)
                c -= ALMOST_LOSS * almost_secure_win_nemica;

            try {
                for (int k = 1; k < K; k++) {
                    if (win_table_mia[k] > 0) {
                        // due almost secure valgono come una secure
                        c += inc_mio * k * win_table_mia[k];
                    }
                    if (win_table_nemica[k] > 0) {
                        c += inc_nemico * k * win_table_nemica[k];
                    }
                }
            } catch (Exception e) {
                System.out.println("Errore dentro count (" + i + "," + j + ") : " + e.toString());
            }
        } catch (Exception e) {
            System.out.println("Errore count (" + i + "," + j + ") : " + e.toString());
        }

        return c;
    }

    int countMieMosse(int i, int j) {
        return countMosseGenerico(i, j, mioStato);
    }

    int countMosseAvversarie(int i, int j) {
        return countMosseGenerico(i, j, nemicoStato);
    }

    /*
     * COSTO : O(m+n)
     */
    int countMosseGenerico(int i, int j, MNKCellState player) {

        MNKCellState opp = (player == mioStato) ? nemicoStato : mioStato;
        if (player == mioStato)
            mie_celle_raggiungibili = 0;
        int c = 0;
        int n = 0;

        try {
            // Diagonal check
            n = 1;
            for (int k = 1; i - k >= 0 && j - k >= 0 && B[i - k][j - k] != opp && k < K; k++) {
                n++; // backward check
                if (B[i - k][j - k] == player)
                    mie_celle_raggiungibili++;
            }
            for (int k = 1; i + k < M && j + k < N && (B[i + k][j + k] != opp) && k < K; k++) {
                n++; // forward check
                if (B[i + k][j + k] == player)
                    mie_celle_raggiungibili++;
            }
            if (n >= K) {
                int inc = n - K + 1;
                if (inc > K)
                    inc = K;
                c += inc;
            }

            // Anti-diagonal check
            n = 1;
            for (int k = 1; i - k >= 0 && j + k < N && (B[i - k][j + k] != opp) && k < K; k++) {
                n++; // backward check
                if (B[i - k][j + k] == player)
                    mie_celle_raggiungibili++;
            }
            for (int k = 1; i + k < M && j - k >= 0 && (B[i + k][j - k] != opp) && k < K; k++) {
                n++; // forward check
                if (B[i + k][j - k] == player)
                    mie_celle_raggiungibili++;
            }
            if (n >= K) {
                int inc = n - K + 1;
                if (inc > K)
                    inc = K;
                c += inc;
            }

            // Horizontal check
            n = 1;
            for (int k = 1; j - k >= 0 && (B[i][j - k] != opp) && k < K; k++) {
                n++; // backward check
                if (B[i][j - k] == player)
                    mie_celle_raggiungibili++;
            }
            for (int k = 1; j + k < N && (B[i][j + k] != opp) && k < K; k++) {
                n++; // forward check
                if (B[i][j + k] == player)
                    mie_celle_raggiungibili++;
            }
            if (n >= K) {
                int inc = n - K + 1;
                if (inc > K)
                    inc = K;
                c += inc;
            }

            // Vertical check
            n = 1;
            for (int k = 1; i - k >= 0 && (B[i - k][j] != opp) && k < K; k++) {
                n++; // backward check
                if (B[i - k][j] == player)
                    mie_celle_raggiungibili++;
            }
            for (int k = 1; i + k < M && (B[i + k][j] != opp) && k < K; k++) {
                n++; // forward check
                if (B[i + k][j] == player)
                    mie_celle_raggiungibili++;
            }
            if (n >= K) {
                int inc = n - K + 1;
                if (inc > K)
                    inc = K;
                c += inc;
            }
        } catch (Exception e) {
            System.out.println("Errore count mosse nemiche (" + i + "," + j + ") : " + e.toString());
        }

        return c;
    }

    private boolean isWinningCell(int i, int j, MNKCellState player) {
        MNKCellState s = player;
        MNKCellState opp = (s == mioStato) ? nemicoStato : mioStato;
        int n;
        if (s == mioStato) {
            secure_win_mia = false;
            almost_secure_win_mia = 0;
        } else {
            secure_win_nemica = false;
            almost_secure_win_nemica = 0;
        }

        Boolean se = false;
        int al = 0;
        int k = 0;
        int b = 0;
        int f = 0;

        try {
            // Useless pedantic check
            if (s == MNKCellState.FREE)
                return false;

            try {
                // Horizontal check
                n = 1;
                b = 1;
                f = 1;
                for (k = 1; j - k >= 0 && B[i][j - k] == s; k++)
                    n++; // backward check
                b = k;
                for (k = 1; j + k < N && B[i][j + k] == s; k++)
                    n++; // forward check
                f = k;
                if (n >= 2) {
                    if (n >= K)
                        return true;
                    int sx = 0, dx = 0;
                    sx = (j - b) + 1;
                    dx = (N - 1) - (j + f - 1);

                    int tot = sx + dx;
                    se = ((j - b >= 0 && B[i][j - b] == freeStato) && (j + f < N && B[i][j + f] == freeStato)
                            && tot >= (K - n) && B[i][(j - b + 1) - sx] != opp && B[i][(j + f - 1) + dx] != opp);
                    if (se) {
                        if (n == K - 1 /* && B[i][j] == player */) {

                            if (player == mioStato) {
                                secure_win_mia = true;
                            } else {
                                secure_win_nemica = true;
                            }
                        }
                        if (n == K - 2/* && B[i][j] == player */) {

                            // __x_ -> _xx_
                            if (j - 1 >= 0 && j + (K - 2) < N) {
                                if (B[i][j - 1] == freeStato && B[i][j + (K - 2)] == freeStato
                                        && ((j - 2 >= 0 && B[i][j - 2] == freeStato)
                                                || j + (K - 1) < N && B[i][j + (K - 1)] == freeStato)) {
                                    al++;
                                }
                            }
                            // _x__ -> _xx_
                            if (j + 1 < N && j - (K - 2) >= 0) {
                                if (B[i][j + 1] == freeStato && B[i][j - (K - 2)] == freeStato
                                        && ((j + 2 < N && B[i][j + 2] == freeStato)
                                                || j - (K - 1) >= 0 && B[i][j - (K - 1)] == freeStato)) {
                                    al++;
                                }
                            }

                            if (player == mioStato) {
                                almost_secure_win_mia = al;
                            } else {
                                almost_secure_win_nemica = al;
                            }
                        }

                        if (player == mioStato) {
                            // controlla le estremità della fila creata, se almeno uno è libero ho la
                            // possibilità di vincere
                            // altrimenti anche la mossa che porta a K-1 è inutile
                            for (k = K - 1; k > 1; k--) {
                                if (n >= k) {
                                    win_table_mia[k]++;
                                    break;
                                }
                            }
                        } else {
                            for (k = K - 1; k > 1; k--) {
                                if (n >= k) {
                                    win_table_nemica[k]++;
                                    break;
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("Errore in Horizontal Check (" + i + "," + j + ") : " + e.toString());
            }

            try {
                // Vertical check
                n = 1;
                b = 1;
                f = 1;
                for (k = 1; i - k >= 0 && B[i - k][j] == s; k++)
                    n++; // backward check
                b = k;
                for (k = 1; i + k < M && B[i + k][j] == s; k++)
                    n++; // forward check
                f = k;
                if (n >= 2) {
                    if (n >= K)
                        return true;
                    int su = 0, giu = 0;
                    su = (i - b) + 1;
                    giu = (M - 1) - (i + f - 1);

                    int tot = su + giu;
                    se = ((i - b >= 0 && B[i - b][j] == freeStato) && (i + f < M && B[i + f][j] == freeStato)
                            && tot >= (K - n) && B[(i - b + 1) - su][j] != opp && B[(i + f - 1) + giu][j] != opp);
                    if (se) {
                        if (n == K - 1 /* && B[i][j] == player */) {

                            if (s == mioStato) {
                                secure_win_mia = true;
                            } else {
                                secure_win_nemica = true;
                            }
                        }
                        if (n == K - 2/* && B[i][j] == player */) {

                            /*
                             * _ _ _ x x -> x _ _
                             */
                            if (i - 1 >= 0 && i + (K - 2) < M) {
                                if (B[i - 1][j] == freeStato && B[i + (K - 2)][j] == freeStato
                                        && ((i - 2 >= 0 && B[i - 2][j] == freeStato)
                                                || i + (K - 1) < M && B[i + (K - 1)][j] == freeStato)) {
                                    al++;
                                }
                            }

                            /*
                             * _ _ x x _ -> x _ _
                             */
                            if (i + 1 < M && i - (K - 2) >= 0) {
                                if (B[i + 1][j] == freeStato && B[i - (K - 2)][j] == freeStato
                                        && ((i + 2 < M && B[i + 2][j] == freeStato)
                                                || i - (K - 1) >= 0 && B[i - (K - 1)][j] == freeStato)) {
                                    al++;
                                }
                            }

                            if (player == mioStato) {
                                almost_secure_win_mia = al;
                            } else {
                                almost_secure_win_nemica = al;
                            }
                        }
                        if (player == mioStato) {
                            // controlla le estremità della fila creata, se almeno uno è libero ho la
                            // possibilità di vincere
                            // altrimenti anche la mossa che porta a K-1 è inutile
                            for (k = K - 1; k > 1; k--) {
                                if (n >= k) {
                                    win_table_mia[k]++;
                                    break;
                                }
                            }
                        } else {
                            for (k = K - 1; k > 1; k--) {
                                if (n >= k) {
                                    win_table_nemica[k]++;
                                    break;
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("Errore in Vertical Check (" + i + "," + j + ") : " + e.toString());
            }

            try {
                // Diagonal check
                n = 1;
                b = 1;
                f = 1;
                for (k = 1; i - k >= 0 && j - k >= 0 && B[i - k][j - k] == s; k++)
                    n++; // backward check
                b = k;
                for (k = 1; i + k < M && j + k < N && B[i + k][j + k] == s; k++)
                    n++; // forward check
                f = k;
                if (n >= 2) {
                    if (n >= K)
                        return true;
                    int su = 0, giu = 0;
                    su = Math.min((j - b) + 1, (i - b) + 1);
                    giu = Math.min((N - 1) - (j + f - 1), (M - 1) - (i + f - 1));

                    int tot = su + giu;
                    se = ((i - b >= 0 && j - b >= 0 && B[i - b][j - b] == freeStato)
                            && (i + f < M && j + f < N && B[i + f][j + f] == freeStato) && tot >= (K - n)
                            && B[(i - b + 1) - su][(j - b + 1) - su] != opp
                            && B[(i + f - 1) + giu][(j + f - 1) + giu] != opp);
                    if (se) {
                        if (n == K - 1 /* && B[i][j] == player */) {

                            if (player == mioStato) {
                                secure_win_mia = true;
                            } else {
                                secure_win_nemica = true;
                            }
                        }
                        if (n == K - 2/* && B[i][j] == player */) {

                            /*
                             * _ _ _ x x -> x _ _
                             */
                            if (i - 1 >= 0 && j - 1 >= 0 && i + (K - 2) < M && j + (K - 2) < N
                                    && B[i - 1][j - 1] == freeStato && B[i + (K - 2)][j + (K - 2)] == freeStato
                                    && ((i - 2 >= 0 && j - 2 >= 0 && B[i - 2][j - 2] == freeStato) || i + (K - 1) < M
                                            && j + (K - 1) < N && B[i + (K - 1)][j + (K - 1)] == freeStato)) {
                                al++;
                            }

                            /*
                             * _ _ x x _ -> x _ _
                             */
                            if (i + 1 < M && j + 1 < N && i - (K - 2) >= 0 && j - (K - 2) >= 0
                                    && B[i + 1][j + 1] == freeStato && B[i - (K - 2)][j - (K - 2)] == freeStato
                                    && ((i + 2 < M && j + 2 < N && B[i + 2][j + 2] == freeStato) || i - (K - 1) >= 0
                                            && j - (K - 1) >= 0 && B[i - (K - 1)][j - (K - 1)] == freeStato)) {
                                al++;
                            }

                            if (player == mioStato) {
                                almost_secure_win_mia = al;
                            } else {
                                almost_secure_win_nemica = al;
                            }
                        }
                        if (player == mioStato) {
                            // controlla le estremità della fila creata, se almeno uno è libero ho la
                            // possibilità di vincere
                            // altrimenti anche la mossa che porta a K-1 è inutile
                            for (k = K - 1; k > 1; k--) {
                                if (n >= k) {
                                    win_table_mia[k]++;
                                    break;
                                }
                            }
                        } else {
                            for (k = K - 1; k > 1; k--) {
                                if (n >= k) {
                                    win_table_nemica[k]++;
                                    break;
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("Errore in Diagonal Check (" + i + "," + j + ") : " + e.toString());
            }

            try {
                // Anti-diagonal check
                n = 1;
                b = 1;
                f = 1;
                for (k = 1; i - k >= 0 && j + k < N && B[i - k][j + k] == s; k++)
                    n++; // backward check
                b = k;
                for (k = 1; i + k < M && j - k >= 0 && B[i + k][j - k] == s; k++)
                    n++;
                f = k;
                if (n >= 2) {
                    if (n >= K)
                        return true;
                    int su = 0, giu = 0;
                    su = Math.min((i - b) + 1, (N - 1) - (j + b - 1));
                    giu = Math.min((j - f) + 1, (M - 1) - (i + f - 1));
                    int tot = su + giu;

                    se = ((i - b >= 0 && j + b < N && B[i - b][j + b] == freeStato)
                            && (i + f < M && j - f >= 0 && B[i + f][j - f] == freeStato) && tot >= (K - n)
                            && B[(i - b + 1) - su][(j + b - 1) + su] != opp
                            && B[(i + f - 1) + giu][(j - f + 1) - giu] != opp);
                    if (se) {
                        if (n == K - 1 /* && B[i][j] == player */) {

                            if (player == mioStato) {
                                secure_win_mia = true;
                            } else {
                                secure_win_nemica = true;
                            }
                        }

                        if (n == K - 2/* && B[i][j] == player */) {
                            /*
                             * _ _ _ x x -> x _ _
                             */
                            if (i - 1 >= 0 && j + 1 < N && i + (K - 2) < M && j - (K - 2) >= 0
                                    && B[i - 1][j + 1] == freeStato && B[i + (K - 2)][j - (K - 2)] == freeStato
                                    && ((i - 2 >= 0 && j + 2 < N && B[i - 2][j + 2] == freeStato) || i + (K - 1) < M
                                            && j - (K - 1) >= 0 && B[i + (K - 1)][j - (K - 1)] == freeStato)) {
                                al++;
                            }
                            if (i + 1 < M && j - 1 >= 0 && i - (K - 2) >= 0 && j + (K - 2) < N
                                    && B[i + 1][j - 1] == freeStato && B[i - (K - 2)][j + (K - 2)] == freeStato
                                    && ((i + 2 < M && j - 2 >= 0 && B[i + 2][j - 2] == freeStato) || i - (K - 1) >= 0
                                            && j + (K - 1) < N && B[i - (K - 1)][j + (K - 1)] == freeStato)) {
                                al++;
                            }

                            if (player == mioStato) {
                                almost_secure_win_mia = al;
                            } else {
                                almost_secure_win_nemica = al;
                            }
                        }

                        if (player == mioStato) {
                            // controlla le estremità della fila creata, se almeno uno è libero ho la
                            // possibilità di vincere
                            // altrimenti anche la mossa che porta a K-1 è inutile
                            for (k = K - 1; k > 1; k--) {
                                if (n >= k) {
                                    win_table_mia[k]++;
                                    break;
                                }
                            }
                        } else {
                            for (k = K - 1; k > 1; k--) {
                                if (n >= k) {
                                    win_table_nemica[k]++;
                                    break;
                                }
                            }
                        }
                    }
                    if (n >= K)
                        return true;
                }
            } catch (Exception e) {
                System.out.println("Errore in Anti diagonal Check (" + i + "," + j + ") : " + e.toString());
            }

        } catch (

        Exception e) {
            System.out.println("Errore in isWinning (" + i + "," + j + ") : " + e.toString());
        }

        win_table_mia[K] = 0;
        win_table_nemica[K] = 0;

        return false;
    }

    // Check winning state from cell i, j
    private boolean isWinningCell(int i, int j) {
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

    public String playerName() {
        return "montagnolaBot";
    }
}
