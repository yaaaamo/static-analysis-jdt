package hai913i.tp1.extraction;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import org.eclipse.jdt.core.dom.CompilationUnit;
import hai913i.tp1.model.*;
import hai913i.tp1.parse.JdtParser.ParsedFile;

/** Builds the fact model from all compilation units, in one pass. */
public final class FactExtractor {

  private FactExtractor() {}

  public static ProjectFacts extract(List<ParsedFile> parsed) throws IOException {
    // Sort files by path: createASTs does not guarantee the order of acceptAST
    List<ParsedFile> files = new ArrayList<>(parsed);
    files.sort(Comparator.comparing(f -> f.path().toString()));

    List<String> sourceFiles = new ArrayList<>();
    TreeSet<String> packages = new TreeSet<>();   // sorted, no duplicates
    int totalLines = 0;
    List<TypeFact> types = new ArrayList<>();
    List<CallFact> calls = new ArrayList<>();

    for (ParsedFile f : files) {
      CompilationUnit unit = f.unit();
      sourceFiles.add(f.path().toString());
      totalLines += Files.readAllLines(f.path()).size();   // convention of §4.2
      packages.add(unit.getPackage() == null ? "(défaut)"
              : unit.getPackage().getName().getFullyQualifiedName());

      unit.accept(new StructureVisitor(unit, types));   // pass 1: structure (A2)
      unit.accept(new CallVisitor(unit, calls));        // pass 2: calls (A3)
    }

    types.sort(Comparator.comparing(TypeFact::qualifiedName));
    calls.sort(Comparator.comparing(CallFact::callerId)
            .thenComparingInt(CallFact::line));   // keeps visit order on a line

    return new ProjectFacts(List.copyOf(sourceFiles), totalLines, List.copyOf(packages),
            List.copyOf(types), List.copyOf(calls));
  }
}