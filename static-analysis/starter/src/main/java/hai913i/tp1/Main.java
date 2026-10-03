package hai913i.tp1;
import java.nio.file.Path;
import java.util.List;

import hai913i.tp1.extraction.FactExtractor;
import hai913i.tp1.model.*;
import org.eclipse.jdt.core.compiler.IProblem;
import hai913i.tp1.parse.JdtParser;
import hai913i.tp1.parse.JdtParser.ParsedFile;
import hai913i.tp1.parse.ProjectSources;
//A1
import hai913i.tp1.extraction.AstPrintVisitor;
//A2
import java.util.ArrayList;
import java.util.Comparator;
import hai913i.tp1.extraction.StructureVisitor;
//A3
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import hai913i.tp1.extraction.CallVisitor;
//B2
import java.util.Locale;
import java.util.function.ToIntFunction;
import hai913i.tp1.metrics.Metrics;


/**
 * Point d'entrée en ligne de commande de l'analyseur (version de départ).
 *
 * Le squelette ne vérifie que l'environnement (point de contrôle A0) : il analyse le projet et affiche
 * le nombre d'unités de compilation et d'erreurs. Tout le reste est à concevoir : extraction de la
 * structure, appels, métriques, graphe d'appel, options comme le seuil X.
 *
 * Usage : java -jar target/hai913i-tp1-analyzer.jar DOSSIER_DU_PROJET
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) throws Exception {
      if (args.length < 1) {
        System.err.println("Usage : java -jar target/hai913i-tp1-analyzer.jar DOSSIER_DU_PROJET");
        System.exit(2);
      }
      // Threshold X for Q11 (optional for now; full CLI in B4)
      Integer x = null;
      if (args.length >= 2) {
        try {
          x = Integer.parseInt(args[1]);
          if (x < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
          System.err.println("Erreur : le seuil X doit etre un entier positif ou nul (recu : "
                  + args[1] + ")");
          System.exit(4);
          return;
        }
      }
      Path project = Path.of(args[0]);
      ProjectSources sources;
      try {
        sources = ProjectSources.of(project);
      } catch (IllegalArgumentException e) {
        System.err.println("Erreur : " + e.getMessage());
        System.exit(3);
        return;
      }

      List<ParsedFile> files = JdtParser.parse(sources, List.of());
      int errors = 0;
      for (ParsedFile file : files) {
        for (IProblem problem : file.unit().getProblems()) {
          if (problem.isError()) {
            errors++;
            System.err.println(file.path().getFileName() + ":" + problem.getSourceLineNumber() + " "
                    + problem.getMessage());
          }
        }
      }
      System.out.println("Racine des sources     : " + sources.sourceRoot());
      System.out.println("Unites de compilation  : " + files.size());
      System.out.println("Erreurs de compilation : " + errors);

      // À FAIRE (A1 et suite) : parcourir les AST avec vos visiteurs, construire votre modèle de faits,
      // puis calculer les métriques et le graphe d'appel. Gardez cette classe courte : elle lit les
      // arguments et délègue.

      // A1 : affichage des AST de Dvd.java et Category.java
      for (ParsedFile file : files) {
        String fileName = file.path().getFileName().toString();
        if (fileName.equals("Dvd.java")
                || fileName.equals("Category.java")) {
          System.out.println();
          System.out.println("===== AST de " + fileName + " =====");
          AstPrintVisitor visitor = new AstPrintVisitor();
          file.unit().accept(visitor);
        }
      }

      // B1: build the fact model once; everything below reads only this
      ProjectFacts facts = FactExtractor.extract(files);

      // A2: project structure
      List<TypeFact> types = facts.types();
      int nbMethods = types.stream().mapToInt(t -> t.methods().size()).sum();
      long nbCtors = types.stream().flatMap(t -> t.methods().stream())
              .filter(MethodFact::constructor).count();
      int nbFields = types.stream().mapToInt(t -> t.fields().size()).sum();

      System.out.println();
      System.out.println("===== Structure (A2) =====");
      System.out.println("Types      : " + types.size());
      System.out.println("Methodes   : " + nbMethods + " (dont " + nbCtors + " constructeurs)");
      System.out.println("Attributs  : " + nbFields);
      System.out.println("Lignes     : " + facts.totalLines());
      System.out.println("Paquetages : " + facts.packages().size() + " " + facts.packages());

      for (TypeFact t : types) {
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

      // A3: calls and static receiver types
      List<CallFact> calls = facts.calls();
      long nbInternal = calls.stream().filter(CallFact::internal).count();
      long nbUnresolved = calls.stream().filter(c -> !c.resolved()).count();
      long nbExternal = calls.size() - nbInternal - nbUnresolved;

      System.out.println();
      System.out.println("===== Appels (A3) =====");
      System.out.println("Appels      : " + calls.size());
      System.out.println("Internes    : " + nbInternal);
      System.out.println("Externes    : " + nbExternal);
      System.out.println("Non resolus : " + nbUnresolved);

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

        // B2: metrics (computed from the model only)
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
        if (x != null) {
          System.out.println("Q11 plus de " + x + " methodes   : "
                  + typesWith(Metrics.moreMethodsThan(facts, x), t -> t.methods().size()));
        } else {
          System.out.println("Q11 plus de X methodes     : X non fourni");
        }
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
      // Two decimals, with a dot whatever the machine's locale (same output everywhere)
      private static String fmt(double d) {
        return String.format(Locale.ROOT, "%.2f", d);
      }

      // "library.model.Item (10), library.model.Member (6)"
      private static String typesWith(List<TypeFact> types, ToIntFunction<TypeFact> value) {
        if (types.isEmpty()) return "aucune";
        return types.stream()
                .map(t -> t.qualifiedName() + " (" + value.applyAsInt(t) + ")")
                .collect(Collectors.joining(", "));
      }

      // "checkOut(library.model.Member)" instead of the full id
      private static String shortId(MethodFact m) {
        return m.id().substring(m.id().indexOf('#') + 1);
      }

}
