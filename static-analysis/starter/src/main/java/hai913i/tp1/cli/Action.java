package hai913i.tp1.cli;

/** What the tool should produce. */
public enum Action {
  AST, STRUCTURE, CALLS, METRICS, GRAPH, ALL;

  static Action parse(String text) throws UsageException {
    for (Action a : values()) {
      if (a.name().equalsIgnoreCase(text)) return a;
    }
    throw new UsageException("action inconnue : " + text);
  }

  /** Q11 needs the threshold X. */
  boolean needsX() {
    return this == METRICS || this == ALL;
  }
}