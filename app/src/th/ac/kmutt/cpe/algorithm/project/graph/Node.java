package th.ac.kmutt.cpe.algorithm.project.graph;

public class Node {
    private int id;
    private int row, col;
    private boolean isStart;
    private boolean isGoal;

    public Node(int id, int row, int col, boolean isStart, boolean isGoal){
        this.id = id;
        this.row = row;
        this.col = col;
        this.isStart = isStart;
        this.isGoal = isGoal;
    }

    public int getId() {
        return id;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public boolean isStart() {
        return isStart;
    }

    public boolean isGoal() {
        return isGoal;
    }
}
