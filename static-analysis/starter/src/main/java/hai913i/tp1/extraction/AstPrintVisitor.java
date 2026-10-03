package hai913i.tp1.extraction;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodDeclaration;

public final class AstPrintVisitor extends ASTVisitor {

  private int depth = 0;

  @Override
  public boolean preVisit2(ASTNode node) {
    System.out.println("  ".repeat(depth)
            + node.getClass().getSimpleName());

    depth++;

    return true;
  }

  @Override
  public void postVisit(ASTNode node) {
    depth--;
  }

  @Override
  public boolean visit(MethodDeclaration node) {
    System.out.println(">>> méthode : "
            + node.getName().getIdentifier());

    return true;
  }
}