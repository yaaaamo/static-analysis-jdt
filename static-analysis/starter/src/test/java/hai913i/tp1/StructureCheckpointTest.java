package hai913i.tp1;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import hai913i.tp1.extraction.StructureVisitor;
import hai913i.tp1.model.FieldFact;
import hai913i.tp1.model.MethodFact;
import hai913i.tp1.model.TypeFact;
import hai913i.tp1.model.TypeKind;
import hai913i.tp1.model.Visibility;
import hai913i.tp1.parse.JdtParser;
import hai913i.tp1.parse.JdtParser.ParsedFile;
import hai913i.tp1.parse.ProjectSources;

/** Checkpoint A2 on the validation project. */
class StructureCheckpointTest {

  private static List<TypeFact> types;

  // Runs once before all tests: parse the validation project and extract the structure
  @BeforeAll
  static void extract() throws Exception {
    ProjectSources sources = ProjectSources.of(Path.of("../resources/validation"));
    List<ParsedFile> files = JdtParser.parse(sources, List.of());
    types = new ArrayList<>();
    for (ParsedFile f : files) {
      f.unit().accept(new StructureVisitor(f.unit(), types));
    }
  }

  private static TypeFact type(String qualifiedName) {
    return types.stream()
            .filter(t -> t.qualifiedName().equals(qualifiedName))
            .findFirst()
            .orElseThrow(() -> new AssertionError("type not found: " + qualifiedName));
  }

  private static long countKind(TypeKind kind) {
    return types.stream().filter(t -> t.kind() == kind).count();
  }

  @Test
  void countsTypesByKind() {
    assertEquals(15, types.size());
    assertEquals(12, countKind(TypeKind.CLASS));
    assertEquals(2, countKind(TypeKind.INTERFACE));
    assertEquals(1, countKind(TypeKind.ENUM));
  }

  @Test
  void countsMethodsAndConstructors() {
    assertEquals(59, types.stream().mapToInt(t -> t.methods().size()).sum());
    assertEquals(9, types.stream().flatMap(t -> t.methods().stream())
            .filter(MethodFact::constructor).count());
  }

  @Test
  void countsFields() {
    assertEquals(21, types.stream().mapToInt(t -> t.fields().size()).sum());
  }

  @Test
  void nestedClassMembersAreNotMerged() {
    TypeFact service = type("library.service.LoanService");
    TypeFact loan = type("library.service.LoanService.Loan");
    assertEquals(6, service.methods().size());
    assertEquals(5, service.fields().size());
    assertEquals(4, loan.methods().size());
    assertEquals(3, loan.fields().size());
  }

  @Test
  void superclassChainComesFromBindings() {
    assertEquals(List.of("java.lang.Exception", "java.lang.Throwable"),
            type("library.service.LoanException").superclasses());
  }

  @Test
  void itemFieldVisibilities() {
    Map<String, Visibility> vis = type("library.model.Item").fields().stream()
            .collect(Collectors.toMap(FieldFact::name, FieldFact::visibility));
    assertEquals(Map.of(
            "MAX_LOAN_DAYS", Visibility.PUBLIC,
            "id", Visibility.PROTECTED,
            "title", Visibility.PROTECTED,
            "borrower", Visibility.PRIVATE,
            "category", Visibility.PACKAGE), vis);
  }

  @Test
  void overloadsHaveDistinctIds() {
    long addIds = type("library.service.Catalog").methods().stream()
            .filter(m -> m.name().equals("add"))
            .map(MethodFact::id)
            .distinct()
            .count();
    assertEquals(2, addIds);
  }
}
