require "spec"
require "load_balancing_connection_pool"

describe "LoadBalancingConnectionPool" do
  class RecoverableError < RuntimeError; end
  class Connection
    def initialize(stuff)
      @stuff                = stuff
      @recoverable_attempts = 0
    end

    def return_me_stuff
      @stuff
    end

    def arguments(a)
      a
    end

    def raise_recoverable
      @recoverable_attempts += 1
      raise RecoverableError, "Recover from me!"
    end

    def recoverable_attempts
      @recoverable_attempts
    end

    def raise_runtime
      raise RuntimeError, "No recover for you."
    end
  end

  class ConnectionFactory
    def initialize(stuff)
      @connection = Connection.new(stuff)
    end

    def create
      @connection
    end
    
    def validate(connection)
      true
    end

    def destroy(connection)
    end
  end

  before do
    @pool_one = ConnectionFactory.new("one")
    @pool_two = ConnectionFactory.new("two")
    @balancer = LoadBalancingConnectionPool.new([@pool_one, @pool_two], 
                                                :recoverable_errors => [RecoverableError],
                                                :max_retries        => 4)
  end

  it "borrows connections from the pools" do
    @balancer.return_me_stuff.should == "one"
    @balancer.return_me_stuff.should == "two"
  end

  it "passes arguments correctly" do
    @balancer.arguments("a").should == "a"
    @balancer.arguments("b").should == "b"
  end

  it "recovers from recoverable errors" do
    lambda { @balancer.raise_recoverable }.should raise_error(RecoverableError)
    @balancer.recoverable_attempts.should == 2
  end
end
