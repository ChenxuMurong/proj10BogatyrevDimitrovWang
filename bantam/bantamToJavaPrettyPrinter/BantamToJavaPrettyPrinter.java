package bantam.bantamToJavaPrettyPrinter;
import bantam.visitor.Visitor;
import bantam.ast.ASTNode;
import bantam.ast.WhileStmt;
import bantam.ast.*;




public class BantamToJavaPrettyPrinter extends Visitor {

    // the method that the users will call
    public void prettyPrint(ASTNode root) {
        root.accept(this);
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

        System.out.print(";");

        return null;

    }

    public Object visit(BinaryArithDivideExpr node) {
        node.getLeftExpr().accept(this);
        System.out.print(" / ");
        node.getRightExpr().accept(this);
        return null;
    }


    public Object visit(BinaryCompEqExpr node) {
        node.getLeftExpr().accept(this);
        System.out.print(" == ");
        node.getRightExpr().accept(this);
        return null;
    }

    public Object visit(BinaryArithMinusExpr node) {
        node.getLeftExpr().accept(this);
        System.out.print(" - ");
        node.getRightExpr().accept(this);
        return null;
    }

    public Object visit(BinaryArithModulusExpr node) {
        node.getLeftExpr().accept(this);
        System.out.print(" % ");
        node.getRightExpr().accept(this);
        return null;
    }

    public Object visit(BinaryArithPlusExpr node) {
        node.getLeftExpr().accept(this);
        System.out.print(" + ");
        node.getRightExpr().accept(this);
        return null;
    }

    public Object visit(BinaryArithTimesExpr node) {
        node.getLeftExpr().accept(this);
        System.out.print(" * ");
        node.getRightExpr().accept(this);
        return null;
    }

    public Object visit(BinaryCompGeqExpr node) {
        node.getLeftExpr().accept(this);
        System.out.print(" >= ");
        node.getRightExpr().accept(this);
        return null;
    }

    public Object visit(BinaryCompGtExpr node) {
        node.getLeftExpr().accept(this);
        System.out.print(node.getOpName());
        node.getRightExpr().accept(this);
        return null;
    }

    public Object visit(BinaryCompLeqExpr node) {
        node.getLeftExpr().accept(this);
        System.out.print(node.getOpName());
        node.getRightExpr().accept(this);
        return null;
    }

    public Object visit(BinaryCompLtExpr node) {
        node.getLeftExpr().accept(this);
        System.out.print(node.getOpName());
        node.getRightExpr().accept(this);
        return null;
    }

    public Object visit(BinaryCompNeExpr node) {
        node.getLeftExpr().accept(this);
        System.out.print(node.getOpName());
        node.getRightExpr().accept(this);
        return null;
    }

    public Object visit(BinaryLogicAndExpr node) {
        node.getLeftExpr().accept(this);
        System.out.print(node.getOpName());
        node.getRightExpr().accept(this);
        return null;
    }

    public Object visit(BreakStmt node) {
        System.out.print("break; \n");
        return null;
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


