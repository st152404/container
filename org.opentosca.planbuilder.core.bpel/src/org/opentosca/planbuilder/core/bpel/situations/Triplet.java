package org.opentosca.planbuilder.core.bpel.situations;

public class Triplet<A, B, C> {
    final A first;
    final B second;
    final C third;

    public Triplet(final A first, final B second, final C third) {
        this.first = first;
        this.second = second;
        this.third = third;
    }

    public A getFirst() {
        return this.first;
    }

    public B getSecond() {
        return this.second;
    }

    public C getThird() {
        return this.third;
    }
}