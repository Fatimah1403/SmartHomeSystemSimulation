package com.fatty.smarthome.concurrent.automation;

import com.fatty.smarthome.core.FacadeSmartHome;
import com.fatty.smarthome.devices.SmartDevice;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * AutomationEngine runs automation rules concurrently.
 * Each rule can be executed in its own thread for parallel processing.
 */
public class AutomationEngine {
    private final FacadeSmartHome facade;
    private final ScheduledExecutorService scheduler;
    private final ExecutorService ruleExecutor;
    private final List<ConcurrentRule> rules = new CopyOnWriteArrayList<>();
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
    private volatile boolean running = false;


    public interface ConcurrentRule {
        String getName();
        boolean shouldExecute();
        void execute(List<SmartDevice> devices);
    }

    public AutomationEngine(FacadeSmartHome facade) {
        this.facade = facade;
        this.scheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r);
            t.setName("AutomationScheduler-" + t.getId());
            t.setDaemon(true);
            return t;
        });
        this.ruleExecutor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r);
            t.setName("RuleExecutor-" + t.getId());
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Add a rule to the automation engine
     */
    public void addRule(ConcurrentRule rule) {
        rules.add(rule);
        System.out.println("➕ Added automation rule: " + rule.getName());
    }

    /**
     * Start the automation engine with periodic rule checking
     */
    public void start() {
        // Schedule rule evaluation every 5 seconds
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(
                this::evaluateRules, 0, 5, TimeUnit.SECONDS);

        scheduledTasks.put("rule-evaluation", future);
        System.out.println("🤖 Automation engine started");
    }

    /**
     * Evaluate all rules concurrently
     */
    private void evaluateRules() {
        List<SmartDevice> devices = facade.getDevices();

        // Submit each rule for concurrent execution
        List<CompletableFuture<Void>> futures = rules.stream()
                .filter(ConcurrentRule::shouldExecute)
                .map(rule -> CompletableFuture.runAsync(() -> {
                    try {
                        rule.execute(devices);
                    } catch (Exception e) {
                        System.err.println("❌ Error executing rule " +
                                rule.getName() + ": " + e.getMessage());
                    }
                }, ruleExecutor))
                .toList();

        // Wait for all rules to complete (with timeout)
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(2, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            System.err.println("⏱️  Some automation rules took too long to execute");
        } catch (Exception e) {
            System.err.println("❌ Error in rule execution: " + e.getMessage());
        }
    }
    /**
     * Stop the automation engine
     */
    /**
     * Stop the automation engine
     */
    public void stop() {
        // Cancel all scheduled tasks
        scheduledTasks.values().forEach(future -> future.cancel(false));
        scheduledTasks.clear();

        // Shutdown executors
        scheduler.shutdown();
        ruleExecutor.shutdown();

        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
            if (!ruleExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                ruleExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            ruleExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        System.out.println("🛑 Automation engine stopped");
    }

    /**
     * Get status of the automation engine
     */
    public String getStatus() {
        return String.format(
                "Automation Engine Status:\n" +
                        "  Active rules: %d\n" +
                        "  Scheduled tasks: %d\n" +
                        "  Scheduler active: %s\n" +
                        "  Rule executor active: %s",
                rules.size(),
                scheduledTasks.size(),
                !scheduler.isShutdown(),
                !ruleExecutor.isShutdown()
        );
    }


}
