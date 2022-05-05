package bantam.bantamToJavaPrettyPrinter;
import bantam.visitor.Visitor;
import bantam.ast.ASTNode;
import bantam.ast.WhileStmt;
import bantam.ast.*;




public class BantamToJavaPrettyPrinter extends Visitor {

    // the method that the users will call
    public void prettyPrint(ASTNode root) {
        return root.accept(this);
    }

    // the visit methods for the 54 subclasses of ASTNode
    public Object visit(WhileStmt node) {
        System.out.print("while (");
        node.getPredExpr().accept(this);
        System.out.print(")\n");
        node.getBodyStmt().accept(this);
        return null;
    }

    public Object visit(BinaryLogicOrExpr node) {
        node.getLeftExpr().accept(this);
        System.out.print(" || ");
        node.getRightExpr().accept(this);
        return null;
    }

    public Object visit(AssignExpr node) {
        if(node.getRefName() != null){
            System.out.print(node.getRefName());
        }
        System.out.print(" = ");

        System.out.print(node.getName());

    }

    //...same for remaining 52 subclasses of ASTNode...


// - - - - - - all the AST subclasses - - - - - -

    // in the WhileStmt class
    public Object accept(Visitor v) {
        return v.visit(this);
    }

    // in the BinaryLogicOrExpr class
    public Object accept(Visitor v) {
        return v.visit(this);
    }

//...same for remaining 53 subclasses of ASTNode...
}


