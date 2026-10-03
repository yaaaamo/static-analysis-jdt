package hai913i.tp1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import hai913i.tp1.callgraph.CallGraph;
import hai913i.tp1.extraction.FactExtractor;
import hai913i.tp1.model.*;
import hai913i.tp1.parse.JdtParser;
import hai913i.tp1.parse.ProjectSources;

/** Checkpoint B3 on the validation project, plus graph rules on a hand-built model. */
class CallGraphTest {

  private static final String BORROW =
          "library.service.LoanService#borrow(library.model.Member,java.lang.String,int)";
  private static final String LOANABLE_CHECKOUT = "library.model.Loanable#checkOut(library.model.Member)";
  private static final String ITEM_CHECKOUT = "library.model.Item#checkOut(library.model.Member)";
  private static final String REPEAT = "library.util.TextUtils#repeat(java.lang.String,int)";

  private static CallGraph graph;

  @BeforeAll
  static void build() throws Exception {
    ProjectSources sources = ProjectSources.of(Path.of("../resources/validation"));
    graph = CallGraph.build(FactExtractor.extract(JdtParser.parse(sources, List.of())));
  }

  // ---- Checkpoint B3 (validation project) ----

  @Test
  void countsNodesEdgesAndSites() {
    assertEquals(59, graph.nodes().size());
    assertEquals(44, graph.edgeCount());
    assertEquals(46, graph.internalSites());
    assertEquals(48, graph.externalCalls());
    assertEquals(0, graph.unresolvedCalls());
  }

  @Test
  void severalSitesMakeOneWeightedEdge() {
    assertEquals(2, graph.calleesOf(
                    "library.service.Catalog#add(library.model.Item,library.model.Item)")
            .get("library.service.Catalog#add(library.model.Item)"));
  }

  @Test
  void recursionIsAnEdgeToItself() {
    assertEquals(1, graph.calleesOf(REPEAT).get(REPEAT));
  }

  @Test
  void callThroughInterfaceTargetsTheInterfaceMethod() {
    assertEquals(Map.of(BORROW, 1), graph.callersOf(LOANABLE_CHECKOUT));
    assertTrue(graph.callersOf(ITEM_CHECKOUT).isEmpty());   // no CHA expansion
  }

  @Test
  void graphCanBeQueriedForward() {
    assertEquals(Map.of(
                    "library.model.Item#isAvailable()", 1,
                    "library.model.Member#addLoan(library.model.Item)", 1),
            graph.calleesOf(ITEM_CHECKOUT));
  }

  @Test
  void weightsAddUpToInternalSites() {
    int sum = graph.edges().stream().mapToInt(CallGraph.Edge::weight).sum();
    assertEquals(graph.internalSites(), sum);
  }

  // ---- Graph rules on a hand-built model (no file, no JDT) ----

  @Test
  void externalAndUnresolvedCallsAreCountedButMakeNoEdge() {
    MethodFact a = new MethodFact("A#a()", "a", List.of(), false, 5);
    MethodFact b = new MethodFact("A#b()", "b", List.of(), false, 1);
    TypeFact type = new TypeFact("A", "p", TypeKind.CLASS, List.of(), List.of(), List.of(), List.of(a, b));
    List<CallFact> calls = List.of(
            new CallFact("A#a()", "b", "A#b()", "A", 1, true, true),                        // internal
            new CallFact("A#a()", "b", "A#b()", "A", 2, true, true),                        // same edge
            new CallFact("A#a()", "size", "java.util.List#size()", "java.util.List", 3, true, false), // external
            new CallFact("A#a()", "foo", null, "?", 4, false, false));                      // unresolved
    CallGraph g = CallGraph.build(new ProjectFacts(List.of(), 0, List.of(), List.of(type), calls));

    assertEquals(1, g.edgeCount());
    assertEquals(2, g.calleesOf("A#a()").get("A#b()"));
    assertEquals(2, g.internalSites());
    assertEquals(1, g.externalCalls());
    assertEquals(1, g.unresolvedCalls());
  }
}
