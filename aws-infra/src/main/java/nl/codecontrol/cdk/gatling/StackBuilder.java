package nl.codecontrol.cdk.gatling;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;

public abstract class StackBuilder<T> {
    private Construct scope;
    private String id;
    private StackProps stackProps;

    public Construct getScope() {
        return scope;
    }

    public String getId() {
        return id;
    }

    public StackProps getStackProps() {
        return stackProps;
    }

    protected T scope(Construct scope) {
        this.scope = scope;
        return self();
    }

    protected T id(String id) {
        this.id = id;
        return self();
    }

    protected T stackProps(StackProps stackProps) {
        this.stackProps = stackProps;
        return self();
    }

    protected abstract Stack build();

    protected abstract T self();
}
