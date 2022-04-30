/*
    checks if an AST node
    has a main method
    within a Main class
 */

package bantam.semant;

import bantam.ast.ASTNode;
import bantam.ast.Class_;
import bantam.ast.Field;
import bantam.ast.Method;
import bantam.visitor.Visitor;

/**
 * contains hasMain method that checks if an
 * AST node contains a void method with no
 * args called "main"
 *
 * @author Baron Wang
 */
public class MainMethodFinder extends Visitor {



    // chenge this to true if found void main()
    private boolean hasMainMethod;

    /**
     * it only returns true if it
     * finds a void method with no
     * parameters that is named "main"
     * and is in a class named "Main"
     *
     * @param rootNode root of AST to check
     *                 (apparently it's always
     *                 going to be a Program)
     * @return whether or not AST has
     *  main method and a main class
     * @author Baron Wang
     */
    public boolean hasMain(ASTNode rootNode){
        rootNode.accept(this);
        return hasMainMethod;
    }

    /**
     * sets hasMainClass to true if class is Main
     * @param klass a class ASTNode, misspelled to
     *              avoid confusion with keyword class
     * @return null
     */
    @Override
    public Object visit(Class_ klass){
        if (klass.getName().equals("Main")){
            // iterate thru the member list to
            // check for main class

             klass.getMemberList().accept(this);
//            for (ASTNode astNode : klass.getMemberList()){
//                // if the member is Method
//                // (only methods are worth checking)
//                if (astNode instanceof Method){
//                    // make it accept a visit
//                    astNode.accept(this);
//                }
//            }
        }
        return null;
    }


    /**
     *     // sets hasMainMethod to true if method is void main()
     *     // otherwise files error
     * @param method a method AST Node
     * @return null
     */
    @Override
    public Object visit(Method method){
        if (method.getReturnType().equals("void")
         && method.getName().equals("main")
         && method.getFormalList().getSize() == 0){
            hasMainMethod = true;
        }

        return null;
    }

    /**
     * no need to visit field node
     * @param node field node
     * @return null
     */
    public Object visit(Field node){
        return null;
    }


}
