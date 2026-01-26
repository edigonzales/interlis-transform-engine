package com.interlis.transform.mapping;

public final class JoinSpec {
    private String leftAlias;
    private String rightAlias;
    private String expr;

    public String getLeftAlias() {
        return leftAlias;
    }

    public void setLeftAlias(String leftAlias) {
        this.leftAlias = leftAlias;
    }

    public String getRightAlias() {
        return rightAlias;
    }

    public void setRightAlias(String rightAlias) {
        this.rightAlias = rightAlias;
    }

    public String getExpr() {
        return expr;
    }

    public void setExpr(String expr) {
        this.expr = expr;
    }
}
