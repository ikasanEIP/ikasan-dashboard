package org.ikasan.dashboard.ui.visualisation.scheduler.model;

import org.ikasan.designer.model.Rectangle;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class Tree<T> {
    private TreeNode<T> root;

    public Tree(TreeNode<T> root) {
        this.root = root;
    }

    public TreeNode<T> getRoot() {
        return root;
    }

    public boolean containsTree(Tree<T> otherTree) {
        AtomicBoolean containsTree = new AtomicBoolean(false);

        if(this.root.getData().equals(otherTree.root.getData())) {
            // same tree so cannot contain the other tree.
            return false;
        }

        this.root.getBranches().forEach(branch -> {
            if(branch.getData().equals(otherTree.root.getData())) {
                containsTree.set(true);
            }
            else {
                if(!branch.isLeaf() && !containsTree.get()) {
                    if(this.branchContainsTree(branch, otherTree))
                        containsTree.set(true);
                }
            }
        });

        return containsTree.get();
    }

    private boolean branchContainsTree(TreeNode<T> branch, Tree<T> otherTree) {
        AtomicBoolean containsTree = new AtomicBoolean(false);

        branch.getBranches().forEach(child -> {
            if(child.getData().equals(otherTree.root.getData())) {
                containsTree.set(true);
            }
            else {
                if(!child.isLeaf() && !containsTree.get()) {
                    if(this.branchContainsTree(child, otherTree))
                        containsTree.set(true);
                }
            }
        });

        return containsTree.get();
    }

    public void pruneTree() {
        this.pruneTree(this.getRoot());
    }

    private void pruneTree(TreeNode<T> node) {
        if(!node.isLeaf()) {
            List<TreeNode<T>> trimmedBranches = new ArrayList<>();
            trimmedBranches.addAll(node.getBranches());

            node.getBranches().forEach(branch -> {
                if (branch.isLeaf()) {

                    node.getBranches().forEach(b -> {

                        if (!branch.equals(b) && !b.isLeaf()) {
                            if (this.isBranchInLowerBranches(branch, b.getBranches())) {
                                trimmedBranches.remove(branch);
                            }
                        }
                    });

                }
            });

            node.getBranches().clear();
            node.getBranches().addAll(trimmedBranches);
        }
    }

    private boolean isBranchInLowerBranches(TreeNode<T> branch, List<TreeNode<T>> lowerBranches) {
        AtomicBoolean isInBranches = new AtomicBoolean(false);

        lowerBranches.forEach(node -> {
            if(branch.getData().equals(node.getData())) {
                isInBranches.set(true);
            }

            if(!isInBranches.get() && !node.isLeaf()) {
                this.isBranchInLowerBranches(branch, node.getBranches());
            }
        });

        return isInBranches.get();
    }
}

