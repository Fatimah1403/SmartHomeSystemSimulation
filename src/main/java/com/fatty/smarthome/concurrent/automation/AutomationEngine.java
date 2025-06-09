package com.fatty.smarthome.concurrent.automation;

import com.fatty.smarthome.core.ConcurrentRule;
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
    public void addRule(com.fatty.smarthome.concurrent.automation.ConcurrentRule rule) {
        rules.add((ConcurrentRule) rule);
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



}
