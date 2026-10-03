package hai913i.tp1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import hai913i.tp1.extraction.CallVisitor;
import hai913i.tp1.model.CallFact;
import hai913i.tp1.parse.JdtParser;
import hai913i.tp1.parse.JdtParser.ParsedFile;
import hai913i.tp1.parse.ProjectSources;

/** Checkpoint A3 on the validation project and on robustness/missing-dep. */
class CallCheckpointTest {

  private static List<CallFact> calls;

  @BeforeAll
  static void extract() throws Exception {
    calls = extractCalls("../resources/validation");
  }

  // Parses a project and runs the CallVisitor on every compilation unit
  private static List<CallFact> extractCalls(String project) throws Exception {
    ProjectSources sources = ProjectSources.of(Path.of(project));
    List<ParsedFile> files = JdtParser.parse(sources, List.of());
    List<CallFact> result = new ArrayList<>();
    for (ParsedFile f : files) {
      f.unit().accept(new CallVisitor(f.unit(), result));
    }
    return result;
  }

  // Finds one call by caller id, line and called method name
  private static CallFact call(List<CallFact> in, String callerId, int line, String name) {
    return in.stream()
            .filter(c -> c.callerId().equals(callerId) && c.line() == line && c.name().equals(name))
            .findFirst()
            .orElseThrow(() -> new AssertionError("call not found: " + callerId + " l." + line + " " + name));
  }

  @Test
  void countsCalls() {
    long internal = calls.stream().filter(CallFact::internal).count();
    long unresolved = calls.stream().filter(c -> !c.resolved()).count();
    assertEquals(94, calls.size());
    assertEquals(46, internal);
    assertEquals(48, calls.size() - internal - unresolved);
    assertEquals(0, unresolved);
  }

  @Test
  void receiverIsTheStaticTypeNotTheObjectType() {
    CallFact c = call(calls,
            "library.service.LoanService#borrow(library.model.Member,java.lang.String,int)", 36, "checkOut");
    assertEquals("library.model.Loanable", c.staticReceiverType());
    assertEquals("library.model.Loanable#checkOut(library.model.Member)", c.targetId());
  }

  @Test
  void inheritedMethodTargetsTheDeclaringClass() {
    CallFact c = call(calls, "library.app.LibraryApp#main(java.lang.String[])", 32, "getTitle");
    assertEquals("library.model.Book", c.staticReceiverType());
    assertEquals("library.model.Item#getTitle()", c.targetId());
  }

  @Test
  void superCallReceiverIsTheDirectSuperclass() {
    CallFact c = call(calls, "library.model.Book#describe()", 32, "describe");
    assertEquals("library.model.Item", c.staticReceiverType());
    assertEquals("library.model.Item#describe()", c.targetId());
  }

  @Test
  void callInLambdaBelongsToEnclosingMethod() {
    CallFact c = call(calls, "library.service.Catalog#available()", 37, "isAvailable");
    assertEquals("library.model.Item", c.staticReceiverType());
  }

  @Test
  void missingDependencyIsReportedButAnalysisContinues() throws Exception {
    List<CallFact> robust = extractCalls("../resources/robustness/missing-dep");
    CallFact capitalize = robust.stream().filter(c -> c.name().equals("capitalize"))
            .findFirst().orElseThrow();
    CallFact decorate = robust.stream().filter(c -> c.name().equals("decorate"))
            .findFirst().orElseThrow();

    assertFalse(capitalize.resolved());
    assertNull(capitalize.targetId());
    assertTrue(decorate.resolved());
    assertEquals("demo.Formatter#decorate(java.lang.String)", decorate.targetId());
  }

  @Test
  void twoRunsGiveTheSameCalls() throws Exception {
    assertEquals(calls, extractCalls("../resources/validation"));
  }
}
