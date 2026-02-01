package com.deadlock.detector.analyzer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 死锁分析器 - 使用图算法检测环
 */
public class DeadlockAnalyzer {

    /**
     * 检测等待图中的环（死锁）
     *
     * @param waitForGraph 等待图: key=等待线程ID, value=被等待线程ID
     * @return 所有检测到的环（每个环是一个线程ID列表）
     */
    public List<List<Long>> detectCycles(Map<Long, Long> waitForGraph) {
        List<List<Long>> cycles = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        Set<Long> inStack = new HashSet<>();

        for (Long node : waitForGraph.keySet()) {
            if (!visited.contains(node)) {
                List<Long> path = new ArrayList<>();
                dfs(node, waitForGraph, visited, inStack, path, cycles);
            }
        }

        return cycles;
    }

    /**
     * 深度优先搜索检测环
     */
    private void dfs(Long node, Map<Long, Long> graph,
                     Set<Long> visited, Set<Long> inStack,
                     List<Long> path, List<List<Long>> cycles) {
        visited.add(node);
        inStack.add(node);
        path.add(node);

        Long next = graph.get(node);
        if (next != null) {
            if (!visited.contains(next)) {
                dfs(next, graph, visited, inStack, path, cycles);
            } else if (inStack.contains(next)) {
                // 找到环，提取环中的节点
                int cycleStart = path.indexOf(next);
                if (cycleStart >= 0) {
                    List<Long> cycle = new ArrayList<>(path.subList(cycleStart, path.size()));
                    cycles.add(cycle);
                }
            }
        }

        path.remove(path.size() - 1);
        inStack.remove(node);
    }

    /**
     * 检测多对多等待关系中的环
     *
     * @param waitForGraph 等待图: key=等待线程ID, value=被等待线程ID集合
     * @return 所有检测到的环
     */
    public List<List<Long>> detectCyclesMulti(Map<Long, Set<Long>> waitForGraph) {
        List<List<Long>> cycles = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        Set<Long> inStack = new HashSet<>();

        for (Long node : waitForGraph.keySet()) {
            if (!visited.contains(node)) {
                List<Long> path = new ArrayList<>();
                dfsMulti(node, waitForGraph, visited, inStack, path, cycles);
            }
        }

        return cycles;
    }

    private void dfsMulti(Long node, Map<Long, Set<Long>> graph,
                          Set<Long> visited, Set<Long> inStack,
                          List<Long> path, List<List<Long>> cycles) {
        visited.add(node);
        inStack.add(node);
        path.add(node);

        Set<Long> nextNodes = graph.get(node);
        if (nextNodes != null) {
            for (Long next : nextNodes) {
                if (!visited.contains(next)) {
                    dfsMulti(next, graph, visited, inStack, path, cycles);
                } else if (inStack.contains(next)) {
                    int cycleStart = path.indexOf(next);
                    if (cycleStart >= 0) {
                        List<Long> cycle = new ArrayList<>(path.subList(cycleStart, path.size()));
                        cycles.add(cycle);
                    }
                }
            }
        }

        path.remove(path.size() - 1);
        inStack.remove(node);
    }
}
