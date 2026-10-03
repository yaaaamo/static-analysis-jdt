package hai913i.tp1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import hai913i.tp1.metrics.Metrics;
import hai913i.tp1.model.*;

/** B2 rules tested on hand-built models: no file is parsed, no JDT is used. */
class MetricsTest {

  // A type whose methods have the given body sizes (0 = no body), with some fields
  private static TypeFact type(String name, int fields, int... bodyLocs) {
    List<MethodFact> methods = new ArrayList<>();
    for (int i = 0; i < bodyLocs.length; i++) {
      methods.add(new MethodFact(name + "#m" + i + "()", "m" + i, List.of(), false, bodyLocs[i]));
    }
    List<FieldFact> fs = new ArrayList<>();
    for (int i = 0; i < fields; i++) {
      fs.add(new FieldFact("f" + i, "int", Visibility.PRIVATE));
    }
    return new TypeFact(name, "p", TypeKind.CLASS, List.of(), List.of(), fs, methods);
  }

  private static ProjectFacts facts(List<TypeFact> types) {
    return new ProjectFacts(List.of(), 0, List.of(), types, List.of());
  }

  private static List<String> names(List<TypeFact> types) {
    return types.stream().map(TypeFact::qualifiedName).toList();
  }

  @Test
  void fifteenCandidatesKeepTwoNotOne() {
    // n = 15 -> k = ceil(15/10) = 2 ; (int)(15 * 0.1) would give 1
    List<TypeFact> types = new ArrayList<>();
    for (int i = 1; i <= 15; i++) {
      types.add(type("T" + i, 0, new int[i]));   // T1 has 1 method, ..., T15 has 15
    }
    assertEquals(List.of("T15", "T14"), names(Metrics.topByMethods(facts(types))));
  }

  @Test
  void tiesWithTheKthAreKept() {
    // 4 classes -> k = 1, but A and B are tied at the top
    ProjectFacts f = facts(List.of(
            type("A", 0, 1, 1, 1), type("B", 0, 1, 1, 1),
            type("C", 0, 1), type("D", 0)));
    assertEquals(List.of("A", "B"), names(Metrics.topByMethods(f)));
  }

  @Test
  void classWithoutMethodIsNeverKept() {
    // all values are 0: nothing is kept, even though k = 1
    ProjectFacts f = facts(List.of(type("A", 0), type("B", 0)));
    assertTrue(Metrics.topByMethods(f).isEmpty());
    assertEquals(0.0, Metrics.avgMethodsPerClass(f));
  }

  @Test
  void methodsWithoutBodyAreNeverInQ12() {
    // like Loanable: only abstract methods
    TypeFact itf = type("I", 0, 0, 0, 0);
    assertTrue(Metrics.longestMethodsPerClass(facts(List.of(itf))).get(itf).isEmpty());
  }

  @Test
  void averageLinesIgnoresMethodsWithoutBody() {
    // bodies of 4 and 2 lines, plus one abstract method: (4 + 2) / 2 = 3.00
    ProjectFacts f = facts(List.of(type("A", 0, 4, 2, 0)));
    assertEquals(3.0, Metrics.avgLinesPerMethod(f), 1e-9);
  }

  @Test
  void thresholdXIsStrict() {
    // Q11: strictly more than X
    ProjectFacts f = facts(List.of(type("Five", 0, 1, 1, 1, 1, 1), type("Six", 0, 1, 1, 1, 1, 1, 1)));
    assertEquals(List.of("Six"), names(Metrics.moreMethodsThan(f, 5)));
  }

  @Test
  void emptyProjectGivesNoResultAndNoDivisionByZero() {
    ProjectFacts f = facts(List.of());
    assertTrue(Metrics.topByMethods(f).isEmpty());
    assertEquals(0.0, Metrics.avgMethodsPerClass(f));
    assertEquals(0.0, Metrics.avgLinesPerMethod(f));
    assertEquals(0, Metrics.maxParameters(f));
  }
}