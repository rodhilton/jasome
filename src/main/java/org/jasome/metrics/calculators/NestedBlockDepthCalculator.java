package org.jasome.metrics.calculators;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.stmt.*;
import com.google.common.collect.ImmutableSet;
import org.jasome.input.Method;
import org.jasome.metrics.Calculator;
import org.jasome.metrics.Metric;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;

public class NestedBlockDepthCalculator implements Calculator<Method> {
    @Override
    public Set<Metric> calculate(Method method) {
        List<BlockStmt> blocks = method.getSource().getNodesByType(BlockStmt.class);
        List<SwitchEntryStmt> switchEntries = method.getSource().getNodesByType(SwitchEntryStmt.class);

        List<Node> allNestedBlocks = new ArrayList<>();
        allNestedBlocks.addAll(blocks);
        allNestedBlocks.addAll(switchEntries);

        OptionalInt maxDepth = allNestedBlocks.parallelStream().mapToInt(block -> {
            //figure out this block's depth and return it
            Node theNode = block;
            int i = 1;
            while (theNode != method.getSource()) {
                if (
                    theNode instanceof IfStmt
                            || theNode instanceof SwitchEntryStmt
                            || theNode instanceof SwitchStmt
                            || theNode instanceof TryStmt
                            || theNode instanceof ForStmt
                            || theNode instanceof WhileStmt
                            || theNode instanceof DoStmt
                            || theNode instanceof LambdaExpr
                            || theNode instanceof ClassOrInterfaceDeclaration
                            || theNode instanceof MethodDeclaration
                            || theNode instanceof SynchronizedStmt
                    ) {
                    //Javaparser has an interesting relationship that shows up here.. basically if you have something like an
                    //if statement, even though that "nests" 1 level, the block statement itself is a separate thing
                    //with the if statement as a parent, which means that we'd count it two.  A few other classes nest like this
                    //so we have to only increase the counter when the node we're looking at isn't one of them.  Thus we
                    //essentially whitelist the kind of statements that DO increase nesting
                    i++;
                }
                if (theNode.getParentNode().isPresent()) {
                    theNode = theNode.getParentNode().get();
                } else {
                    break;
                }

            }


            return i;
        }).max();

        return ImmutableSet.of(Metric.of("NBD", "Nested Block Depth", maxDepth.orElse(1)));
    }
}
