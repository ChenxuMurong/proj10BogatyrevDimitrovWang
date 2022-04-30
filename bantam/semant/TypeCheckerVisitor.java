/**
 * File: TypeCheckerVisitor.java
 * Authors: Dale Skrien, Marc Corliss,
 *          David Furcy and E Christopher Lewis
 * Date: 4/12/2022
 */
package bantam.semant;

import bantam.ast.*;
import bantam.util.ClassTreeNode;
import bantam.util.Error;
import bantam.util.ErrorHandler;
import bantam.util.SymbolTable;
import bantam.visitor.Visitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/* TODO:
    implement:
    *   ForStmt,
    *   DeclStmt, -done
    *   BreakStmt,
    *   DispatchExpr,
    *   AssignExpr,
    *   VarExpr, -done
    *   CastExpr, -confusing logic

 */

/**
 * This visitor find the types of all expression nodes and sets the type field
 * of the nodes.  It reports an error for any type incompatibility.
 */
public class TypeCheckerVisitor extends Visitor
{
    /** the current class being visited */
    private ClassTreeNode currentClass;
    /** the current method being visited */
    private Method currentMethod;
    /** the ErrorHandler that records the errors */
    private final ErrorHandler errorHandler;
    /** the current symbolTable to use for checking types */
    private SymbolTable currentSymbolTable;
    /** a stack of the current nested for or while statements
     for checking whether a break statement is inside a loop. */
    private final Stack<Stmt> currentNestedLoops;
    /** the level of the current class fields that gets updated
     * every time the program enters a new class_ */
    private int currentClassFieldLevel;

    public TypeCheckerVisitor(ErrorHandler errorHandler, ClassTreeNode root) {
        this.errorHandler = errorHandler;
        this.currentClass = root; // the Object class
        this.currentMethod = null;
        this.currentSymbolTable = null;
        this.currentNestedLoops = new Stack<>();
    }

    /*
     * CLASS INVARIANT:  Every visit method for Expr nodes sets the type field
     *                   of the Expr node being visited to a valid type.
     *                   If the node's calculated type is illegal,
     *                   an error was reported and the node's type
     *                   is set to the type it should have been or to
     *                   a generic type like "Object" so that the visits can continue.
     */

    /**
     * returns true if the first type is the same type or a subtype of the second type
     * It assumes t1 and t2 are legal types or null.  For the purpose of this
     * method, we are assuming null is a subtype of all non-primitive types.
     *
     * @param t1 the String name of the first type
     * @param t2 the String name of the second type
     * @return true if t1 is a subtype of t2
     */
    private boolean isSubtype(String t1, String t2) {
        if (t1.equals("null") && !isPrimitiveType(t2)) {
            return true;
        }
        if (t1.equals("int") || t2.equals("int")) {
            return t2.equals(t1);
        }
        if (t1.equals("boolean") || t2.equals("boolean")) {
            return t2.equals(t1);
        }
        // go up the inheritance tree of t1 to see if you
        // encounter t2
        ClassTreeNode t1Node = currentClass.lookupClass(t1);
        ClassTreeNode t2Node = currentClass.lookupClass(t2);
        while (t1Node != null) {
            if (t1Node == t2Node) {
                return true;
            }
            t1Node = t1Node.getParent();
        }
        return false;
    }

    /**
     * returns true if the given type is int or boolean
     */
    private boolean isPrimitiveType(String type) {
        return type.equals("int") || type.equals("boolean");
    }

    /**
     * returns true if the given type is a primitive type or a declared class
     */
    private boolean typeHasBeenDeclared(String type) {
        return isPrimitiveType(type) || currentClass.lookupClass(type) != null;
    }

    /**
     * register an error with the Errorhandler
     * @param node the ASTNode where the error was found
     * @param message the error message
     */
    private void registerError(ASTNode node, String message) {
        errorHandler.register(Error.Kind.SEMANT_ERROR,
                currentClass.getASTNode().getFilename(), node.getLineNum(), message);
    }

    /**
     * Visit a class node
     *
     * @param node the class node
     * @return result of the visit
     */
    public Object visit(Class_ node) {
        // set the currentClass to this class
        currentClass = currentClass.lookupClass(node.getName());
        currentSymbolTable = currentClass.getVarSymbolTable();
        // this line updates the field currentClassFieldLevel
        // to make it easier to check whether a variable is a class's field
        currentClassFieldLevel = currentSymbolTable.getCurrScopeLevel();
        node.getMemberList().accept(this);
        return null;
    }

    /**
     * Visit a field node
     *
     * @param node the field node
     * @return result of the visit
     */
    public Object visit(Field node) {
        //The fields have already been added to the symbol table by the SemanticAnalyzer,
        // so the only thing to check is the compatibility of the init expr's type with
        //the field's type.
        if (!typeHasBeenDeclared(node.getType())) {
            registerError(node,"The declared type " + node.getType() +
                    " of the field " + node.getName() + " is undefined.");
        }
        Expr initExpr = node.getInit();
        if (initExpr != null) {
            initExpr.accept(this);
            if (!isSubtype(initExpr.getExprType(), node.getType())) {
                registerError(node,"The type of the initializer is "
                        + initExpr.getExprType() + " which is not compatible with the "
                        + node.getName() + " field's type " + node.getType());
            }
        }
        //Note: if there is no initial value, then leave it with its default Java value
        return null;
    }

    /**
     * Visit a method node
     *
     * @param node the method node
     * @return result of the visit
     */
    public Object visit(Method node) {
        // is the return type a legitimate type
        if (!typeHasBeenDeclared(node.getReturnType()) && !node.getReturnType().equals(
                "void")) {
            registerError(node,"The return type " + node.getReturnType() +
                    " of the method " + node.getName() + " is undefined.");
        }

        //create a new scope for the method
        currentSymbolTable.enterScope();
        currentMethod = node;
        node.getFormalList().accept(this);
        node.getStmtList().accept(this);

        //check that non-void methods end with a return stmt
        if(! node.getReturnType().equals("void")) {
            StmtList sList = node.getStmtList();
            if (sList.getSize() == 0
                    || !(sList.get(sList.getSize() - 1) instanceof ReturnStmt)) {
                registerError(node, "Methods with non-void return type must " +
                        "end with a return statement.");
            }
        }
        currentMethod = null;
        currentSymbolTable.exitScope();
        return null;
    }

    /**
     * Visit a formal node
     *
     * @param node the formal node
     * @return result of the visit
     */
    public Object visit(Formal node) {
        if (!typeHasBeenDeclared(node.getType())) {
            registerError(node,"The declared type " + node.getType() +
                    " of the formal parameter " + node.getName() + " is undefined.");
        }
        // add it to the current scope if there isn't already a formal of the same name

        if (currentSymbolTable.getScopeLevel(node.getName()) ==
                currentSymbolTable.getCurrScopeLevel()) {
            registerError(node,"The name of the formal parameter "
                    + node.getName() + " is the same as the name of another formal" +
                    " parameter.");
        }
        currentSymbolTable.add(node.getName(), node.getType());
        return null;
    }

    /**
     * Visit a declaration statement node
     *
     * @param node the declaration statement node
     *             (Str type, Str name, Expr init)
     * @return result of the visit
     */
    public Object visit(DeclStmt node) {
        /* every var decl must be initiated.
        * so we should perform setting the type first */

        // this sets the type automatically
        node.getInit().accept(this);
        node.setType(node.getInit().getExprType());

        // null/void type gives an error
        if (node.getType().equals("null") || node.getType().equals("void")){
            registerError(node,"Initialization can't have value null or void");
            node.setType("Object");
        }

        // if the type isn't present in classMap and it isn't a primitive, register error
        if (currentClass.lookupClass(node.getType()) == null && !isPrimitiveType(node.getType())) {
            registerError(node,"The type " + node.getType() + " does not exist.");
            node.setType("Object"); // to allow analysis to continue
        }

        // If currentScopeLevel reports a duplicate name, we must check
        // whether the duplicate is in on scope-level of a class field

        // if it senses a duplicate
        if (currentSymbolTable.lookup(node.getName(),
                currentSymbolTable.getCurrScopeLevel() - 1) != null
        ) {

            // if the name duplicate is NOT in the current scope
            // (meaning it could be a class/superclass field)
            boolean duplicateIsField = false;
            if (currentSymbolTable.getScopeLevel(node.getName()) != currentSymbolTable.getCurrScopeLevel()) {

                // check for the class scope and the super class scopes.
                // 2 is like the outermost class scope level a program can have
                for (int i = currentClassFieldLevel; i >= 2; i--) {
                    if (currentSymbolTable.getScopeLevel(node.getName()) == i) {
                        duplicateIsField = true;
                    }
                }
            }

            if (!duplicateIsField) {
                registerError(node, "Variable " + node.getName()
                        + " is already defined in this scope.");
                // skip checking the rest
                return null;
            }

        }


        // add declaration to symbol table
        currentSymbolTable.add(node.getName(),node.getType());

        return null;
    }

    /**
     * Visit an if statement node
     *
     * @param node the if statement node
     * @return result of the visit
     */
    public Object visit(IfStmt node) {
        node.getPredExpr().accept(this);
        String predExprType = node.getPredExpr().getExprType();
        if (!"boolean".equals(predExprType)) {
            registerError(node,"The type of the predicate is " +
                    (predExprType != null ? predExprType : "unknown") + ", not boolean.");
        }
        currentSymbolTable.enterScope();
        node.getThenStmt().accept(this);
        currentSymbolTable.exitScope();
        if (node.getElseStmt() != null) {
            currentSymbolTable.enterScope();
            node.getElseStmt().accept(this);
            currentSymbolTable.exitScope();
        }
        return null;
    }

    /**
     * Visit a while statement node
     *
     * @param node the while statement node
     * @return result of the visit
     */
    public Object visit(WhileStmt node) {
        node.getPredExpr().accept(this);
        if (!isSubtype(node.getPredExpr().getExprType(), "boolean")) {
            registerError(node,"The type of the predicate is " +
                    node.getPredExpr().getExprType() + " which is not boolean.");
        }
        currentSymbolTable.enterScope();
        currentNestedLoops.push(node);
        node.getBodyStmt().accept(this);
        currentNestedLoops.pop();
        currentSymbolTable.exitScope();
        return null;
    }

    /**
     * Visit a for statement node
     *
     * @param node the for statement node
     * @return result of the visit
     */

    public Object visit(ForStmt node) {
        if(node.getInitExpr() != null) {
            node.getInitExpr().accept(this);
        }
        if(node.getPredExpr() != null) {
            node.getPredExpr().accept(this);
            if (!isSubtype(node.getPredExpr().getExprType(), "boolean")) {
                registerError(node,"The type of the predicate is " +
                        node.getPredExpr().getExprType() + " which is not boolean.");
            }
        }
        if(node.getUpdateExpr() != null) {
            node.getUpdateExpr().accept(this);
        }
        currentSymbolTable.enterScope();
        currentNestedLoops.push(node);
        node.getBodyStmt().accept(this);
        currentNestedLoops.pop();
        currentSymbolTable.exitScope();
        return null;
    }

    /**
     * Visit a break statement node
     *
     * @param node the break statement node
     * @return result of the visit
     */
    public Object visit(BreakStmt node) {
        if (currentNestedLoops.empty()){
            registerError(node,"Break statement not inside of a loop.");
        }
        return null;
    }

    /**
     * Visit a block statement node
     *
     * @param node the block statement node
     * @return result of the visit
     */
    public Object visit(BlockStmt node) {
        currentSymbolTable.enterScope();
        node.getStmtList().accept(this);
        currentSymbolTable.exitScope();
        return null;
    }

    /**
     * Visit a return statement node
     *
     * @param node the return statement node
     * @return result of the visit
     */
    public Object visit(ReturnStmt node) {
        if (node.getExpr() != null) {
            node.getExpr().accept(this);
            if (!isSubtype(node.getExpr().getExprType(), currentMethod.getReturnType())) {
                registerError(node,"The type of the return expr is " +
                        node.getExpr().getExprType() + " which is not compatible with the " +
                        currentMethod.getName() + " method's return type "
                        + currentMethod.getReturnType());
            }
        }
        else if (!currentMethod.getReturnType().equals("void")) {
            registerError(node, "The type of the method " + currentMethod.getName() +
                    " is not void and so return statements in it must return a value.");
        }
        return null;
    }

    /**
     * helper method that looks up the method name
     * in scope specified by "this" and sets the
     * type of the dispatchExpr node
     * @param node dispatch expression node whose
     * @return method node corrsponding to the name
     *          null if method name isn't in the scope
     * @author Baron Wang
     */
    private Method methodNodeInThisScope(DispatchExpr node){
        Method methodNode = (Method) currentClass.getMethodSymbolTable().lookup(node.getMethodName()
                ,currentClassFieldLevel-1);
        // if no method is found in the "this" scope
        if (methodNode == null) {
            registerError(node, "Method name " + node.getMethodName() +
                    " is undefined in 'this' class scope.");
            node.setExprType("Object"); // to allow analysis to continue
            return null;
            // if checking was successful,
        } else {
            // if it finds a match, then set the type of the node
            node.setExprType(methodNode.getReturnType());
            return methodNode;
        }
    }


    /**
     * helper method that looks up the method name
     * in scope specified by "super" and sets the
     * type of the dispatchExpr node
     * @param node dispatch expression node whose
     * @return method node corrsponding to the name
     *          null if method name isn't in the scope
      @author Baron Wang
     */
    private Method methodNodeInSuperScope(DispatchExpr node){
        Method methodNode = (Method) currentClass.getClassMap().get(currentSymbolTable.lookup("super"))
                .getMethodSymbolTable().lookup(node.getMethodName());
        // check if var name is in the superclass scope
        if (methodNode == null) {
            registerError(node, "Method name " + node.getMethodName() +
                    " is undefined in superclass scope.");
            node.setExprType("Object"); // to allow analysis to continue
            return null;
        } else {
            // if it passes then set the type
            node.setExprType(methodNode.getReturnType());
            return methodNode;
        }
    }

    /**
     * checks if the method call of the dispatch expr
     * is valid in respect to the reference object
     * @param node dispatch expr
     * @return
     */
    private Method methodCallValidForRefObj(DispatchExpr node){
        // check validity of method (as a method of ref obj)
        if (currentClass.lookupClass(node.getRefExpr().getExprType())
                .getMethodSymbolTable().lookup(node.getMethodName()) != null) {

            return (Method) currentClass.lookupClass(node.getRefExpr().getExprType())
                    .getMethodSymbolTable().lookup(node.getMethodName());
        } else {
            registerError(node, "Method " + node.getMethodName()
                    + " is undefined with reference object " + ((VarExpr) node.getRefExpr()).getName());
            node.setExprType("Object");
            return null;
        }
    }

    /**
     * Visit a dispatch expression node
     *
     * @param node the dispatch expression node
     * @return result of the visit
     */
    public Object visit(DispatchExpr node) {

        // this node makes it easier to compare arguments
        // after we checked for method name validity
        Method methodNode = null;

        // if has object reference, check for "this" or "super"
        if (node.getRefExpr() != null) {

            // this reference obj
            if (((VarExpr) node.getRefExpr()).getName().equals("this")) {
                // check if var name is in the class scope (with var name "this")
                methodNode = methodNodeInThisScope(node);

                // if no method is found in the "this" scope, skip the rest of function
                if (methodNode == null) {
                    return null;
                }

            } else if (((VarExpr) node.getRefExpr()).getName().equals("super")) {
                // get the type of the var in super scope
                methodNode = methodNodeInSuperScope(node);
                // check if var name is in the superclass scope
                if (methodNode == null) {
                    return null;
                }
            }
            // when reference obj of reference obj is neither this or super
            else {
                // get the reference obj of the reference obj
                String refNameOfRef = ((VarExpr) node.getRefExpr()).getRef() == null ?
                        null : ((VarExpr) ((VarExpr) node.getRefExpr()).getRef()).getName();

                // (this/super.a.b() scenario)
                if (refNameOfRef != null) {
                    // if reference obj of the reference obj isn't this or super, error out
                    if (!refNameOfRef.equals("this") && !refNameOfRef.equals("super")) {
                        registerError(node, "Illegal reference object " + refNameOfRef + "; in Bantam you can" +
                                "only do \"this\" or \"super\"");
                        node.setExprType("Object");
                        return null;
                    }

                    // check if reference object is a field
                    node.getRefExpr().accept(this);
                    // if the method name has been defined as a method of ref obj, safely populate methodNode
                    methodNode = methodCallValidForRefObj(node);
                    if (methodNode == null){
                        return null;
                    }

                    // otherwise the reference object doesn't have any refernce object (a.b() scenario)
                } else {

                    if (currentClass.getVarSymbolTable().lookup(((VarExpr) node.getRefExpr()).getName()) == null) {
                        registerError(node, "Reference object " + ((VarExpr) node.getRefExpr()).getName()
                                + " is undefined");
                        node.setExprType("Object");
                        return null;
                    }

                    node.getRefExpr().accept(this);

                    methodNode = methodCallValidForRefObj(node);
                    if (methodNode == null){
                        return null;
                    }
                }
            }

            // if no object reference
            // if method name is in the symbol table, we can safely populate methodnode
        } else if (currentClass.getMethodSymbolTable().lookup(node.getMethodName()) != null) {
            methodNode = (Method) currentClass.getMethodSymbolTable().lookup(node.getMethodName());
        }
        // if theres no object reference, and the name is not in the symbol table
        else {
            registerError(node, "Method name " + node.getMethodName() + " is undefined");
            node.setExprType("Object");
            return null;
        }

        // at this point, we are sure that the method must exist in the symbol table
        // and the method node must have been populated and not be null

        /* check arg num & type */
        // set the type for each expr in arg list
        node.getActualList().accept(this);

        ExprList actualList = node.getActualList();
        List<String> formalList = getFormalTypesList(methodNode);

        if (!dispatchArgsAreGood(node,formalList,actualList)){
            return null;
        }

        // if the program made it here, it has successfully
        // checked a dispatch expr. Set the type to return type
        node.setExprType(methodNode.getReturnType());
        return null;
    }

    /**
     * checks if the number of dispatchExpr arguments
     * is right, then checks the type of them. Returns true
     * if it passes both. False otherwise
     * @param node DispatchExpr node. Will be set to Object
     *             if it fails either args check
     * @param formalTypes list of formal types (the right types)
     * @param actualTypes list of actual types (to be checked)
     * @return true if dispatch args are good. False otherwise.
     * @author Baron Wang
     */
    private boolean dispatchArgsAreGood(DispatchExpr node, List<String> formalTypes, ExprList actualTypes){
        // check if actualList has the right size
        if (actualTypes.getSize() != formalTypes.size()) {
            registerError(node, "Method " + node.getMethodName() + " requires " +
                    formalTypes.size() + " argument(s) to work, but " +
                    " there is/are " + actualTypes.getSize());
            node.setExprType("Object");
            return false;
        }

        // check if actualTypes has the right types as the methodNode's formal list
        // at this point, actualize.getsize() == formalTypes.getsize()
        for (int i = 0; i < actualTypes.getSize(); i++) {
            // comparing formal type to actual type. If they mismatch, file error
            String formalType = formalTypes.get(i);
            String actualType = ((Expr) actualTypes.iterator().next()).getExprType();

            if (!formalType.equals(actualType)) {
                registerError(node, "Method " + node.getMethodName() + " requires "
                        + "a(n) " + formalType + " as an argument, but got a(n) " + actualType);
                node.setExprType("Object");
                return false;
            }

        }
        return true;
    }

    /**
     * returns a list of the types of the formal parameters
     *
     * @param method the methods whose formal parameter types are desired
     * @return a List of Strings (the types of the formal parameters)
     */
    private List<String> getFormalTypesList(Method method) {
        List<String> result = new ArrayList<>();
        for (ASTNode formal : method.getFormalList())
            result.add(((Formal) formal).getType());
        return result;
    }

    /**
     * Visit a list node of expressions
     *
     * @param node the expression list node
     * @return a List<String> of the types of the expressions
     */
    public Object visit(ExprList node) {
        List<String> typesList = new ArrayList<>();
        for (ASTNode expr : node) {
            expr.accept(this);
            typesList.add(((Expr) expr).getExprType());
        }
        //return a List<String> of the types of the expressions
        return typesList;
    }

    /**
     * Visit a new expression node
     *
     * @param node the new expression node
     * @return the type of the expression
     */
    public Object visit(NewExpr node) {
        if (currentClass.lookupClass(node.getType()) == null) {
            registerError(node,"The type " + node.getType() + " does not exist.");
            node.setExprType("Object"); // to allow analysis to continue
        }
        else {
            node.setExprType(node.getType());
        }
        return null;
    }

    /**
     * Visit an instanceof expression node
     *
     * @param node the instanceof expression node
     * @return the type of the expression
     */
    public Object visit(InstanceofExpr node) {
        if (currentClass.lookupClass(node.getType()) == null) {
            registerError(node,"The reference type " + node.getType()
                    + " does not exist.");
        }
        node.getExpr().accept(this);
        if (isSubtype(node.getExpr().getExprType(), node.getType())) {
            node.setUpCheck(true);
        }
        else if (isSubtype(node.getType(), node.getExpr().getExprType())) {
            node.setUpCheck(false);
        }
        else {
            registerError(node,"You can't compare type " +
                    node.getExpr().getExprType() + " to " + "incompatible type "
                    + node.getType() + ".");
        }
        node.setExprType("boolean");
        return null;
    }

    /**
     * Visit a cast expression node
     *
     *
     * @param node the cast expression node
     * @return the type of the expression
     */
    public Object visit(CastExpr node) {
        /*

         * compiler errors:

         * any cast from one class to another
         * class, which is neither a subclass
         * or a superclass, is a compiler error
         *
         * casting primitives
         *
         * the target type does not exist.
         */

        /* destination type */
        String destType = node.getType();

        /* set the type of expr */
        node.getExpr().accept(this);
        String exprType = node.getExpr().getExprType();

        // error from casting primitive type
        if (isPrimitiveType(exprType)){
            registerError(node,"Casting primitive type " +
                    exprType + " not supported in Bantam Java.");

            node.setExprType(destType);
            return null;
        }

        // error from using undeclared type.
        if (!typeHasBeenDeclared(destType)){
            registerError(node,"The destination type " + destType +
                   " is undefined.");
            node.setExprType(destType);
            return null;
        }

        // check if they are related
        else if (!isSubtype(destType, exprType)
              && !isSubtype(exprType, destType)){
            registerError(node,"Illegal cast : the expression type "
                    + exprType + " and the destination type " + destType
                    + " are not" + " subclass nor superclass of one another. ");
        }

        // check if expr is a subtype
        // of destination
        // (setting upcast field)
        if (isSubtype(exprType, destType)){
            // make sure they aren't the same
            if (!isSubtype(destType, exprType)){
                node.setUpCast(true);
            }
        }

        node.setExprType(destType);

        return null;
    }

    /**
     * Visit an assignment expression node
     *
     * @param node the assignment expression node
     * @return the type of the expression
     */
    public Object visit(AssignExpr node) {

        // set the type of the (rhs) expr
        node.getExpr().accept(this);
        String exprType = node.getExpr().getExprType();
        String lhsType = (String) currentSymbolTable.lookup(node.getName());

        // to check the validity of left hand side expression
        // if refname isn't nul, let's reuse the logic of checking
        // a VarExpr.

        // But the only way to do that is to create
        // a VarExpr of our own (lhs stands for left-hand side)

        // node with a non-null ref name
        if (node.getRefName() != null){
            VarExpr lhsExpr = new VarExpr(node.getLineNum(),
                    new VarExpr(node.getLineNum(), null, node.getRefName()),
                    node.getName());

            // check if lhs is valid
            boolean lhsIsValid = setNodesTypeWithRef(lhsExpr);
            if (!lhsIsValid){
                registerError(node,"The left hand expression \"" + node.getRefName() +
                        "." + node.getName() + "\" in this assignment is invalid");
                node.setExprType(exprType);
                return null;
            }
        }
        // node without ref name
        else{
            // if unable to find the name in symbol table
            if (lhsType == null){
                registerError(node,"The left hand expression \"" + node.getName() +
                        "\" in this assignment is undefined");
                node.setExprType(exprType);
                return null;
            }
            // if the name has been defined, we do nothing and carry on checking

        }

        // type checking
        // if rhs is NOT a subtype of lhs (e.g. tiger = new Animal())
        // and if rhs isn't "null"
        if (!isSubtype(exprType,lhsType) && !"null".equals(exprType)){
            registerError(node,"The right hand side type " +
                    exprType + " does not conform to the left hand side type "+
                    lhsType);
            node.setExprType(exprType);
            return null;
        }

        node.setExprType(exprType);
        return null;

    }

    /**
     * helper method that looks up a String and
     * figures out if it is a class field or not
     * @param name str to be looked up
     * @return type or null (if lookup failed)
     * @author Baron Wang
     */
    private String fieldType(String name){
        return (String) currentSymbolTable.lookup(name,currentClassFieldLevel - 1);
    }

    /**
     *
     * helper method to set the type of the VarExpr node with
     * a ref name (ONLY WORKS WHEN NODE HAS A NON-NULL REF NAME)
     *
     * @param node VarExpr node whose type needs to be set
     * @return boolean - whether type setting was successful
     * @author Baron Wang
     */
    private boolean setNodesTypeWithRef(VarExpr node){
        if (((VarExpr) node.getRef()).getName().equals("this")) {
            // check if var name is in the class scope (with var name "this")
            // saving the type to a var.
            String typeStr;
            // currentClassFieldLevel could be the same as the current level
            // need to subtract one for it to work
            typeStr = fieldType(node.getName());

            // if it can't find the node name, or the node name isn't in the same scope as this class:
            if (typeStr == null ) {
                registerError(node, "Identifier " + node.getName() +
                        " is undefined in this class scope.");
                node.setExprType("Object"); // to allow analysis to continue
                return false;
                // if checking was successful
            }else{
                node.setExprType(typeStr);
                return true;
            }

        } else if (((VarExpr) node.getRef()).getName().equals("super")) {
            // get the type of the var in super scope
            String typeStr = (String) currentClass.getClassMap().get(currentSymbolTable.lookup("super"))
                    .getVarSymbolTable().lookup(node.getName());
            // check if var name is in the superclass scope
            if (typeStr == null ) {
                registerError(node, "Identifier " + node.getName() +
                        " is undefined in superclass scope.");
                node.setExprType("Object"); // to allow analysis to continue
                return false;
            }else{
                node.setExprType(typeStr);
                return true;
            }
        }
        else{
            registerError(node, "Invalid reference object "
                    + ((VarExpr) node.getRef()).getName() + "; in Bantam you can" +
                    "only do \"this\" or \"super\"");
            node.setExprType("Object");
            return false;
        }
    }

    /**
     * Visit a variable expression node
     *
     * @param node the variable expression node
     * @return the type of the expression
     */
    public Object visit(VarExpr node) {

        // if it has a "this" or "super"
        if (node.getRef() != null) {
            boolean success;
            success = setNodesTypeWithRef(node);
            if (!success){
                // if type setting wasn't successful,
                // halt the function and return null
                return null;
            }
        }

        // case: empty object reference
        // first check if identifier name is null
        if (node.getName().equals("null")){
            node.setExprType("null");
            return null;
        }
        // check if identifier name has been defined before
        else if (currentSymbolTable.lookup(node.getName(),
                currentSymbolTable.getCurrScopeLevel() - 1) == null
        )
        {
            registerError(node,"Identifier " + node.getName() +
                    " hasn't been defined yet");
            node.setExprType("Object"); // to allow analysis to continue
            return null;
        }

        node.setExprType((String) currentSymbolTable.lookup(node.getName()));
        return null;
    }

    /**
     * returns an array of length 2 containing the types of
     * the left and right children of the node.
     * @param node The BinaryExpr whose children are to be typed
     * @return A String[] of length 2 with the types of the 2 children
     */
    private String[] getLeftAndRightTypes(BinaryExpr node) {
        node.getLeftExpr().accept(this);
        node.getRightExpr().accept(this);
        String type1 = node.getLeftExpr().getExprType();
        String type2 = node.getRightExpr().getExprType();
        return new String[]{type1,type2};
    }

    /**
     * Visit a binary comparison equals expression node
     *
     * @param node the binary comparison equals expression node
     * @return the type of the expression
     */
    public Object visit(BinaryCompEqExpr node) {
        String[] types = getLeftAndRightTypes(node);
        if (types[0] == null || types[1] == null) {
            return null; //error in one expr, so skip further checking
        }
        if (!(isSubtype(types[0], types[1]) || isSubtype(types[1], types[0]))) {
            registerError(node,"The " + "two values being compared for " +
                    "equality are not compatible types.");
        }
        node.setExprType("boolean");
        return null;
    }

    /**
     * Visit a binary comparison not equals expression node
     *
     * @param node the binary comparison not equals expression node
     * @return the type of the expression
     */
    public Object visit(BinaryCompNeExpr node) {
        String[] types = getLeftAndRightTypes(node);
        if (!(isSubtype(types[0], types[1]) || isSubtype(types[1], types[0]))) {
            registerError(node,"The two values being compared for equality " +
                    "are not compatible types.");
        }
        node.setExprType("boolean");
        return null;
    }

    /**
     * Visit a binary comparison less than expression node
     *
     * @param node the binary comparison less than expression node
     * @return the type of the expression
     */
    public Object visit(BinaryCompLtExpr node) {
        String[] types = getLeftAndRightTypes(node);
        if (!(types[0].equals("int") && types[1].equals("int"))) {
            registerError(node,"The two values being compared by \"<\" are " +
                    "not both ints.");
        }
        node.setExprType("boolean");
        return null;
    }

    /**
     * Visit a binary comparison less than or equal to expression node
     *
     * @param node the binary comparison less than or equal to expression node
     * @return the type of the expression
     */
    public Object visit(BinaryCompLeqExpr node) {
        String[] types = getLeftAndRightTypes(node);
        if (!(types[0].equals("int") && types[1].equals("int"))) {
            registerError(node,"The  two values being compared by \"<=\" are" +
                    " not both ints.");
        }
        node.setExprType("boolean");
        return null;
    }

    /**
     * Visit a binary comparison greater than expression node
     *
     * @param node the binary comparison greater than expression node
     * @return the type of the expression
     */
    public Object visit(BinaryCompGtExpr node) {
        String[] types = getLeftAndRightTypes(node);
        if (!(types[0].equals("int") && types[1].equals("int"))) {
            registerError(node,"The two values being compared by \">\" are" +
                    " not both ints.");
        }
        node.setExprType("boolean");
        return null;
    }

    /**
     * Visit a binary comparison greater than or equal to expression node
     *
     * @param node the binary comparison greater to or equal to expression node
     * @return the type of the expression
     */
    public Object visit(BinaryCompGeqExpr node) {
        String[] types = getLeftAndRightTypes(node);
        if (!(types[0].equals("int") && types[1].equals("int"))) {
            registerError(node,"The  two values being compared by \">=\" are " +
                    "not both ints.");
        }
        node.setExprType("boolean");
        return null;
    }

    /**
     * Visit a binary arithmetic plus expression node
     *
     * @param node the binary arithmetic plus expression node
     * @return the type of the expression
     */
    public Object visit(BinaryArithPlusExpr node) {
        String[] types = getLeftAndRightTypes(node);
        if (!(types[0].equals("int") && types[1].equals("int"))) {
            registerError(node,"The two values being added are not both ints.");
        }
        node.setExprType("int");
        return null;
    }

    /**
     * Visit a binary arithmetic minus expression node
     *
     * @param node the binary arithmetic minus expression node
     * @return the type of the expression
     */
    public Object visit(BinaryArithMinusExpr node) {
        String[] types = getLeftAndRightTypes(node);
        if (!(types[0].equals("int") && types[1].equals("int"))) {
            registerError(node,"The two values being subtraced are not both ints.");
        }
        node.setExprType("int");
        return null;
    }

    /**
     * Visit a binary arithmetic times expression node
     *
     * @param node the binary arithmetic times expression node
     * @return the type of the expression
     */
    public Object visit(BinaryArithTimesExpr node) {
        String[] types = getLeftAndRightTypes(node);
        if (!(types[0].equals("int") && types[1].equals("int"))) {
            registerError(node,"The two values being multiplied are not both ints.");
        }
        node.setExprType("int");
        return null;
    }

    /**
     * Visit a binary arithmetic divide expression node
     *
     * @param node the binary arithmetic divide expression node
     * @return the type of the expression
     */
    public Object visit(BinaryArithDivideExpr node) {
        String[] types = getLeftAndRightTypes(node);
        if (!(types[0].equals("int") && types[1].equals("int"))) {
            registerError(node,"The two values being divided are not both ints.");
        }
        node.setExprType("int");
        return null;
    }

    /**
     * Visit a binary arithmetic modulus expression node
     *
     * @param node the binary arithmetic modulus expression node
     * @return the type of the expression
     */
    public Object visit(BinaryArithModulusExpr node) {
        String[] types = getLeftAndRightTypes(node);
        if (!(types[0].equals("int") && types[1].equals("int"))) {
            registerError(node,"The two values being operated on with % are " +
                    "not both ints.");
        }
        node.setExprType("int");
        return null;
    }

    /**
     * Visit a binary logical AND expression node
     *
     * @param node the binary logical AND expression node
     * @return the type of the expression
     */
    public Object visit(BinaryLogicAndExpr node) {
        String[] types = getLeftAndRightTypes(node);
        if (!(types[0].equals("boolean") && types[1].equals("boolean"))) {
            registerError(node,"The two values being operated on with && are not both booleans" + ".");
        }
        node.setExprType("boolean");
        return null;
    }

    /**
     * Visit a binary logical OR expression node
     *
     * @param node the binary logical OR expression node
     * @return the type of the expression
     */
    public Object visit(BinaryLogicOrExpr node) {
        String[] types = getLeftAndRightTypes(node);
        if (!(types[0].equals("boolean") && types[1].equals("boolean"))) {
            registerError(node,"The two values being operated on with || are not both booleans" + ".");
        }
        node.setExprType("boolean");
        return null;
    }

    /**
     * Visit a unary negation expression node
     *
     * @param node the unary negation expression node
     * @return the type of the expression
     */
    public Object visit(UnaryNegExpr node) {
        node.getExpr().accept(this);
        String type = node.getExpr().getExprType();
        if (!(type.equals("int"))) {
            registerError(node,"The value being negated is of type "
                    + type + ", not int.");
        }
        node.setExprType("int");
        return null;
    }

    /**
     * Visit a unary NOT expression node
     *
     * @param node the unary NOT expression node
     * @return the type of the expression
     */
    public Object visit(UnaryNotExpr node) {
        node.getExpr().accept(this);
        String type = node.getExpr().getExprType();
        if (!type.equals("boolean")) {
            registerError(node,"The not (!) operator applies only to boolean " +
                    "expressions, not " + type + " expressions.");
        }
        node.setExprType("boolean");
        return null;
    }

    /**
     * Visit a unary increment expression node
     *
     * @param node the unary increment expression node
     * @return the type of the expression
     */
    public Object visit(UnaryIncrExpr node) {
        if (!(node.getExpr() instanceof VarExpr)) {
            registerError(node,"The  expression being incremented can only be " +
                    "a variable name with an optional \"this.\" or \"super.\" prefix.");
        }
        node.getExpr().accept(this);
        String type = node.getExpr().getExprType();
        if (!(type.equals("int"))) {
            registerError(node,"The value being incremented is of type "
                    + type + ", not int.");
        }
        node.setExprType("int");
        return null;
    }

    /**
     * Visit a unary decrement expression node
     *
     * @param node the unary decrement expression node
     * @return the type of the expression
     */
    public Object visit(UnaryDecrExpr node) {
        if (!(node.getExpr() instanceof VarExpr)) {
            registerError(node,"The  expression being incremented can only be " +
                    "a variable name with an optional \"this.\" or \"super.\" prefix.");
        }
        node.getExpr().accept(this);
        String type = node.getExpr().getExprType();
        if (!(type.equals("int"))) {
            registerError(node,"The value being decremented is of type "
                    + type + ", not int.");
        }
        node.setExprType("int");
        return null;
    }

    /**
     * Visit an int constant expression node
     *
     * @param node the int constant expression node
     * @return the type of the expression
     */
    public Object visit(ConstIntExpr node) {
        node.setExprType("int");
        return null;
    }

    /**
     * Visit a boolean constant expression node
     *
     * @param node the boolean constant expression node
     * @return the type of the expression
     */
    public Object visit(ConstBooleanExpr node) {
        node.setExprType("boolean");
        return null;
    }

    /**
     * Visit a string constant expression node
     *
     * @param node the string constant expression node
     * @return the type of the expression
     */
    public Object visit(ConstStringExpr node) {
        node.setExprType("String");
        return null;
    }

}