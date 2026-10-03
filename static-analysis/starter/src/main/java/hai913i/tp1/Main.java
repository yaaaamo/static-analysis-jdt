package hai913i.tp1;
import java.nio.file.Path;
import java.util.List;
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
import hai913i.tp1.model.FieldFact;
import hai913i.tp1.model.MethodFact;
import hai913i.tp1.model.TypeFact;
//A3
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import hai913i.tp1.extraction.CallVisitor;
import hai913i.tp1.model.CallFact;



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

        // A2: project structure
        List<TypeFact> types = new ArrayList<>();
        for (ParsedFile file : files) {
          file.unit().accept(new StructureVisitor(file.unit(), types));
        }
        types.sort(Comparator.comparing(TypeFact::qualifiedName));

        int nbMethods = types.stream().mapToInt(t -> t.methods().size()).sum();
        long nbCtors = types.stream().flatMap(t -> t.methods().stream())
                .filter(MethodFact::constructor).count();
        int nbFields = types.stream().mapToInt(t -> t.fields().size()).sum();

        System.out.println();
        System.out.println("===== Structure (A2) =====");
        System.out.println("Types     : " + types.size());
        System.out.println("Methodes  : " + nbMethods + " (dont " + nbCtors + " constructeurs)");
        System.out.println("Attributs : " + nbFields);

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

      List<CallFact> calls = new ArrayList<>();
      for (ParsedFile file : files) {
        file.unit().accept(new CallVisitor(file.unit(), calls));
      }

      long nbInternal = calls.stream().filter(CallFact::internal).count();
      long nbUnresolved = calls.stream().filter(c -> !c.resolved()).count();
      long nbExternal = calls.size() - nbInternal - nbUnresolved;

      System.out.println();
      System.out.println("===== Extraire les appels et le type statique du receveur (A3) =====");
      System.out.println("Appels      : " + calls.size());
      System.out.println("Internes    : " + nbInternal);
      System.out.println("Externes    : " + nbExternal);
      System.out.println("Non resolus : " + nbUnresolved);

      // Group by caller, sorted by caller id (deterministic output)
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
}
