package hai913i.tp1.report;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

import hai913i.tp1.callgraph.CallGraph;
import hai913i.tp1.metrics.Metrics;
import hai913i.tp1.model.CallFact;
import hai913i.tp1.model.FieldFact;
import hai913i.tp1.model.MethodFact;
import hai913i.tp1.model.ProjectFacts;
import hai913i.tp1.model.TypeFact;

/** Text output of the results. Reads the model, the metrics and the graph; never JDT. */
public final class TextReport {

  private TextReport() {}

  // ---- A2: structure ----

  public static void printStructure(ProjectFacts facts) {
    long ctors = facts.types().stream().flatMap(t -> t.methods().stream())
            .filter(MethodFact::constructor).count();

    System.out.println();
    System.out.println("===== Structure (A2) =====");
    System.out.println("Types      : " + Metrics.classCount(facts));
    System.out.println("Methodes   : " + Metrics.methodCount(facts) + " (dont " + ctors + " constructeurs)");
    System.out.println("Attributs  : " + Metrics.fieldCount(facts));
    System.out.println("Lignes     : " + facts.totalLines());
    System.out.println("Paquetages : " + facts.packages().size() + " " + facts.packages());

    for (TypeFact t : facts.types()) {
      System.out.println();
      System.out.println(t.qualifiedName() + " [" + t.kind() + "] paquetage " + t.packageName());
      System.out.println("  superclasses : " + t.superclasses());
      System.out.println("  interfaces   : " + t.interfaces());
      System.out.println("  attributs (" + t.fields().size() + ")");
      for (FieldFact f : t.fields()) {
        System.out.println("    " + f.visibility() + " " + f.declaredType() + " " + f.name());
      }
      System.out.println("  methodes (" + t.methods().size() + ")");
      for (MethodFact m : t.methods()) {
        System.out.println("    " + (m.constructor() ? "[ctor] " : "")
                + m.name() + " : " + m.parameterTypes().size() + " parametre(s)");
      }
    }
  }

  // ---- A3: calls ----

  public static void printCalls(ProjectFacts facts) {
    List<CallFact> calls = facts.calls();
    long internal = calls.stream().filter(CallFact::internal).count();
    long unresolved = calls.stream().filter(c -> !c.resolved()).count();

    System.out.println();
    System.out.println("===== Appels (A3) =====");
    System.out.println("Appels      : " + calls.size());
    System.out.println("Internes    : " + internal);
    System.out.println("Externes    : " + (calls.size() - internal - unresolved));
    System.out.println("Non resolus : " + unresolved);

    Map<String, List<CallFact>> byCaller = calls.stream()
            .collect(Collectors.groupingBy(CallFact::callerId, TreeMap::new, Collectors.toList()));
    for (Map.Entry<String, List<CallFact>> e : byCaller.entrySet()) {
      System.out.println();
      System.out.println(e.getKey());
      for (CallFact c : e.getValue()) {
        String target = c.resolved() ? c.targetId() : "NON RESOLU";
        String scope = !c.resolved() ? "" : (c.internal() ? " [interne]" : " [externe]");
        System.out.println("  l." + c.line() + "  " + c.name()
                + "  receveur " + c.staticReceiverType() + "  -> " + target + scope);
      }
    }
  }

  // ---- B2: metrics ----

  public static void printMetrics(ProjectFacts facts, int x) {
    System.out.println();
    System.out.println("===== Metriques (B2) =====");
    System.out.println("Q1  classes                : " + Metrics.classCount(facts));
    System.out.println("Q2  lignes de code         : " + Metrics.lineCount(facts));
    System.out.println("Q3  methodes               : " + Metrics.methodCount(facts));
    System.out.println("Q4  paquetages             : " + Metrics.packageCount(facts));
    System.out.println("Q5  methodes par classe    : " + fmt(Metrics.avgMethodsPerClass(facts)));
    System.out.println("Q6  lignes par methode     : " + fmt(Metrics.avgLinesPerMethod(facts))
            + " (" + Metrics.bodyLines(facts) + " lignes / "
            + Metrics.methodsWithBody(facts) + " methodes ayant un corps)");
    System.out.println("Q7  attributs par classe   : " + fmt(Metrics.avgFieldsPerClass(facts)));
    System.out.println("Q8  10% + de methodes      : "
            + typesWith(Metrics.topByMethods(facts), t -> t.methods().size()));
    System.out.println("Q9  10% + d'attributs      : "
            + typesWith(Metrics.topByFields(facts), t -> t.fields().size()));
    System.out.println("Q10 les deux               : "
            + typesWith(Metrics.topByMethodsAndFields(facts), t -> t.methods().size()));
    System.out.println("Q11 plus de X methodes (X=" + x + ") : "
            + typesWith(Metrics.moreMethodsThan(facts, x), t -> t.methods().size()));
    System.out.println("Q12 10% des methodes les plus longues, par classe :");
    for (Map.Entry<TypeFact, List<MethodFact>> e : Metrics.longestMethodsPerClass(facts).entrySet()) {
      String methods = e.getValue().isEmpty() ? "aucune"
              : e.getValue().stream()
              .map(m -> shortId(m) + " (" + m.bodyLoc() + ")")
              .collect(Collectors.joining(", "));
      System.out.println("    " + e.getKey().qualifiedName() + " : " + methods);
    }
    System.out.println("Q13 max parametres         : " + Metrics.maxParameters(facts) + " -> "
            + Metrics.methodsWithMaxParameters(facts).stream()
            .map(MethodFact::id).collect(Collectors.joining(", ")));
  }

  // ---- B3: call graph ----

  public static void printGraph(CallGraph graph, String methodQuery) {
    System.out.println();
    System.out.println("===== Graphe d'appel (B3) =====");
    System.out.println("Noeuds             : " + graph.nodes().size());
    System.out.println("Arcs               : " + graph.edgeCount());
    System.out.println("Sites internes     : " + graph.internalSites());
    System.out.println("Appels externes    : " + graph.externalCalls());
    System.out.println("Appels non resolus : " + graph.unresolvedCalls());
    System.out.println("Arcs (appelant -> appelee [poids]) :");
    for (CallGraph.Edge e : graph.edges()) {
      System.out.println("  " + e.caller() + " -> " + e.callee() + " [" + e.weight() + "]");
    }

    if (methodQuery == null) return;
    List<String> found = graph.find(methodQuery);
    System.out.println();
    if (found.size() != 1) {
      System.out.println("Methode '" + methodQuery + "' : "
              + (found.isEmpty() ? "introuvable" : "ambigue " + found));
      return;
    }
    String id = found.get(0);
    System.out.println("Methode    : " + id);
    System.out.println("appelees   : " + weights(graph.calleesOf(id)));
    System.out.println("appelantes : " + weights(graph.callersOf(id)));
  }

  // ---- helpers ----

  // Two decimals, with a dot whatever the machine's locale (same output everywhere)
  private static String fmt(double d) {
    return String.format(Locale.ROOT, "%.2f", d);
  }

  private static String typesWith(List<TypeFact> types, ToIntFunction<TypeFact> value) {
    if (types.isEmpty()) return "aucune";
    return types.stream()
            .map(t -> t.qualifiedName() + " (" + value.applyAsInt(t) + ")")
            .collect(Collectors.joining(", "));
  }

  private static String shortId(MethodFact m) {
    return m.id().substring(m.id().indexOf('#') + 1);
  }

  private static String weights(Map<String, Integer> methods) {
    if (methods.isEmpty()) return "aucune";
    return methods.entrySet().stream()
            .map(e -> e.getKey() + " [" + e.getValue() + "]")
            .collect(Collectors.joining(", "));
  }
}