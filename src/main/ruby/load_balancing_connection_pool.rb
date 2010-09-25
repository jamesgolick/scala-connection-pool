require "java"

class LoadBalancingConnectionPool
  @@converter = java_import "scala.collection.JavaConversions"
  java_import "connectionpool.LoadBalancedConnectionPool"
  java_import "connectionpool.SimpleConnectionPool"

  def initialize(factories, options = {})
    options[:recoverable_errors] ||= []
    canRecover = Proc.new { |error| options[:recoverable_errors].any? { |e| puts(error.inspect); error.getException.is_a?(e) } }
    pools = factories.map { |f| SimpleConnectionPool.new(f, options[:min_idle] || 5, options[:max_active] || 20, options[:max_idle] || 20) }
    @pool = LoadBalancedConnectionPool.new(@@converter.asBuffer(pools),
                                           Proc.new { |throwable| options[:recoverable_errors].any? { |e| throwable.getException.is_a?(e) } },
                                           options[:max_retries] || 3,
                                           options[:retry_down_node_after] || 1200,
                                           options[:all_nodes_down_factor] || 2)
  end

  def method_missing(method, *args, &block)
    @pool.apply { |connection| connection.send(method, *args, &block) }
  end
end
