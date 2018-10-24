/* ===========================================================================
 * Copyright 2015 EUROPEAN UNION
 *
 * Licensed under the EUPL, Version 1.1 or subsequent versions of the
 * EUPL (the "License"); You may not use this work except in compliance
 * with the License. You may obtain a copy of the License at
 * http://ec.europa.eu/idabc/eupl
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Date: 02/04/2015
 * Authors:
 * - Michel Gerboles, michel.gerboles@jrc.ec.europa.eu, 
 *   Laurent Spinelle, laurent.spinelle@jrc.ec.europa.eu and 
 *   Alexander Kotsev, alexander.kotsev@jrc.ec.europa.eu:
 *			European Commission - Joint Research Centre, 
 * - Marco Signorini, marco.signorini@liberaintentio.com
 *
 * ===========================================================================
 */

package airsenseur.dev.helpers;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Implements a NTP-drift aware periodic scheduler
 * @author marco
 */
public abstract class TaskScheduler implements Runnable {
    
    private static class TaskSchedulerThreadFactory implements ThreadFactory {
        
        private final String taskName;
        
        private TaskSchedulerThreadFactory() {
            this.taskName = "AirSensEUR-Thread";
        }
        
        public TaskSchedulerThreadFactory(String taskName) {
            this.taskName = taskName;
        }
        
        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, taskName);
        }
    }
    
    private ScheduledExecutorService worker;
    private ScheduledFuture scheduled;
    private ThreadFactory threadFactory;
    
    public abstract void taskMain();
    public abstract String getTaskName();
    
    public void startPeriodic(long milliSeconds) {
        
        if (scheduled != null) {
            scheduled.cancel(true);
        }
        
        threadFactory = new TaskSchedulerThreadFactory(getTaskName());
        worker = Executors.newScheduledThreadPool(1, threadFactory);
        scheduled = worker.scheduleAtFixedRate(this, 0, milliSeconds, TimeUnit.MILLISECONDS);
    }
    
    public void startNow() {
        if (scheduled != null) {
            scheduled.cancel(true);
        }
        
        threadFactory = new TaskSchedulerThreadFactory(getTaskName());
        worker = Executors.newScheduledThreadPool(1, threadFactory);
        scheduled = worker.schedule(this, 0, TimeUnit.MILLISECONDS);
    }
    
    public boolean waitForTermination(long milliSeconds) throws InterruptedException {
        if (worker != null) {
            return worker.awaitTermination(milliSeconds, TimeUnit.MILLISECONDS);
        }
        
        return true;
    }
    
    public boolean isShutdown() {
        return (worker != null) && worker.isShutdown();
    }
    
    public void stop() {
        if (scheduled != null) {
            scheduled.cancel(true);
        }
        
        if (worker != null) {
            worker.shutdown();
        }
    }

    @Override
    public void run() {
        taskMain();
    }
    
}
