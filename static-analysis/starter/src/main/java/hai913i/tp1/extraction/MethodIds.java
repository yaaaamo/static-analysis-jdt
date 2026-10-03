package hai913i.tp1.extraction;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.jdt.core.dom.*;

/** Builds stable method ids: qualified.Type#name(param,types). Used by A2 and A3. */
public final class MethodIds {

  private MethodIds() {}

  /** Id from a binding (call targets and declared methods). */
  public static String of(IMethodBinding mb) {
    IMethodBinding decl = mb.getMethodDeclaration();   // List<Item>.add(Item) -> List.add(E)
    String owner = decl.getDeclaringClass().getErasure().getQualifiedName();
    List<String> params = new ArrayList<>();
    for (ITypeBinding p : decl.getParameterTypes()) {
      params.add(p.getErasure().getQualifiedName());   // E -> java.lang.Object
    }
    return owner + "#" + decl.getName() + "(" + String.join(",", params) + ")";
  }

  /** Id of a declared method, with a fallback when the binding is missing. */
  public static String of(MethodDeclaration md, String owner) {
    IMethodBinding mb = md.resolveBinding();
    if (mb != null) return of(mb);
    List<String> params = new ArrayList<>();
    for (Object p : md.parameters()) {
      params.add(((SingleVariableDeclaration) p).getType().toString());
    }
    return owner + "#" + md.getName().getIdentifier() + "(" + String.join(",", params) + ")";
  }
}
