package hai913i.tp1.model;

import java.util.List;

/** All the facts of an analysed project. Built once; B2 and B3 read only this. */
public record ProjectFacts(List<String> sourceFiles,   // analysed .java files, sorted
                           int totalLines,             // physical lines of all files (Q2)
                           List<String> packages,      // distinct package names, sorted (Q4)
                           List<TypeFact> types,       // sorted by qualified name
                           List<CallFact> calls) {}    // sorted by caller, then line