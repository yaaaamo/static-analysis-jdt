package hai913i.tp1.callgraph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import hai913i.tp1.model.CallFact;
import hai913i.tp1.model.MethodFact;
import hai913i.tp1.model.ProjectFacts;
import hai913i.tp1.model.TypeFact;

/** Static call graph built from the fact model only (step B3). No JDT. */
public final class CallGraph {

  /** One edge caller -> callee; weight = number of call sites from caller to callee. */
  public record Edge(String caller, String callee, int weight) {}

  private final SortedSet<String> nodes = new TreeSet<>();
  // caller -> (callee -> weight), and the reverse graph callee -> (caller -> weight)
  private final SortedMap<String, SortedMap<String, Integer>> callees = new TreeMap<>();
  private final SortedMap<String, SortedMap<String, Integer>> callers = new TreeMap<>();
  private int internalSites;
  private int externalCalls;
  private int unresolvedCalls;

  private CallGraph() {}

  public static CallGraph build(ProjectFacts facts) {
    CallGraph g = new CallGraph();

    // (1) Nodes: every counted method of the project (constructors included)
    for (TypeFact t : facts.types()) {
      for (MethodFact m : t.methods()) {
        g.nodes.add(m.id());
      }
    }

    // (2) Edges: one call site at a time
    for (CallFact c : facts.calls()) {
      if (!c.resolved()) {
        g.unresolvedCalls++;                  // counted, no edge
      } else if (!g.nodes.contains(c.targetId())) {
        g.externalCalls++;                    // JDK or library: counted, no edge
      } else {
        g.addSite(c.callerId(), c.targetId());
      }
    }
    return g;
  }

  // (3) Several sites f -> g give one edge whose weight is the number of sites
  private void addSite(String from, String to) {
    internalSites++;
    callees.computeIfAbsent(from, k -> new TreeMap<>()).merge(to, 1, Integer::sum);
    callers.computeIfAbsent(to, k -> new TreeMap<>()).merge(from, 1, Integer::sum);
  }

  // ---- Queries ----

  public SortedSet<String> nodes() { return Collections.unmodifiableSortedSet(nodes); }
  public int internalSites()       { return internalSites; }
  public int externalCalls()       { return externalCalls; }
  public int unresolvedCalls()     { return unresolvedCalls; }

  /** All edges, sorted by caller then callee (deterministic). */
  public List<Edge> edges() {
    List<Edge> result = new ArrayList<>();
    callees.forEach((from, tos) -> tos.forEach((to, w) -> result.add(new Edge(from, to, w))));
    return result;
  }

  public int edgeCount() { return edges().size(); }

  /** (4) Forward direction: the methods called by id, with weights. */
  public SortedMap<String, Integer> calleesOf(String id) {
    return Collections.unmodifiableSortedMap(callees.getOrDefault(id, new TreeMap<>()));
  }

  /** (4) Reverse direction: the methods that call id (impact analysis). */
  public SortedMap<String, Integer> callersOf(String id) {
    return Collections.unmodifiableSortedMap(callers.getOrDefault(id, new TreeMap<>()));
  }

  /** Nodes matching a query: the exact id if it exists, otherwise ids containing the text. */
  public List<String> find(String query) {
    if (nodes.contains(query)) return List.of(query);
    return nodes.stream().filter(id -> id.contains(query)).toList();
  }
}
