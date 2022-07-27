package org.ikasan.dashboard.ui.visualisation.scheduler.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TreeNode<T> {
    private T data;
    private List<TreeNode<T>> branches;
    private TreeNode<T> parent;

    public TreeNode(T data) {
        this.data = data;
        branches = new ArrayList<>();
    }

    public List<TreeNode<T>> getBranches() {
        return branches;
    }

    public TreeNode<T> addBranch(TreeNode<T> branch) {
        branch.setParent(this);
        this.branches.add(branch);
        return branch;
    }

    public TreeNode<T> getBranchBefore(TreeNode<T> branch) {
        if(this.branches.contains(branch) && this.branches.indexOf(branch) > 0) {
            return this.branches.get(this.branches.indexOf(branch)-1);
        }

        return null;
    }

    public boolean isFirstBranch(TreeNode<T> branch) {
        if(this.branches.indexOf(branch) == 0) {
            return true;
        }

        return false;
    }

    public boolean isLastBranch(TreeNode<T> branch) {
        if(this.branches.indexOf(branch) == this.branches.size()-1) {
            return true;
        }

        return false;
    }

    public TreeNode<T> getLastBranch() {
        if(!this.branches.isEmpty()) {
            return this.branches.get(branches.size()-1);
        }
        return null;
    }

    public boolean isLeaf() {
        return this.branches.isEmpty();
    }

    public T getData() {
        return data;
    }

    public TreeNode<T> getParent() {
        return parent;
    }

    public void setParent(TreeNode<T> parent) {
        this.parent = parent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TreeNode<?> treeNode = (TreeNode<?>) o;
        return data.equals(treeNode.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(data);
    }
}