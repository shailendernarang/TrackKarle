package com.example.wealthtracker.analytics

import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PerformanceManager @Inject constructor() {
    
    private val firebasePerformance: FirebasePerformance by lazy {
        FirebasePerformance.getInstance()
    }

    // Custom traces for critical operations
    fun startTrace(traceName: String): Trace {
        return firebasePerformance.newTrace(traceName).apply {
            start()
        }
    }

    fun stopTrace(trace: Trace) {
        trace.stop()
    }

    // Convenience methods for common operations
    fun tracePortfolioCalculation(block: () -> Unit) {
        val trace = startTrace("portfolio_calculation")
        try {
            block()
        } finally {
            stopTrace(trace)
        }
    }

    fun traceDataLoad(dataType: String, block: () -> Unit) {
        val trace = startTrace("data_load_$dataType")
        try {
            block()
        } finally {
            stopTrace(trace)
        }
    }

    fun traceChartGeneration(chartType: String, block: () -> Unit) {
        val trace = startTrace("chart_generation_$chartType")
        try {
            block()
        } finally {
            stopTrace(trace)
        }
    }

    fun traceDatabaseOperation(operation: String, block: () -> Unit) {
        val trace = startTrace("db_operation_$operation")
        try {
            block()
        } finally {
            stopTrace(trace)
        }
    }

    fun traceNetworkCall(endpoint: String, block: () -> Unit) {
        val trace = startTrace("network_call_${endpoint.replace("/", "_")}")
        try {
            block()
        } finally {
            stopTrace(trace)
        }
    }

    // Add custom attributes to traces
    fun addTraceAttribute(trace: Trace, key: String, value: String) {
        trace.putAttribute(key, value)
    }

    fun addTraceMetric(trace: Trace, metricName: String, value: Long) {
        trace.putMetric(metricName, value)
    }

    // Increment counters
    fun incrementTraceMetric(trace: Trace, metricName: String) {
        trace.incrementMetric(metricName, 1)
    }
}
