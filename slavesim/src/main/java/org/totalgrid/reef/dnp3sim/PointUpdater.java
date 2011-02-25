/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  Green Energy Corp licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.totalgrid.reef.dnp3sim;

import org.totalgrid.reef.protocol.dnp3.Analog;
import org.totalgrid.reef.protocol.dnp3.Binary;
import org.totalgrid.reef.protocol.dnp3.Counter;
import org.totalgrid.reef.protocol.dnp3.IDataObserver;

import java.util.*;


public class PointUpdater {
    private final int interval = 100;
    private final double pointRate;  // update / sec

    private Vector<ObserverUpdater> updaters;
    private final int pointCount;

    private Random random = new Random(System.currentTimeMillis());

    private Map<Integer, Boolean> lastStatuses = new HashMap<Integer, Boolean>();

    private Timer timer = new Timer();

    public PointUpdater(Collection<Stack> stacks, double pointRate) {
        this.pointRate = pointRate;

        int count = 0;
        updaters = new Vector<ObserverUpdater>(stacks.size());
        for (Stack stack : stacks) {
            ObserverUpdater updater = new ObserverUpdater(stack.getObserver(), stack.getDimension(), random);
            updaters.add(updater);
            count += updater.getPointCount();
        }
        pointCount = count;
    }

    public int getPointCount() {
        return pointCount;
    }

    public void start() {
        loadInitial();
        timer.scheduleAtFixedRate(new RandomUpdateTask(), interval, interval);
    }

    public void stop() {
        timer.cancel();
    }

    protected int randPos(int mod) {
        return Math.abs(random.nextInt()) % mod;
    }

    protected void loadInitial() {
        for (ObserverUpdater up : updaters) {
            up.loadAll();
        }
    }

    class RandomUpdateTask extends TimerTask {
        @Override
        public void run() {
            int updatedCount = 0;
            double updatesToDo = pointCount * ((double)interval/(double)1000) * pointRate;
            double updatesPerDevice = updatesToDo / (double)updaters.size();

            if (updatesPerDevice > 1) {
                for (ObserverUpdater up : updaters) {
                    up.update((int)updatesPerDevice);
                    updatedCount += (int)updatesPerDevice;
                }
            } else {
                int devicesToUpdate = (int)Math.ceil(updatesPerDevice * (double)updaters.size());
                Set<Integer> selected = new HashSet<Integer>();
                for (int i = 0; i < devicesToUpdate; i++) {
                    int index = SetSelect.selectIndex(randPos(updaters.size()), selected, updaters.size());
                    updaters.get(index).update(1);
                    updatedCount += 1;
                    selected.add(index);
                }
            }
        }
    }
}
