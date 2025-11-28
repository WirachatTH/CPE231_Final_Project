package th.ac.kmutt.cpe.algorithm.project.graph;

public class Edge {
    private int to;
    private int weight;

    public Edge(int t, int w) {
        this.to = t;
        this.weight = w;
    }

    public int getTo() {
        return to;
    }

    public int getWeight() {
        return weight;
    }
}
