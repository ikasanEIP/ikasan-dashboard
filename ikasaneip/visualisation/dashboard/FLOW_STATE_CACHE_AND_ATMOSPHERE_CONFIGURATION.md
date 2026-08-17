# Ikasan Dashboard Configuration Guide

This document describes the externalized configuration options available for the Ikasan Dashboard, including FlowStateCache throttling and Atmosphere framework settings.

## Table of Contents
- [FlowStateCache Configuration](#flowstatecache-configuration)
- [Atmosphere Framework Configuration](#atmosphere-framework-configuration)

---

## FlowStateCache Configuration

The FlowStateCache is responsible for managing flow state transitions and broadcasting state changes to the UI. To prevent memory issues caused by rapid state oscillations (particularly RECOVERING ↔ STOPPED transitions), the cache implements intelligent throttling.

### Purpose

**Problem**: When flows repeatedly oscillate between RECOVERING and STOPPED states (e.g., during connection issues), each state change triggers a broadcast to all connected clients. Rapid oscillations can cause:
- Excessive memory consumption
- UI performance degradation
- Network bandwidth saturation
- Database overload

**Solution**: The throttling mechanism detects oscillations and limits broadcast frequency while ensuring:
- Non-oscillating states are broadcast immediately
- First occurrence of an oscillation is broadcast immediately
- Subsequent oscillations are throttled to a configurable interval
- The latest state is always broadcast eventually

### Configuration Properties

#### `flow.state.cache.throttle.interval.millis`

**Purpose**: Sets the minimum time interval (in milliseconds) between broadcasts for flows experiencing RECOVERING ↔ STOPPED oscillations.

**Default**: `60000` (1 minute)

**Example**:
```properties
# application.properties
flow.state.cache.throttle.interval.millis=30000
```

**Recommended Values**:
- **Production**: `60000` (1 minute) - Balances responsiveness with stability
- **High-frequency environments**: `120000` (2 minutes) - Reduces broadcast load further
- **Development/Testing**: `5000` (5 seconds) - Faster feedback for debugging

**Impact**:
- **Lower values**: More frequent broadcasts, higher resource usage, more responsive UI
- **Higher values**: Fewer broadcasts, lower resource usage, less responsive UI during oscillations

---

#### `flow.state.cache.oscillation.window.millis`

**Purpose**: Defines the time window (in milliseconds) within which state changes must occur to be considered part of an oscillation. If state transitions occur with gaps longer than this window, they are NOT considered oscillating and are broadcast immediately.

**Default**: `5000` (5 seconds)

**Example**:
```properties
# application.properties
flow.state.cache.oscillation.window.millis=10000
```

**Behavior**:
- State changes within the window → Treated as oscillation, throttling applies
- State changes outside the window → Treated as independent events, broadcast immediately

**Recommended Values**:
- **Fast-failing systems**: `3000` (3 seconds) - Tight oscillation detection
- **Standard**: `5000` (5 seconds) - Default, works for most scenarios
- **Slow recovery systems**: `10000` (10 seconds) - Allows longer gaps between oscillating states

**Impact**:
- **Lower values**: Stricter oscillation detection, fewer states throttled
- **Higher values**: More lenient oscillation detection, more states throttled

---

### Complete Example Configuration

```properties
# FlowStateCache Throttling Configuration
# =======================================

# Throttle interval: Wait at least 45 seconds between broadcasts for oscillating flows
flow.state.cache.throttle.interval.millis=45000

# Oscillation window: Consider states within 8 seconds as part of an oscillation
flow.state.cache.oscillation.window.millis=8000
```

### How Throttling Works

1. **First RECOVERING state** → Broadcast immediately
2. **First STOPPED state** (within oscillation window) → Broadcast immediately, mark as "in oscillation"
3. **Subsequent oscillations** (within oscillation window) → Throttled
   - Cancel any pending broadcast
   - Schedule new broadcast after throttle interval
   - Latest state always broadcast eventually
4. **Non-oscillating states** (RUNNING, PAUSED, etc.) → Always broadcast immediately
5. **States outside oscillation window** → Reset oscillation detection, broadcast immediately

---

## Atmosphere Framework Configuration

The Atmosphere framework handles real-time push notifications from server to clients (WebSocket/Server-Sent Events). These configurations allow fine-tuning of the broadcast behavior.

### Purpose

Atmosphere is the underlying technology for Vaadin Push, enabling real-time updates to the UI without polling. Configuration allows:
- Performance tuning for different deployment scenarios
- Adjusting timeouts for slow networks
- Controlling connection pooling
- Managing broadcast message queuing

### Configuration Properties

#### `org.atmosphere.cpr.broadcaster.use.externalised.configurations`

**Purpose**: Enables or disables the use of externalized Atmosphere configurations. When `true`, the dashboard will read additional Atmosphere settings from the configuration map.

**Default**: `false`

**Example**:
```properties
# application.properties
org.atmosphere.cpr.broadcaster.use.externalised.configurations=true
```

---

#### `org.atmosphere.cpr.configurations`

**Purpose**: A map of Atmosphere framework configuration key-value pairs. Only used when `use.externalised.configurations=true`.

**Format**: Spring Expression Language (SpEL) map syntax

**Example**:
```properties
# Enable externalized configurations
org.atmosphere.cpr.broadcaster.use.externalised.configurations=true

# Atmosphere configuration map
org.atmosphere.cpr.configurations={'org.atmosphere.cpr.broadcaster.maxProcessingThreads': '10', \
                                   'org.atmosphere.cpr.broadcaster.writeTimeout': '60000', \
                                   'org.atmosphere.cpr.broadcaster.maxAsyncWriteTimeout': '60000', \
                                   'org.atmosphere.cpr.CometSupport.maxInactiveActivity': '300000', \
                                   'org.atmosphere.cpr.broadcaster.shareableThreadPool': 'true', \
                                   'org.atmosphere.websocket.maxTextMessageSize': '1048576', \
                                   'org.atmosphere.websocket.maxBinaryMessageSize': '1048576'}
```

### Common Atmosphere Parameters

| Parameter | Purpose | Default | Recommended |
|-----------|---------|---------|-------------|
| `org.atmosphere.cpr.broadcaster.maxProcessingThreads` | Max threads for broadcast processing | 20 | 10-50 based on load |
| `org.atmosphere.cpr.broadcaster.writeTimeout` | Timeout for write operations (ms) | 30000 | 60000-120000 |
| `org.atmosphere.cpr.broadcaster.maxAsyncWriteTimeout` | Max timeout for async writes (ms) | 30000 | 60000-120000 |
| `org.atmosphere.cpr.CometSupport.maxInactiveActivity` | Max inactive time before disconnect (ms) | 300000 | 300000-600000 |
| `org.atmosphere.cpr.broadcaster.shareableThreadPool` | Share thread pool across broadcasters | false | true (for efficiency) |
| `org.atmosphere.websocket.maxTextMessageSize` | Max WebSocket text message size (bytes) | 8192 | 1048576 (1MB) |
| `org.atmosphere.websocket.maxBinaryMessageSize` | Max WebSocket binary message size (bytes) | 8192 | 1048576 (1MB) |

### Complete Example Configuration

```properties
# Atmosphere Framework Configuration
# ===================================

# Enable externalized Atmosphere configurations
org.atmosphere.cpr.broadcaster.use.externalised.configurations=true

# Atmosphere configuration map for production environment
org.atmosphere.cpr.configurations={'org.atmosphere.cpr.broadcaster.maxProcessingThreads': '20', \
                                   'org.atmosphere.cpr.broadcaster.writeTimeout': '120000', \
                                   'org.atmosphere.cpr.broadcaster.maxAsyncWriteTimeout': '120000', \
                                   'org.atmosphere.cpr.CometSupport.maxInactiveActivity': '600000', \
                                   'org.atmosphere.cpr.broadcaster.shareableThreadPool': 'true', \
                                   'org.atmosphere.websocket.maxTextMessageSize': '2097152', \
                                   'org.atmosphere.websocket.maxBinaryMessageSize': '2097152', \
                                   'org.atmosphere.cpr.broadcaster.cache.strategy': 'org.atmosphere.cache.UUIDBroadcasterCache'}
```
## See Also

- [FlowStateCache Source](src/main/java/org/ikasan/dashboard/cache/FlowStateCache.java)
- [FlowStateCacheTest](src/test/java/org/ikasan/dashboard/cache/FlowStateCacheTest.java)
- [DashboardComponentFactory Source](src/main/java/org/ikasan/dashboard/beans/DashboardComponentFactory.java)
- [Atmosphere Framework Documentation](https://github.com/Atmosphere/atmosphere)
- [Vaadin Push Documentation](https://vaadin.com/docs/latest/advanced/server-push)
