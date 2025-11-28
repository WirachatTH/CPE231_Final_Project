package th.ac.kmutt.cpe.algorithm.project.strategy;

import java.util.*;
import th.ac.kmutt.cpe.algorithm.project.graph.*;

public class DijkstraStrategy implements PathFindingStrategy {
    public PathFindingResult solve(Graph graph) {

        int startId = graph.getStartId();
        int goalId = graph.getGoalId();

        Map<Integer, Node> nodes = graph.getNodeMap();
        Map<Integer, ArrayList<Edge>> adj = graph.getAdjacencyListMap();

        HashMap<Integer, Integer> dist = new HashMap<>();
        HashMap<Integer, Integer> prev = new HashMap<>();
        HashSet<Integer> visited = new HashSet<>();

        // initialize dist
        for (int id : nodes.keySet()) {
            dist.put(id, Integer.MAX_VALUE);
            prev.put(id, -1);
        }

        dist.put(startId, 0);

        // PQ stores: [cost, nodeId]
        PriorityQueue<int[]> pq =
                new PriorityQueue<>(Comparator.comparingInt(a -> a[0]));

        pq.add(new int[]{0, startId});

        while (!pq.isEmpty()) {

            int[] cur = pq.poll();
            int curCost = cur[0];
            int curId = cur[1];

            // Skip outdated entries
            if (curCost > dist.get(curId)) continue;

            // Skip if already finalized
            if (visited.contains(curId)) continue;
            visited.add(curId);

            // Stop if reached goal
            if (curId == goalId) break;

            for (Edge e : adj.get(curId)) {

                int nextId = e.getTo();
                int weight = e.getWeight();

                // Dijkstra relax step
                int newCost = dist.get(curId) + weight;

                // STRICT BETTER → avoid equal-cost loop
                if (newCost < dist.get(nextId)) {
                    dist.put(nextId, newCost);
                    prev.put(nextId, curId);
                    pq.add(new int[]{newCost, nextId});
                }
            }
        }

        return reconstruct(dist, prev, startId, goalId);
    }

    private PathFindingResult reconstruct(
            HashMap<Integer, Integer> dist,
            HashMap<Integer, Integer> prev,
            int startId,
            int goalId) {

        ArrayList<Integer> path = new ArrayList<>();

        // unreachable case
        if (dist.get(goalId) == Integer.MAX_VALUE) {
            return new PathFindingResult(Integer.MAX_VALUE, path);
        }

        int cur = goalId;
        HashSet<Integer> detectLoop = new HashSet<>();

        
        while (cur != -1) {
            if (detectLoop.contains(cur)) {
                return new PathFindingResult(Integer.MAX_VALUE, new ArrayList<>());
            }
            detectLoop.add(cur);

            path.add(cur);
            cur = prev.get(cur);
        }

        Collections.reverse(path);

        return new PathFindingResult(dist.get(goalId), path);
    }
}
