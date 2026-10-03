package hai913i.tp1;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jdt.core.compiler.IProblem;

import hai913i.tp1.callgraph.CallGraph;
import hai913i.tp1.cli.Action;
import hai913i.tp1.cli.Options;
import hai913i.tp1.cli.UsageException;
import hai913i.tp1.extraction.AstPrintVisitor;
import hai913i.tp1.extraction.FactExtractor;
import hai913i.tp1.model.ProjectFacts;
import hai913i.tp1.parse.JdtParser;
import hai913i.tp1.parse.JdtParser.ParsedFile;
import hai913i.tp1.parse.ProjectSources;
import hai913i.tp1.report.TextReport;

/** Command-line entry point: reads the options, checks the input, delegates. Computes nothing. */
public final class Main {

  private Main() {
  }

  public static void main(String[] args) {
    try {
      System.exit(run(args));
    } catch (Exception e) {
      // Last safety net: a clear message, never a raw stack trace
      System.err.println("Erreur inattendue : " + e.getMessage());
      System.exit(1);
    }
  }

  static int run(String[] args) throws IOException {
    if (args.length > 0 && (args[0].equals("-h") || args[0].equals("--help"))) {
      System.out.println(Options.USAGE);
      return 0;
    }

    // 1. Command line
    Options opt;
    try {
      opt = Options.parse(args);
    } catch (UsageException e) {
      System.err.println("Erreur : " + e.getMessage());
      System.err.println(Options.USAGE);
      return 2;
    }

    // 2. Input checks: the analysis is impossible -> message + non-zero code
    if (!Files.isDirectory(opt.project())) {
      System.err.println("Erreur : dossier introuvable : " + opt.project());
      return 3;
    }
    ProjectSources sources = ProjectSources.of(opt.project());
    if (sources.javaFiles().isEmpty()) {
      System.err.println("Erreur : aucun fichier .java sous " + sources.sourceRoot());
      return 4;
    }

    // 3. Parse (A0); compilation errors are reported but do not stop the analysis
    List<ParsedFile> files = new ArrayList<>(JdtParser.parse(sources, List.of()));
    files.sort(Comparator.comparing(f -> f.path().toString()));
    int errors = reportCompilationErrors(files);
    System.out.println("Racine des sources     : " + sources.sourceRoot());
    System.out.println("Unites de compilation  : " + files.size());
    System.out.println("Erreurs de compilation : " + errors);
    if (errors > 0) {
      System.out.println("Attention : des fichiers ne compilent pas ; l'analyse continue sur ce qui a pu etre lu.");
    }

    // 4. Delegate according to the action
    Action action = opt.action();
    if (action == Action.AST) {
      printAst(files, opt.astFile());
      return 0;
    }
    ProjectFacts facts = FactExtractor.extract(files);
    if (action == Action.STRUCTURE || action == Action.ALL) TextReport.printStructure(facts);
    if (action == Action.CALLS || action == Action.ALL)     TextReport.printCalls(facts);
    if (action == Action.METRICS || action == Action.ALL)   TextReport.printMetrics(facts, opt.x());
    if (action == Action.GRAPH || action == Action.ALL)     TextReport.printGraph(CallGraph.build(facts), opt.method());
    return 0;
  }

  private static int reportCompilationErrors(List<ParsedFile> files) {
    int errors = 0;
    for (ParsedFile file : files) {
      for (IProblem problem : file.unit().getProblems()) {
        if (problem.isError()) {
          errors++;
          System.err.println(file.path().getFileName() + ":" + problem.getSourceLineNumber()
                  + " " + problem.getMessage());
        }
      }
    }
    return errors;
  }

  // A1: print the AST of one file, or of Dvd.java and Category.java by default
  private static void printAst(List<ParsedFile> files, String fileName) {
    for (ParsedFile file : files) {
      String name = file.path().getFileName().toString();
      boolean wanted = fileName == null
              ? name.equals("Dvd.java") || name.equals("Category.java")
              : name.equals(fileName);
      if (wanted) {
        System.out.println();
        System.out.println("===== AST de " + name + " =====");
        file.unit().accept(new AstPrintVisitor());
      }
    }
  }
}