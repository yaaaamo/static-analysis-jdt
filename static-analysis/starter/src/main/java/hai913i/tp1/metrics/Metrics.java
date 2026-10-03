package hai913i.tp1.metrics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

import hai913i.tp1.model.MethodFact;
import hai913i.tp1.model.ProjectFacts;
import hai913i.tp1.model.TypeFact;

/** The 13 metrics of step B2, computed from the fact model only (no JDT). */
public final class Metrics {

  private Metrics() {}

  // ---- Q1-Q4: simple counts ----

  public static int classCount(ProjectFacts f)   { return f.types().size(); }
  public static int lineCount(ProjectFacts f)    { return f.totalLines(); }
  public static int packageCount(ProjectFacts f) { return f.packages().size(); }

  public static int methodCount(ProjectFacts f) {
    return f.types().stream().mapToInt(t -> t.methods().size()).sum();
  }

  public static int fieldCount(ProjectFacts f) {
    return f.types().stream().mapToInt(t -> t.fields().size()).sum();
  }

  // ---- Q5-Q7: averages ----

  public static double avgMethodsPerClass(ProjectFacts f) {
    return ratio(methodCount(f), classCount(f));
  }

  /** Methods with a body: bodyLoc > 0 (abstract and interface methods have 0). */
  public static int methodsWithBody(ProjectFacts f) {
    return (int) allMethods(f).stream().filter(m -> m.bodyLoc() > 0).count();
  }

  public static int bodyLines(ProjectFacts f) {
    return allMethods(f).stream().mapToInt(MethodFact::bodyLoc).sum();
  }

  public static double avgLinesPerMethod(ProjectFacts f) {
    return ratio(bodyLines(f), methodsWithBody(f));
  }

  public static double avgFieldsPerClass(ProjectFacts f) {
    return ratio(fieldCount(f), classCount(f));
  }

  // ---- Q8-Q12: rankings ----

  /** Q8: the 10% of classes with the most methods. */
  public static List<TypeFact> topByMethods(ProjectFacts f) {
    return topTenPercent(f.types(), t -> t.methods().size());
  }

  /** Q9: the 10% of classes with the most fields. */
  public static List<TypeFact> topByFields(ProjectFacts f) {
    return topTenPercent(f.types(), t -> t.fields().size());
  }

  /** Q10: classes in both Q8 and Q9. */
  public static List<TypeFact> topByMethodsAndFields(ProjectFacts f) {
    Set<String> inQ9 = topByFields(f).stream()
            .map(TypeFact::qualifiedName).collect(Collectors.toSet());
    return topByMethods(f).stream()
            .filter(t -> inQ9.contains(t.qualifiedName())).toList();
  }

  /** Q11: classes with strictly more than x methods. */
  public static List<TypeFact> moreMethodsThan(ProjectFacts f, int x) {
    return f.types().stream().filter(t -> t.methods().size() > x).toList();
  }

  /** Q12: for each class, the 10% of its methods with the most body lines. */
  public static Map<TypeFact, List<MethodFact>> longestMethodsPerClass(ProjectFacts f) {
    Map<TypeFact, List<MethodFact>> result = new LinkedHashMap<>();   // keeps class order
    for (TypeFact t : f.types()) {
      result.put(t, topTenPercent(t.methods(), MethodFact::bodyLoc));
    }
    return result;
  }

  // ---- Q13: maximum number of parameters ----

  public static int maxParameters(ProjectFacts f) {
    return allMethods(f).stream().mapToInt(m -> m.parameterTypes().size()).max().orElse(0);
  }

  public static List<MethodFact> methodsWithMaxParameters(ProjectFacts f) {
    int max = maxParameters(f);
    return allMethods(f).stream().filter(m -> m.parameterTypes().size() == max).toList();
  }

  // ---- The "10 %" rule of the subject (§4.3) ----

  /**
   * Keeps the k = ceil(n/10) best candidates in decreasing order of value,
   * plus every candidate equal to the k-th one. A value of 0 is never kept.
   */
  public static <T> List<T> topTenPercent(List<T> candidates, ToIntFunction<T> value) {
    int n = candidates.size();
    if (n == 0) return List.of();
    int k = (n + 9) / 10;                       // ceil(n/10) with integers: 15 -> 2

    List<T> sorted = new ArrayList<>(candidates);
    sorted.sort(Comparator.comparingInt(value).reversed());   // stable: ties keep name order
    int threshold = value.applyAsInt(sorted.get(k - 1));     // value of the k-th candidate

    return sorted.stream()
            .filter(c -> value.applyAsInt(c) >= threshold)       // first k + ties with the k-th
            .filter(c -> value.applyAsInt(c) > 0)                // 0 is never kept
            .toList();
  }

  // ---- helpers ----

  private static List<MethodFact> allMethods(ProjectFacts f) {
    return f.types().stream().flatMap(t -> t.methods().stream()).toList();
  }

  private static double ratio(int a, int b) {
    return b == 0 ? 0.0 : (double) a / b;
  }
}