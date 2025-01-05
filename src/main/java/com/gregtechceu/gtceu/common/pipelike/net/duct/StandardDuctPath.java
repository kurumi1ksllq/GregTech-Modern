package com.gregtechceu.gtceu.common.pipelike.net.duct;

import com.gregtechceu.gtceu.api.data.medicalcondition.MedicalCondition;
import com.gregtechceu.gtceu.api.graphnet.logic.NetLogicData;
import com.gregtechceu.gtceu.api.graphnet.logic.WeightFactorLogic;
import com.gregtechceu.gtceu.api.graphnet.net.NetEdge;
import com.gregtechceu.gtceu.api.graphnet.net.NetNode;
import com.gregtechceu.gtceu.api.graphnet.path.PathBuilder;
import com.gregtechceu.gtceu.api.graphnet.path.SingletonNetPath;
import com.gregtechceu.gtceu.api.graphnet.path.StandardNetPath;
import com.gregtechceu.gtceu.api.graphnet.predicate.test.IPredicateTestObject;
import com.gregtechceu.gtceu.common.pipelike.net.energy.*;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class StandardDuctPath extends StandardNetPath implements DuctPath {

    public StandardDuctPath(@NotNull ImmutableCollection<NetNode> nodes, @NotNull ImmutableCollection<NetEdge> edges,
                            double weight) {
        super(nodes, edges, weight);
    }

    public StandardDuctPath(@NotNull StandardDuctPath reverse) {
        super(reverse);
    }

    @NotNull
    @Override
    public DuctPath.PathFlowReport traverse(final MedicalCondition condition, float differenceAmount) {
        if (differenceAmount <= 0) return EMPTY;
        for (NetEdge edge : getOrderedEdges()) {
            if (!edge.test(IPredicateTestObject.INSTANCE)) return EMPTY;
        }
        return new StandardReport(condition, differenceAmount);
    }

    @Override
    public @NotNull StandardDuctPath reversed() {
        if (reversed == null) {
            reversed = new StandardDuctPath(this);
        }
        return (StandardDuctPath) reversed;
    }

    public static final class Builder implements PathBuilder {

        public final List<NetNode> nodes = new ObjectArrayList<>();
        public final List<NetEdge> edges = new ObjectArrayList<>();

        public Builder(@NotNull NetNode startingNode) {
            nodes.add(startingNode);
            handleAdditionalInfo(startingNode);
        }

        private void handleAdditionalInfo(@NotNull NetNode node) {}

        @Override
        @Contract("_, _ -> this")
        public Builder addToEnd(@NotNull NetNode node, @NotNull NetEdge edge) {
            NetNode end = nodes.get(nodes.size() - 1);
            if (edge.getOppositeNode(node) != end)
                throw new IllegalArgumentException("Edge does not link last node and new node!");
            nodes.add(node);
            handleAdditionalInfo(node);
            edges.add(edge);
            return this;
        }

        @Override
        @Contract("_, _ -> this")
        public Builder addToStart(@NotNull NetNode node, @NotNull NetEdge edge) {
            NetNode end = nodes.get(0);
            if (edge.getOppositeNode(node) != end)
                throw new IllegalArgumentException("Edge does not link last node and new node!");
            nodes.add(0, node);
            handleAdditionalInfo(node);
            edges.add(0, edge);
            return this;
        }

        @Override
        @Contract("-> this")
        public Builder reverse() {
            Collections.reverse(nodes);
            Collections.reverse(edges);
            return this;
        }

        @Override
        public StandardDuctPath build() {
            double sum = 0.0;
            for (NetEdge edge : edges) {
                double edgeWeight = edge.getWeight();
                sum += edgeWeight;
            }
            return new StandardDuctPath(ImmutableSet.copyOf(nodes), ImmutableSet.copyOf(edges), sum);
        }
    }

    public static class SingletonDuctPath extends SingletonNetPath implements DuctPath {

        protected final long voltageLimit;

        protected final long loss;

        protected final long amperageLimit;

        public SingletonDuctPath(NetNode node) {
            this(node, node.getData().getLogicEntryDefaultable(WeightFactorLogic.TYPE).getValue());
        }

        public SingletonDuctPath(NetNode node, double weight) {
            super(node, weight);
            NetLogicData data = node.getData();
            this.voltageLimit = data.getLogicEntryDefaultable(VoltageLimitLogic.TYPE).getValue();
            this.loss = (long) Math.ceil(data.getLogicEntryDefaultable(VoltageLossLogic.TYPE).getValue());
            this.amperageLimit = data.getLogicEntryDefaultable(AmperageLimitLogic.TYPE).getValue();
        }

        @Override
        public @NotNull PathFlowReport traverse(final MedicalCondition condition, float amount) {
            return new StandardReport(condition, amount);
        }
    }

    protected static final DuctPath.PathFlowReport EMPTY = new DuctPath.PathFlowReport() {

        @Override
        public MedicalCondition conditionOut() {
            return null;
        }

        @Override
        public float amountOut() {
            return 0;
        }

        @Override
        public void report() {}
    };

    public static final class StandardReport implements PathFlowReport {

        private final MedicalCondition condition;
        private final float amount;
        private final Runnable[] report;

        public StandardReport(MedicalCondition condition, float amount, @NotNull Runnable @NotNull... report) {
            this.condition = condition;
            this.amount = amount;
            this.report = report;
        }

        public StandardReport(MedicalCondition condition, float amount, @NotNull List<@NotNull Runnable> report) {
            this.condition = condition;
            this.amount = amount;
            this.report = report.toArray(Runnable[]::new);
        }

        @Override
        public MedicalCondition conditionOut() {
            return condition;
        }

        @Override
        public float amountOut() {
            return amount;
        }

        @Override
        public void report() {
            for (Runnable runnable : report) {
                runnable.run();
            }
        }
    }
}
