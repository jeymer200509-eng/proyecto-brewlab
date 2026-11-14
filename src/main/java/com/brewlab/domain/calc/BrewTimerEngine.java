package com.brewlab.domain.calc;

import com.brewlab.domain.model.BrewEvent;
import com.brewlab.domain.model.BrewSession;
import com.brewlab.domain.model.RecipeStep;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Motor determinista de temporización de {@link BrewSession}.
 * <p>
 * Reconstruye el tiempo transcurrido a partir de marcas temporales reales
 * ({@code startedAt}, {@code pausedAt}, {@code accumulatedPauseMs} y {@code completedAt}),
 * nunca de un contador incremental que pierde tiempo en segundo plano o al recargar la página.
 */
public final class BrewTimerEngine {

    private BrewTimerEngine() {
    }

    /**
     * Reconstruye los milisegundos netos transcurridos de la sesión en el instante {@code now}:
     * {@code elapsed = referenceEnd - startedAt - accumulatedPauseMs}.
     * Mientras la sesión está en pausa, el tiempo permanece fijo en {@code pausedAt}.
     */
    public static long computeElapsedMs(BrewSession session, Instant now) {
        if (session == null || session.getStartedAt() == null) {
            return 0L;
        }
        String status = session.getStatus();
        if (BrewSession.STATUS_PREPARED.equals(status)) {
            return 0L;
        }
        Instant effectiveEnd;
        if (BrewSession.STATUS_PAUSED.equals(status) && session.getPausedAt() != null) {
            effectiveEnd = session.getPausedAt();
        } else if ((BrewSession.STATUS_COMPLETED.equals(status) || BrewSession.STATUS_CANCELLED.equals(status))
                && session.getCompletedAt() != null) {
            effectiveEnd = session.getCompletedAt();
        } else {
            effectiveEnd = now;
        }
        long raw = effectiveEnd.toEpochMilli() - session.getStartedAt().toEpochMilli() - session.getAccumulatedPauseMs();
        return Math.max(0L, raw);
    }

    public static int computeElapsedSeconds(BrewSession session, Instant now) {
        return (int) Math.round(computeElapsedMs(session, now) / 1000.0);
    }

    /**
     * Calcula la nueva duración acumulada de pausas al reanudar una sesión pausada en {@code pausedAt}.
     */
    public static long computeAccumulatedPauseAfterResume(long previousAccumulatedPauseMs,
                                                          Instant pausedAt,
                                                          Instant resumedAt) {
        if (pausedAt == null || resumedAt == null) {
            return previousAccumulatedPauseMs;
        }
        long pauseDelta = Math.max(0L, resumedAt.toEpochMilli() - pausedAt.toEpochMilli());
        return previousAccumulatedPauseMs + pauseDelta;
    }

    /** Devuelve el primer paso de la instantánea que aún no ha sido completado ni omitido. */
    public static RecipeStep findCurrentStep(BrewSession session) {
        Set<Integer> handledOrders = handledStepOrders(session.getEvents());
        for (RecipeStep step : session.getSnapshotSteps()) {
            if (!handledOrders.contains(Integer.valueOf(step.getOrder()))) {
                return step;
            }
        }
        return null;
    }

    /** Devuelve el siguiente paso pendiente después del paso actual, o {@code null}. */
    public static RecipeStep findNextStep(BrewSession session) {
        Set<Integer> handledOrders = handledStepOrders(session.getEvents());
        boolean skippedFirstPending = false;
        for (RecipeStep step : session.getSnapshotSteps()) {
            if (!handledOrders.contains(Integer.valueOf(step.getOrder()))) {
                if (!skippedFirstPending) {
                    skippedFirstPending = true;
                } else {
                    return step;
                }
            }
        }
        return null;
    }

    public static Set<Integer> handledStepOrders(List<BrewEvent> events) {
        Set<Integer> done = new HashSet<>();
        if (events == null) {
            return done;
        }
        for (BrewEvent ev : events) {
            if ((BrewEvent.TYPE_STEP_COMPLETED.equals(ev.getEventType())
                    || BrewEvent.TYPE_STEP_SKIPPED.equals(ev.getEventType()))
                    && ev.getStepOrder() != null) {
                done.add(ev.getStepOrder());
            }
        }
        return done;
    }
}
