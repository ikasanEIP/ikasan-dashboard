# Ikasan SolrCloud Configuration Template
# This is a unified template for all nodes in the SolrCloud cluster
# The actual solr.in.sh will be sourced by Solr and gets config from ikasan-env.sh

# ===========================================
# Environment Configuration
# ===========================================

# Source environment configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
if [ -f "$SCRIPT_DIR/../config/ikasan-env.sh" ]; then
    source "$SCRIPT_DIR/../config/ikasan-env.sh"
else
    echo "WARNING: config/ikasan-env.sh not found, using defaults"
fi

# ===========================================
# Solr Mode
# ===========================================

# Enable SolrCloud mode
SOLR_MODE="solrcloud"

# ===========================================
# Network Configuration
# ===========================================

# Hostname for this Solr node
# Sourced from ikasan-env.sh, falls back to localhost
SOLR_HOST="${SOLR_HOST}"
[ -z "$SOLR_HOST" ] && SOLR_HOST="localhost"

# Solr port for this node
# IMPORTANT: Set SOLR_PORT in ikasan-env.sh
SOLR_PORT="${SOLR_PORT}"
[ -z "$SOLR_PORT" ] && SOLR_PORT="8983"

# ===========================================
# ZooKeeper Configuration
# ===========================================

# ZooKeeper connection string (sourced from ikasan-env.sh)
# Format: host1:port1,host2:port2,host3:port3
ZK_HOST="${ZK_HOSTS}"
[ -z "$ZK_HOST" ] && ZK_HOST="localhost:2181,localhost:2182,localhost:2183"

# ZooKeeper client timeout
#ZK_CLIENT_TIMEOUT="30000"

# ===========================================
# Directory Configuration
# ===========================================

# Solr home directory (contains collections and configuration)
# Use absolute path from ikasan-env.sh
SOLR_HOME="${SOLR_INSTALL_DIR}/server/solr"

# Data directory (where indexes are stored)
SOLR_DATA_DIR="${SOLR_DATA_DIR}"

# Log directory
SOLR_LOGS_DIR="${SOLR_LOG_DIR}"

# PID directory
SOLR_PID_DIR="${SOLR_PID_DIR}"

# ===========================================
# Java Memory Configuration
# ===========================================

# Java heap size (min and max)
SOLR_HEAP="${SOLR_HEAP}"
[ -z "$SOLR_HEAP" ] && SOLR_HEAP="2g"

# Java min heap size (if different from max)
#SOLR_JAVA_MEM="-Xms2g -Xmx2g"

# ===========================================
# Garbage Collection Configuration
# ===========================================

# GC Tuning
GC_TUNE="${GC_TUNE}"
[ -z "$GC_TUNE" ] && GC_TUNE="-XX:+UseG1GC -XX:+PerfDisableSharedMem -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=250 -XX:+AlwaysPreTouch"

# GC Logging
#GC_LOG_OPTS="-Xlog:gc*:file=${SOLR_LOGS_DIR}/solr_gc.log:time,uptime:filecount=9,filesize=20M"

# ===========================================
# Advanced Java Options
# ===========================================

# Additional Java options
SOLR_OPTS="${SOLR_OPTS}"
[ -z "$SOLR_OPTS" ] && SOLR_OPTS="-Dsolr.autoSoftCommit.maxTime=3000"

# Java system properties
#SOLR_OPTS="$SOLR_OPTS -Dsolr.autoSoftCommit.maxTime=3000"
#SOLR_OPTS="$SOLR_OPTS -Dsolr.autoCommit.maxTime=60000"

# ===========================================
# Solr Security
# ===========================================

# Enable SSL
#SOLR_SSL_ENABLED=true
#SOLR_SSL_KEY_STORE=/path/to/keystore.p12
#SOLR_SSL_KEY_STORE_PASSWORD=secret
#SOLR_SSL_KEY_STORE_TYPE=PKCS12
#SOLR_SSL_TRUST_STORE=/path/to/truststore.p12
#SOLR_SSL_TRUST_STORE_PASSWORD=secret
#SOLR_SSL_TRUST_STORE_TYPE=PKCS12
#SOLR_SSL_NEED_CLIENT_AUTH=false
#SOLR_SSL_WANT_CLIENT_AUTH=false

# Authentication
#SOLR_AUTH_TYPE=basic
#SOLR_AUTHENTICATION_OPTS="-Dbasicauth=user:password"

# ===========================================
# Jetty Configuration
# ===========================================

# Jetty thread pool
#SOLR_JETTY_THREADS_MIN="${SOLR_JETTY_THREADS_MIN:-10}"
#SOLR_JETTY_THREADS_MAX="${SOLR_JETTY_THREADS_MAX:-10000}"
#SOLR_JETTY_THREADS_IDLE_TIMEOUT="${SOLR_JETTY_THREADS_IDLE_TIMEOUT:-5000}"
#SOLR_JETTY_THREADS_STOP_TIMEOUT="${SOLR_JETTY_THREADS_STOP_TIMEOUT:-60000}"

# Jetty accept queue size
#SOLR_JETTY_ACCEPT_QUEUE_SIZE="${SOLR_JETTY_ACCEPT_QUEUE_SIZE:-128}"

# Jetty request header size
#SOLR_JETTY_REQUEST_HEADER_SIZE="${SOLR_JETTY_REQUEST_HEADER_SIZE:-65536}"

# Jetty response header size
#SOLR_JETTY_RESPONSE_HEADER_SIZE="${SOLR_JETTY_RESPONSE_HEADER_SIZE:-65536}"

# ===========================================
# Performance Tuning
# ===========================================

# Enable remote JMX monitoring
#ENABLE_REMOTE_JMX_OPTS="${ENABLE_REMOTE_JMX_OPTS:-false}"
#RMI_PORT="${RMI_PORT:-18983}"

# Wait for Solr to be seen by ZooKeeper
#SOLR_WAIT_FOR_ZK="${SOLR_WAIT_FOR_ZK:-30}"

# Timeout for stop operation
#SOLR_STOP_WAIT="${SOLR_STOP_WAIT:-180}"

# ===========================================
# Logging Configuration
# ===========================================

# Log level (ALL, TRACE, DEBUG, INFO, WARN, ERROR, FATAL, OFF)
#SOLR_LOG_LEVEL="${SOLR_LOG_LEVEL:-INFO}"

# Log4j configuration file
#LOG4J_PROPS="${LOG4J_PROPS:-${SOLR_HOME}/log4j2.xml}"

# Disable log watcher (set to false to enable)
#SOLR_LOG_PRESTART_ROTATION="${SOLR_LOG_PRESTART_ROTATION:-true}"

# ===========================================
# Module Configuration
# ===========================================

# Solr modules to enable (comma-separated)
# Available: extraction, clustering, analysis-extras, sql, scripting
#SOLR_MODULES="${SOLR_MODULES:-}"

# ===========================================
# Time Zone
# ===========================================

# Set timezone (e.g., UTC, America/New_York, Europe/London)
#SOLR_TIMEZONE="${SOLR_TIMEZONE:-UTC}"

# ===========================================
# Advanced Configuration
# ===========================================

# Solr install directory override
#SOLR_INSTALL_DIR="${SOLR_INSTALL_DIR}"

# Solr temporary directory
#SOLR_TMP_DIR="${SOLR_TMP_DIR:-${SOLR_HOME}/tmp}"

# Enable/disable collection auto-creation
#SOLR_AUTO_CREATE_COLLECTIONS="${SOLR_AUTO_CREATE_COLLECTIONS:-true}"

# ZooKeeper ensemble health check timeout
#ZK_ENSEMBLE_TIMEOUT="${ZK_ENSEMBLE_TIMEOUT:-30000}"

# Maximum number of update retries
#SOLR_UPDATE_MAX_RETRIES="${SOLR_UPDATE_MAX_RETRIES:-3}"

# ===========================================
# Commit Configuration
# ===========================================

# Auto soft commit max time (milliseconds)
#SOLR_AUTO_SOFT_COMMIT_MAX_TIME="${SOLR_AUTO_SOFT_COMMIT_MAX_TIME:-3000}"

# Auto hard commit max time (milliseconds)
#SOLR_AUTO_COMMIT_MAX_TIME="${SOLR_AUTO_COMMIT_MAX_TIME:-60000}"

# ===========================================
# Cache Configuration
# ===========================================

# Document cache size
#SOLR_DOCUMENT_CACHE_SIZE="${SOLR_DOCUMENT_CACHE_SIZE:-512}"

# Query result cache size
#SOLR_QUERY_RESULT_CACHE_SIZE="${SOLR_QUERY_RESULT_CACHE_SIZE:-512}"

# Filter cache size
#SOLR_FILTER_CACHE_SIZE="${SOLR_FILTER_CACHE_SIZE:-512}"
