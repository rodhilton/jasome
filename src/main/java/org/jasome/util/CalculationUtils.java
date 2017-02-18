package org.jasome.util;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.Statement;

import java.util.ArrayList;
import java.util.List;

public class CalculationUtils {
    public static boolean isFieldAccessedWithinMethod(MethodDeclaration method, VariableDeclarator variable) {
        if(!method.getBody().isPresent()) return false;

        List<FieldAccessExpr> fieldAccesses = method.getBody().get().getNodesByType(FieldAccessExpr.class);

        //If we have a field match we can just count it, it's directly prefixed with 'this.' so there's no room for shadowing

        boolean anyDirectAccess = fieldAccesses.stream().anyMatch(fieldAccessExpr -> fieldAccessExpr.getName().equals(variable.getName()));

        if(anyDirectAccess) return true;
        else {
            List<NameExpr> nameAccesses = method.getBody().get().getNodesByType(NameExpr.class);
            
            boolean anyIndirectAccess = nameAccesses
                    .stream()
                    .anyMatch(nameAccessExpr -> {


                        List<Statement> allBlocksFromMethodDeclarationToNameAccessExpr = getAllVariableDefinitionScopesBetweenMethodDefinitionAndNode(nameAccessExpr);

                        //so the blocks on the path are, in order, each block stmt between the method declaration and the use of the variable

                        //so now we're going to get all of the variable definitions from the topmost block.  for each of them, we're going to get all THEIR parent
                        // block definitions, and ensure that each of them is in this list.  if any are off the list, it's a divergent path and we don't have to worry about it

                        List<VariableDeclarator> variablesDefinedInMethod = method.getNodesByType(VariableDeclarator.class);

                        boolean isVariableRedefinedInScope = variablesDefinedInMethod.stream().anyMatch(variableDeclaration-> {
                            //if any of these variables have all their parents in the allBlocks list above, then that variable shadows nameExpr (as long as the name matches)

                            if( variableDeclaration.getName().equals(nameAccessExpr.getName()) ) {
                                List<Statement> variableBlockStmts = getAllVariableDefinitionScopesBetweenMethodDefinitionAndNode(variableDeclaration);
                                return allBlocksFromMethodDeclarationToNameAccessExpr.containsAll(variableBlockStmts);
                            } else {
                                return false;
                            }
                        });


                        if(isVariableRedefinedInScope) {
                             return false;
                        } else {
                            return nameAccessExpr.getName().equals(variable.getName());
                        }
                    });

            if(anyIndirectAccess) return true;
        }


        return false;
    }

    private static List<Statement> getAllVariableDefinitionScopesBetweenMethodDefinitionAndNode(Node theNode) {
        List<Statement> blocksOnPathToMethodDeclaration = new ArrayList<>();

        while(!(theNode instanceof MethodDeclaration)) {

            if(theNode instanceof BlockStmt) {
                blocksOnPathToMethodDeclaration.add((BlockStmt)theNode);
            }

            if (theNode.getParentNode().isPresent()) {
                theNode = theNode.getParentNode().get();
            } else {
                break;
            }
        }

        return blocksOnPathToMethodDeclaration;
    }
}
