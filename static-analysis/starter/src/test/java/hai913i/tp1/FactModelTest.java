package hai913i.tp1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

import hai913i.tp1.extraction.FactExtractor;
import hai913i.tp1.model.ProjectFacts;
import hai913i.tp1.parse.JdtParser;
import hai913i.tp1.parse.ProjectSources;

/** Checkpoint B1: the numbers of A2 and A3 come from the model, deterministically. */
class FactModelTest {

  private static ProjectFacts build() throws Exception {
    ProjectSources sources = ProjectSources.of(Path.of("../resources/validation"));
    return FactExtractor.extract(JdtParser.parse(sources, List.of()));
  }

  @Test
  void modelHoldsTheA2AndA3Numbers() throws Exception {
    ProjectFacts facts = build();
    assertEquals(15, facts.types().size());
    assertEquals(59, facts.types().stream().mapToInt(t -> t.methods().size()).sum());
    assertEquals(21, facts.types().stream().mapToInt(t -> t.fields().size()).sum());
    assertEquals(94, facts.calls().size());
    assertEquals(493, facts.totalLines());
    assertEquals(4, facts.packages().size());
  }

  @Test
  void crossFileCallIsResolved() throws Exception {
    // LoanService.java calls Catalog.findById, declared in Catalog.java
    assertTrue(build().calls().stream().anyMatch(c ->
            c.callerId().startsWith("library.service.LoanService#borrow(")
                    && "library.service.Catalog#findById(java.lang.String)".equals(c.targetId())
                    && c.internal()));
  }

  @Test
  void twoRunsGiveTheSameModel() throws Exception {
    assertEquals(build(), build());
  }
}