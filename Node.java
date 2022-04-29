package mnkgame;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;

import javax.swing.event.ListDataEvent;

public class Node {
    Node parent;
    List<Node> children;

    private Random rand;
    protected MNKCellState[][] B;
    MNKCell[] FC;
    Queue<MNKCell> NotUsedFreeCell;
    MNKCell[] MC;
    private int M, N, K;
    private MNKCellState nodeState;

    int simulationCount;
    double uct;
    int value;

    public Node(int M, int N, int K, MNKCellState nodeState, MNKCell[] FC, MNKCell[] MC, MNKCellState[][] B) {
        try {
            children = new ArrayList<Node>();
            rand = new Random(System.currentTimeMillis());
            this.FC = copyMatrix(FC);
            this.MC = copyMatrix(MC);
            this.B = copyMatrixState(B);
            this.nodeState = nodeState;

            this.M = M;
            this.N = N;
            this.K = K;
            this.simulationCount = 0;
            this.value = 0;
            this.uct = Double.POSITIVE_INFINITY;

            // init queue
            this.NotUsedFreeCell = new LinkedList<MNKCell>();
            for (int i = 0; i < this.FC.length; i++) {
                if (this.FC[i] != null)
                    this.NotUsedFreeCell.add(this.FC[i]);
            }
            // print("INIT NOT USED FREE CELL : " + NotUsedFreeCell.size() + " of " +
            // this.FC.length + "\n");
        } catch (Exception e) {
            print("ERRORE COSTRUTTORE NODE : " + e + "\n");
        }
    }

    // #region GET E SET

    public List<Node> getChildren() {
        return children;
    }

    public int getSimulationCount() {
        return simulationCount;
    }

    public double getUCT() {
        return uct;
    }

    public int getValue() {
        return value;
    }

    public void setParent(Node par) {
        this.parent = par;
    }

    // #endregion

    /*
     * Metodo : Selection Descrizione : ricerca il nodo con l'uct e prosegue finche
     * non si arriva ad un nodo che ancora non ha raggiunto il suo numero massimo di
     * figli, ovvero il numero di possibili mosse e poi si effettua expansion una
     * volta raggiunto un nodo completo si effettua selection sul figlio con massimo
     * uct
     */
    public void Selection() {
        try {
            if (NotUsedFreeCell.size() > 0) {
                Expansion();
            } else {
                Node bestNodeUCT = null;
                for (Node n : children) {
                    if (bestNodeUCT == null) {
                        bestNodeUCT = n;
                    } else {
                        if (n.getUCT() > bestNodeUCT.getUCT()) {
                            bestNodeUCT = n;
                        }
                    }
                }

                if (bestNodeUCT != null)
                    bestNodeUCT.Selection();
                else {
                    // ho raggiunto lo stadio finale
                    Simulation();
                    // print("SOLUZIONE NON TROVATA : " + children.size() + " - " +
                    // NotUsedFreeCell.size() + "\n");
                    // showMap();
                }
            }
        } catch (Exception e) {
            print("ERRORE DURANTE LA SELECTION : " + e + "\n");
        }
    }

    public MNKCell CurrentMove() {
        if (MC.length > 0) {
            return MC[MC.length - 1];
        }
        return null;
    }

    public MNKCell BestMove() {
        MNKCell cell = null;
        Node bestNodeUCT = null;
        for (Node n : children) {
            if (bestNodeUCT == null) {
                bestNodeUCT = n;
            } else {
                if (n.getUCT() > bestNodeUCT.getUCT()) {
                    bestNodeUCT = n;
                }
            }
        }
        if (bestNodeUCT != null) {
            cell = bestNodeUCT.CurrentMove();
            print("BEST UCT : " + bestNodeUCT.getUCT() + "\n");
        }
        return cell;
    }

    public void Expansion() {
        try {
            MNKCell cell = NotUsedFreeCell.peek();
            if (cell == null) {
                print("CELL IS NULL\n");
                return;
            }
            NotUsedFreeCell.remove();
            MNKCell[] newFC = new MNKCell[FC.length - 1];
            MNKCell[] newMC = new MNKCell[MC.length + 1];
            MNKCellState[][] newB = copyMatrixState(B);
            MNKCellState newState = swapCellState(nodeState);

            int j = 0;
            for (int i = 0; i < FC.length; i++) {
                if (FC[i] != cell) {
                    newFC[j] = FC[i];
                    j++;
                }
            }

            for (int i = 0; i < MC.length; i++) {
                newMC[i] = MC[i];
            }
            newMC[newMC.length - 1] = cell;
            newB[cell.i][cell.j] = nodeState;

            Node newNode = new Node(M, N, K, newState, newFC, newMC, newB);
            newNode.setParent(this);
            children.add(newNode);
            newNode.Simulation();
        } catch (Exception e) {
            print("ERRORE DURANTE LA EXPANSION : " + e + "\n");
        }
    }

    public void Simulation() {
        try {
            MNKCellState currState = nodeState;
            MNKCellState[][] backup = copyMatrixState(B);

            List<MNKCell> listFC = new ArrayList<MNKCell>();
            int inc = 0;

            for (int i = 0; i < FC.length; i++) {
                if (FC[i] != null)
                    listFC.add(FC[i]);
            }

            if (MC.length > 0) {
                if (isWinningCell(MC[MC.length - 1].i, MC[MC.length - 1].j, nodeState)) {
                    inc = 1;
                    listFC = new ArrayList<MNKCell>();
                }

                if (isWinningCell(MC[MC.length - 1].i, MC[MC.length - 1].j, swapCellState(nodeState))) {
                    inc = -1;
                    listFC = new ArrayList<MNKCell>();
                }
            }

            while (listFC.size() > 0) {
                currState = swapCellState(nodeState);
                int r = rand.nextInt(listFC.size());
                MNKCell cell = listFC.get(r);
                listFC.remove(r);

                try {
                    B[cell.i][cell.j] = currState;
                    if (isWinningCell(cell.i, cell.j, currState)) {
                        if (currState == nodeState) {
                            inc = 1;
                        } else {
                            inc = -1;
                        }
                        break;
                    }
                } catch (Exception e) {
                    print("ERRORE : " + e + " : " + (cell == null) + " (r : " + r + " out of " + FC.length + ")\n");
                }
            }

            B = copyMatrixState(backup);
            BackPropagation(inc);
        } catch (Exception e) {
            print("ERRORE DURANTE LA SIMULATION : " + e + "\n");
        }
    }

    public void BackPropagation(int inc) {
        simulationCount++;
        value += inc;
        /*
         * si metto -inc perchè il turno del padre era quello dell'altro giocatore
         */
        if (parent != null)
            parent.BackPropagation(-inc);
        for (Node n : children) {
            n.CalcolaUCT(simulationCount);
            if (parent == null && simulationCount == 5000) {
                print("UCT : " + n.getUCT() + "\nSIM COUNT : " + n.getSimulationCount() + "\nPAR COUNT : "
                        + simulationCount + "\nVAL : " + n.getValue() + "\n");
                n.showMap();
            }
        }
    }

    public double CalcolaUCT(int parentSimulationCount) {
        int t = parentSimulationCount;
        uct = Math.sqrt(2) * Math.sqrt(Math.log((double) t) / (double) simulationCount);
        uct += (double) ((double) (value) / (double) (simulationCount));
        return uct;
    }

    private MNKCellState swapCellState(MNKCellState state) {
        if (state == MNKCellState.P1)
            return MNKCellState.P2;
        else if (state == MNKCellState.P2)
            return MNKCellState.P1;
        return MNKCellState.FREE;
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
        for (int k = 1; i + k < N && j - k >= 0 && B[i + k][j - k] == s; k++)
            n++; // backward check
        if (n >= K)
            return true;

        return false;
    }

    private void print(String msg) {
        System.out.print(msg);
    }

    private MNKCellState[][] copyMatrixState(MNKCellState[][] A) {
        MNKCellState[][] D = new MNKCellState[A.length][];
        for (int i = 0; i < A.length; i++) {
            D[i] = new MNKCellState[A[i].length];
            for (int j = 0; j < A[i].length; j++) {
                D[i][j] = A[i][j];
            }
        }

        return D;
    }

    private MNKCell[] copyMatrix(MNKCell[] A) {
        MNKCell[] D = new MNKCell[A.length];
        for (int i = 0; i < A.length; i++) {
            D[i] = A[i];
        }

        return D;
    }

    public void showMap() {
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

}
