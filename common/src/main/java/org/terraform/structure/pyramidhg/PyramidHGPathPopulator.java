package org.terraform.structure.pyramidhg;

import org.terraform.structure.room.PathPopulatorAbstract;
import org.terraform.structure.room.PathPopulatorData;

import java.util.Random;

public class PyramidHGPathPopulator extends PathPopulatorAbstract {
    @SuppressWarnings("unused")
    private final Random rand;

    public PyramidHGPathPopulator(Random rand) {
        this.rand = rand;
    }

    @Override
    public void populate(PathPopulatorData ppd) {
        // PyramidHG paths intentionally contain no loot or traps.
    }

    @Override
    public int getPathWidth() {
        return 1;
    }

    @Override
    public int getPathHeight() {
        return 3;
    }
}
