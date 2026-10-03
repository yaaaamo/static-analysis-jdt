package hai913i.tp1.extraction;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import org.eclipse.jdt.core.dom.*;
import hai913i.tp1.model.CallFact;

/** Extracts the calls of every counted method (step A3). */
public final class CallVisitor extends ASTVisitor {

  private final CompilationUnit unit;
  private final List<CallFact> calls;
  // Ids of the counted methods we are currently inside (top = innermost)
  private final Deque<String> callers = new ArrayDeque<>();

  public CallVisitor(CompilationUnit unit, List<CallFact> calls) {
    this.unit = unit;
    this.calls = calls;
  }

  // ---- (1) Track the current caller ----

  @Override
  public boolean visit(MethodDeclaration md) {
    if (isCounted(md)) callers.push(MethodIds.of(md, ownerName(md)));
    return true;   // descend: the calls are in the body
  }

  @Override
  public void endVisit(MethodDeclaration md) {
    if (isCounted(md)) callers.pop();
  }

  // A method is counted if it belongs to a named, non-local type (same rule as A2).
  // Methods of anonymous classes are not counted: their calls go to the enclosing method.
  private static boolean isCounted(MethodDeclaration md) {
    if (!(md.getParent() instanceof AbstractTypeDeclaration t)) return false;
    ITypeBinding b = t.resolveBinding();
    return b != null && !b.isLocal();
  }

  private static String ownerName(MethodDeclaration md) {
    AbstractTypeDeclaration t = (AbstractTypeDeclaration) md.getParent();
    ITypeBinding b = t.resolveBinding();
    return b != null ? b.getQualifiedName() : t.getName().getIdentifier();
  }

  // ---- (2) The two kinds of calls ----

  @Override
  public boolean visit(MethodInvocation n) {
    if (callers.isEmpty()) return true;   // outside any counted method (field initializer): ignored
    IMethodBinding mb = n.resolveMethodBinding();
    Expression receiverExpr = n.getExpression();

    String receiver;
    if (receiverExpr != null) {
      receiver = typeName(receiverExpr.resolveTypeBinding());       // x.m(): static type of x
    } else if (mb != null && Modifier.isStatic(mb.getModifiers())) {
      receiver = typeName(mb.getDeclaringClass());                  // m() static: declaring type
    } else {
      receiver = typeName(enclosingType(n));                        // m(): implicit this
    }
    record(n.getName().getIdentifier(), mb, receiver, n);
    return true;   // descend: arguments may contain other calls, e.g. a(b())
  }

  @Override
  public boolean visit(SuperMethodInvocation n) {
    if (callers.isEmpty()) return true;
    ITypeBinding enclosing = enclosingType(n);
    String receiver = typeName(enclosing == null ? null : enclosing.getSuperclass()); // direct superclass
    record(n.getName().getIdentifier(), n.resolveMethodBinding(), receiver, n);
    return true;
  }

  // ---- (3) Build the fact ----

  private void record(String name, IMethodBinding mb, String receiver, ASTNode n) {
    boolean resolved = mb != null && !mb.isRecovered();
    String target = resolved ? MethodIds.of(mb) : null;
    boolean internal = resolved && mb.getDeclaringClass().isFromSource();
    int line = unit.getLineNumber(n.getStartPosition());
    calls.add(new CallFact(callers.peek(), name, target, receiver, line, resolved, internal));
  }

  // Nearest enclosing named type of a node
  private static ITypeBinding enclosingType(ASTNode node) {
    for (ASTNode p = node.getParent(); p != null; p = p.getParent()) {
      if (p instanceof AbstractTypeDeclaration t) return t.resolveBinding();
    }
    return null;
  }

  // Erased qualified name: java.util.List<Item> -> java.util.List
  private static String typeName(ITypeBinding t) {
    return t == null ? "(non résolu)" : t.getErasure().getQualifiedName();
  }
}
