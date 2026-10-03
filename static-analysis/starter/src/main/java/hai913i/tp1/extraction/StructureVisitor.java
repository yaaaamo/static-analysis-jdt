package hai913i.tp1.extraction;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jdt.core.dom.*;
import hai913i.tp1.model.*;

public final class StructureVisitor extends ASTVisitor {

  private final CompilationUnit unit;
  private final List<TypeFact> types;

  public StructureVisitor(CompilationUnit unit, List<TypeFact> types) {
    this.unit = unit;
    this.types = types;
  }

  // (1) Three node types: enum and record are NOT TypeDeclaration
  @Override public boolean visit(TypeDeclaration n) {
    return record(n, n.isInterface() ? TypeKind.INTERFACE : TypeKind.CLASS);
  }
  @Override public boolean visit(EnumDeclaration n)   { return record(n, TypeKind.ENUM); }
  @Override public boolean visit(RecordDeclaration n) { return record(n, TypeKind.RECORD); }

  private boolean record(AbstractTypeDeclaration n, TypeKind kind) {
    ITypeBinding b = n.resolveBinding();
    if (b == null || b.isLocal()) return false;   // (2) local classes are not counted

    String qName = b.getQualifiedName();           // library.service.LoanService.Loan
    String pkg = unit.getPackage() == null ? "(défaut)"
            : unit.getPackage().getName().getFullyQualifiedName();
    boolean inInterface = kind == TypeKind.INTERFACE;

    List<FieldFact> fields = new ArrayList<>();
    List<MethodFact> methods = new ArrayList<>();

    // (3) Read members ONLY from bodyDeclarations()
    for (Object o : n.bodyDeclarations()) {
      if (o instanceof FieldDeclaration fd) {
        ITypeBinding tb = fd.getType().resolveBinding();
        String type = tb != null ? tb.getQualifiedName() : fd.getType().toString();
        Visibility vis = visibility(fd.getModifiers(), inInterface);
        for (Object f : fd.fragments()) {      // int a, b; -> 2 fields
          VariableDeclarationFragment frag = (VariableDeclarationFragment) f;
          fields.add(new FieldFact(frag.getName().getIdentifier(), type, vis));
        }
      } else if (o instanceof MethodDeclaration md) {
        methods.add(method(md, qName));
      }
    }
    types.add(new TypeFact(qName, pkg, kind, superclasses(b), interfaces(b), fields, methods));
    return true;   // (4) true to descend into nested types (Loan)
  }

  private MethodFact method(MethodDeclaration md, String owner) {
    String name = md.getName().getIdentifier();
    List<String> params = new ArrayList<>();
    IMethodBinding mb = md.resolveBinding();
    if (mb != null) {
      for (ITypeBinding p : mb.getParameterTypes())
        params.add(p.getErasure().getQualifiedName());
    } else {
      for (Object p : md.parameters())
        params.add(((SingleVariableDeclaration) p).getType().toString());
    }
    //A2
    //String id = owner + "#" + name + "(" + String.join(",", params) + ")";
    //A3
    String id = MethodIds.of(md, owner);
    return new MethodFact(id, name, params, md.isConstructor(), bodyLoc(md));
  }

  private int bodyLoc(MethodDeclaration md) {
    Block body = md.getBody();
    if (body == null) return 0;    // abstract method (no body)
    int start = unit.getLineNumber(body.getStartPosition());
    int end = unit.getLineNumber(body.getStartPosition() + body.getLength() - 1);
    return end - start + 1;
  }

  private static List<String> superclasses(ITypeBinding b) {
    List<String> result = new ArrayList<>();
    for (ITypeBinding s = b.getSuperclass(); s != null; s = s.getSuperclass()) {
      String q = s.getErasure().getQualifiedName();
      if (q.equals("java.lang.Object")) break;
      result.add(q);
    }
    return result;
  }

  private static List<String> interfaces(ITypeBinding b) {
    List<String> result = new ArrayList<>();
    for (ITypeBinding i : b.getInterfaces()) result.add(i.getErasure().getQualifiedName());
    return result;
  }

  private static Visibility visibility(int mods, boolean inInterface) {
    if (Modifier.isPrivate(mods)) return Visibility.PRIVATE;
    if (Modifier.isPublic(mods) || inInterface) return Visibility.PUBLIC;
    if (Modifier.isProtected(mods)) return Visibility.PROTECTED;
    return Visibility.PACKAGE;
  }
}
