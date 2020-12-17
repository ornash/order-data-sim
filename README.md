# Cloud Kitchen Dispatch Simulator
-Roshan Ghumare

-----------------------------------------------------------------------------------------------------------------------
## Execution
This project is written in Scala, it can be executed using following options:
### sbt
1. Installing SBT on MacOS

`brew install sbt`

2. Execution

`sbt`

For larger simulations:

`sbt -J-Xmx8G` 

Use following commands for compile, test and execution on the sbt prompt.

`compile` 
`test`
`run` or `runMain com.css.simulator.DispatchSimulator`

### IntelliJ IDEA
1. Import this directory as a new project.
2. Setup Scala SDK
3. IntelliJ will detect it as an sbt project and build it automatically.
   - Tests - Individual test suite or entire package.
   - Execution - Open class com.css.simulator.DispatchSimulator, since it is an "App" it can directly be executed as a main class.

Note: Increase memory in DispatchSimulator application configuration for larger simulations.

### Log Configuration
Log configuration XML is located at "./src/main/resources/logback.xml" in this directory.
Default log level is set to "debug".
Use "info" log level for trying out larger simulations. Too many logs are printed at DEBUG level which consume resources and affect performance.

-----------------------------------------------------------------------------------------------------------------------
## Terms Used
### DispatchSimulator Input Config
DispatchSimulator prompt will ask for the following configurations:
1. Orders input file: This file is expected to be a json of array of order notifications. The default is `./dispatch_orders.json`.
2. Order receipt speed per second: Orders arrival speed can be simulated with this config. The default is 2.
3. Worker thread count for processing orders: Orders are processed in a thread pool. The default is good enough even for larger simulations but you can tweak this parameter for experimentation. The default count is half of your machines total number of processors.
4. Worker thread count for dispatching couriers: Courier dispatches are processed in a separate thread pool. The default is good enough even for larger simulations but you can tweak this parameter for experimentation. The default count is half of your machines total number of processors.
5. Minimum delay(in seconds) for courier dispatch: This is the minimum amount of time a courier can take to arrive at CloudKitchen. The default is 3.
6. Maximum delay(in seconds) for courier dispatch: This is the maximum amount of time a courier can take to arrive at CloudKitchen. The default is 15. Simulation will use a random number between these min and max durations.
7. Match Strategy: This is strategy that will be used to match ready orders with arrived couriers. The default is 1 (FIFO)
   - "1" -> FIFO match i.e. couriers will be assigned cooked orders in the order they arrive at CloudKitchen.
   - "2" -> Order-Id based match i.e. couriers will be dispatched for specific order-id and they will pickup only those order-ids. 

### DispatchSimulator Output
DispatchSimulator prints statistics(average, median, min and max) related to order and courier. Here is a sample output:
```
INFO  MatchStrategyStats - Simulation stats for order-courier match strategy: FifoMatchStrategy()
INFO  MatchStrategyStats - Expected order prepDuration stats:      Total=132, Avg=8946ms, Median=9000ms, Max=15000ms, Min=3000ms
INFO  MatchStrategyStats - Actual order prepDuration stats:        Total=132, Avg=8947ms, Median=9000ms, Max=15002ms, Min=3000ms
INFO  MatchStrategyStats - Order match waitDuration stats:         Total=132, Avg=114ms, Median=1ms, Max=1008ms, Min=0ms
INFO  MatchStrategyStats -
INFO  MatchStrategyStats - Expected courier transitDuration stats: Total=132, Avg=8727ms, Median=8000ms, Max=15000ms, Min=3000ms
INFO  MatchStrategyStats - Actual courier transitDuration stats:   Total=132, Avg=8728ms, Median=8000ms, Max=15004ms, Min=3000ms
INFO  MatchStrategyStats - Courier match waitDuration stats:       Total=132, Avg=334ms, Median=9ms, Max=1978ms, Min=0ms
```
1. Expected order prepDuration stats: This is the expected duration for cooking an order. This is obtained from input json.
2. Actual order prepDuration stats: This is the actual duration taken for cooking an order. This output is provided to measure performance of thread pool and scheduler. If this duration is larger than expected duration in (1), it means the thread pool is not able to keep up with order arrival speed.
3. Order match waitDuration stats: This is duration an order spent waiting for courier after it was ready. This can be used to compare FIFO vs Order-Id match strategy.
4. Expected courier transitDuration stats: This is the expected duration a courier spends in transit after dispatch and before arrival at CloudKitchen. This is obtained by picking a random number between min and max input for courier dispatch.
5. Actual courier transitDuration stats: This is the actual duration a courier spends in transit after dispatch and before arrival at CloudKitchen. This output is provided to measure performance of thread pool and scheduler. If this duration is larger than expected duration in (3), it means the thread pool is not able to keep up with courier arrival speed.
6. Courier match waitDuration stats: This is duration a courier spent waiting for an order after courier's arrival. This can be used to compare FIFO vs Order-Id match strategy.

-----------------------------------------------------------------------------------------------------------------------
### Approach
- Order and Courier classes model order and courier along with their various state transitions from reception to delivery.
- CloudKitchen class models tasks of chefs and couriers i.e. cooking and dispatch. Java's ScheduledThreadPool is used for efficiency, using this pool allows simulations with high arrival speed. I was able to test with arrival speed as high as 500,000 orders per second.
- MatchStrategy interface models the strategy for matching ready orders with arrived couriers. It has two implementations, FIFO and Order-Id based match.
- Matcher class runs in a single thread continuously and applies selected MatchStrategy on ready orders and arrived couriers. This is not multi-threaded because I was able to test this with good performance even for high arrival speed.
- Two queues, OrderQueue and CourierQueue are used for communication be CloudKithen threads and Matcher thread.
- Order input files. 
  - I have included 4 order input files: dispatch_orders.json, dispatch_orders_1k.json, dispatch_orders_16k.json, and dispatch_orders_135k.json for testing with large number of orders.
  - I was able to test with over a million orders and arrival speed as high as 500,000 orders per second.
- Note: Disable DEBUG log for accurate results when simulating with large number of orders or high order arrival speed.

-----------------------------------------------------------------------------------------------------------------------
### Conclusion
- The total number of orders or couriers does not affect performance.
- The performance is mainly affected by order arrival speed.
- FIFO strategy is consistently faster as compared to Order-Id based match strategy for lower as well as higher order arrival speed.

-----------------------------------------------------------------------------------------------------------------------
### Sample Results
#### Total Orders: 1081344, Order Arrival Speed: 300000 per second
MatchStrategyStats - Simulation stats for order-courier match strategy: FifoMatchStrategy()
MatchStrategyStats - Expected order prepDuration stats:      Total=1081344, Avg=9010ms, Median=9000ms, Max=15000ms, Min=3000ms
MatchStrategyStats - Actual order prepDuration stats:        Total=1081344, Avg=9048ms, Median=9000ms, Max=15273ms, Min=3000ms
MatchStrategyStats - Order match waitDuration stats:         Total=1081344, Avg=502ms, Median=4ms, Max=3148ms, Min=0ms
MatchStrategyStats -
MatchStrategyStats - Expected courier transitDuration stats: Total=1081344, Avg=8999ms, Median=9000ms, Max=15000ms, Min=3000ms
MatchStrategyStats - Actual courier transitDuration stats:   Total=1081344, Avg=9026ms, Median=9000ms, Max=15271ms, Min=3000ms
MatchStrategyStats - Courier match waitDuration stats:       Total=1081344, Avg=525ms, Median=29ms, Max=3167ms, Min=0ms

MatchStrategyStats - Simulation stats for order-courier match strategy: OrderIdMatchStrategy()
MatchStrategyStats - Expected order prepDuration stats:      Total=1081344, Avg=9010ms, Median=9000ms, Max=15000ms, Min=3000ms
MatchStrategyStats - Actual order prepDuration stats:        Total=1081344, Avg=9031ms, Median=9000ms, Max=15353ms, Min=3000ms
MatchStrategyStats - Order match waitDuration stats:         Total=1081344, Avg=2153ms, Median=11ms, Max=12403ms, Min=0ms
MatchStrategyStats -
MatchStrategyStats - Expected courier transitDuration stats: Total=1081344, Avg=9000ms, Median=9000ms, Max=15000ms, Min=3000ms
MatchStrategyStats - Actual courier transitDuration stats:   Total=1081344, Avg=9021ms, Median=9000ms, Max=15354ms, Min=3000ms
MatchStrategyStats - Courier match waitDuration stats:       Total=1081344, Avg=2163ms, Median=14ms, Max=12361ms, Min=0ms

#### Total Orders: 135168, Order Arrival Speed: 10000 per second
MatchStrategyStats - Simulation stats for order-courier match strategy: FifoMatchStrategy()
MatchStrategyStats - Expected order prepDuration stats:      Total=135168, Avg=8985ms, Median=9000ms, Max=15000ms, Min=3000ms
MatchStrategyStats - Actual order prepDuration stats:        Total=135168, Avg=8985ms, Median=9000ms, Max=15005ms, Min=3000ms
MatchStrategyStats - Order match waitDuration stats:         Total=135168, Avg=35ms, Median=4ms, Max=968ms, Min=0ms
MatchStrategyStats -
MatchStrategyStats - Expected courier transitDuration stats: Total=135168, Avg=9020ms, Median=9000ms, Max=15000ms, Min=3000ms
MatchStrategyStats - Actual courier transitDuration stats:   Total=135168, Avg=9020ms, Median=9000ms, Max=15010ms, Min=3000ms
MatchStrategyStats - Courier match waitDuration stats:       Total=135168, Avg=1ms, Median=0ms, Max=998ms, Min=0ms

MatchStrategyStats - Simulation stats for order-courier match strategy: OrderIdMatchStrategy()
MatchStrategyStats - Expected order prepDuration stats:      Total=135168, Avg=8985ms, Median=9000ms, Max=15000ms, Min=3000ms
MatchStrategyStats - Actual order prepDuration stats:        Total=135168, Avg=8986ms, Median=9000ms, Max=15005ms, Min=3000ms
MatchStrategyStats - Order match waitDuration stats:         Total=135168, Avg=2155ms, Median=0ms, Max=12005ms, Min=0ms
MatchStrategyStats -
MatchStrategyStats - Expected courier transitDuration stats: Total=135168, Avg=8996ms, Median=9000ms, Max=15000ms, Min=3000ms
MatchStrategyStats - Actual courier transitDuration stats:   Total=135168, Avg=8997ms, Median=9000ms, Max=15005ms, Min=3000ms
MatchStrategyStats - Courier match waitDuration stats:       Total=135168, Avg=2143ms, Median=0ms, Max=12004ms, Min=0ms

#### Total Orders: 16896, Order Arrival Speed: 1000 per second
MatchStrategyStats - Simulation stats for order-courier match strategy: FifoMatchStrategy()
MatchStrategyStats - Expected order prepDuration stats:      Total=16896, Avg=9023ms, Median=9000ms, Max=15000ms, Min=3000ms
MatchStrategyStats - Actual order prepDuration stats:        Total=16896, Avg=9023ms, Median=9000ms, Max=15019ms, Min=3000ms
MatchStrategyStats - Order match waitDuration stats:         Total=16896, Avg=26ms, Median=0ms, Max=991ms, Min=0ms
MatchStrategyStats -
MatchStrategyStats - Expected courier transitDuration stats: Total=16896, Avg=9031ms, Median=9000ms, Max=15000ms, Min=3000ms
MatchStrategyStats - Actual courier transitDuration stats:   Total=16896, Avg=9031ms, Median=9000ms, Max=15019ms, Min=3000ms
MatchStrategyStats - Courier match waitDuration stats:       Total=16896, Avg=18ms, Median=0ms, Max=999ms, Min=0ms

MatchStrategyStats - Simulation stats for order-courier match strategy: OrderIdMatchStrategy()
MatchStrategyStats - Expected order prepDuration stats:      Total=16896, Avg=9023ms, Median=9000ms, Max=15000ms, Min=3000ms
MatchStrategyStats - Actual order prepDuration stats:        Total=16896, Avg=9023ms, Median=9000ms, Max=15005ms, Min=3000ms
MatchStrategyStats - Order match waitDuration stats:         Total=16896, Avg=2148ms, Median=0ms, Max=12005ms, Min=0ms
MatchStrategyStats -
MatchStrategyStats - Expected courier transitDuration stats: Total=16896, Avg=9012ms, Median=9000ms, Max=15000ms, Min=3000ms
MatchStrategyStats - Actual courier transitDuration stats:   Total=16896, Avg=9013ms, Median=9000ms, Max=15005ms, Min=3000ms
MatchStrategyStats - Courier match waitDuration stats:       Total=16896, Avg=2159ms, Median=0ms, Max=12049ms, Min=0ms

#### Total Orders: 1056, Order Arrival Speed: 100 per second
MatchStrategyStats - Simulation stats for order-courier match strategy: FifoMatchStrategy()
MatchStrategyStats - Expected order prepDuration stats:      Total=1056, Avg=8857ms, Median=9000ms, Max=15000ms, Min=3000ms
MatchStrategyStats - Actual order prepDuration stats:        Total=1056, Avg=8858ms, Median=9000ms, Max=15004ms, Min=3000ms
MatchStrategyStats - Order match waitDuration stats:         Total=1056, Avg=428ms, Median=18ms, Max=1003ms, Min=0ms
MatchStrategyStats -
MatchStrategyStats - Expected courier transitDuration stats: Total=1056, Avg=9277ms, Median=9000ms, Max=15000ms, Min=3000ms
MatchStrategyStats - Actual courier transitDuration stats:   Total=1056, Avg=9277ms, Median=9001ms, Max=15004ms, Min=3000ms
MatchStrategyStats - Courier match waitDuration stats:       Total=1056, Avg=9ms, Median=0ms, Max=995ms, Min=0ms

MatchStrategyStats - Simulation stats for order-courier match strategy: OrderIdMatchStrategy()
MatchStrategyStats - Expected order prepDuration stats:      Total=1056, Avg=8857ms, Median=9000ms, Max=15000ms, Min=3000ms
MatchStrategyStats - Actual order prepDuration stats:        Total=1056, Avg=8858ms, Median=9000ms, Max=15004ms, Min=3000ms
MatchStrategyStats - Order match waitDuration stats:         Total=1056, Avg=2123ms, Median=0ms, Max=12004ms, Min=0ms
MatchStrategyStats -
MatchStrategyStats - Expected courier transitDuration stats: Total=1056, Avg=8766ms, Median=9000ms, Max=15000ms, Min=3000ms
MatchStrategyStats - Actual courier transitDuration stats:   Total=1056, Avg=8766ms, Median=9000ms, Max=15005ms, Min=3000ms
MatchStrategyStats - Courier match waitDuration stats:       Total=1056, Avg=2215ms, Median=0ms, Max=12004ms, Min=0ms
