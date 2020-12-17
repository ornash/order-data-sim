# Cloud Kitchen Dispatch Simulator
-Roshan Ghumare

## Execution
This project is written in Scala, it can be executed using following options:
### sbt
1. Installing SBT on MacOS

`brew install sbt`

2. Execution

`sbt`

For larger simulations:

`sbt -J-Xmx8G` 

Use following commands on the sbt prompt.

2.1. Compilation
`compile`
2.2. Test
`test`
2.3. Run Simulation
`run` or `runMain com.css.simulator.DispatchSimulator`

### IntelliJ IDEA
1. Import this directory as a new project.
2. Setup Scala SDK
3. IntelliJ will detect it as an sbt project and build it automatically.
3.1. Tests - Individual test suite or entire package.
3.2. Run - Open class com.css.simulator.DispatchSimulator, since it is an "App" it can directly be executed as a main class.

Note: Increase memory in DispatchSimulator application configuration for larger simulations.

### Log Configuration
Log configuration XML is located at "./src/main/resources/logback.xml" in this directory.
Default log level is set to "debug"
Use "info" log level for trying out larger simulations. Too many logs are printed at debug level which consume resources and affect performance.

## Terms Used
### DispatchSimulator Input Config
DispatchSimulator prompt will ask for the following configurations:
1. Orders input file
This file is expected to be a json of array of order notifications. The default is `./dispatch_orders.json`.
2. Order receipt speed per second
Orders arrival speed can be simulated with this config. The default is 2.
3. Worker thread count for processing orders
Orders are processed in a thread pool. The default is good enough even for larger simulations but you can tweak this parameter for experimentation. The default count is half of your machines total number of processors.
4. Worker thread count for dispatching couriers
Courier dispatches are processed in a separate thread pool. The default is good enough even for larger simulations but you can tweak this parameter for experimentation. The default count is half of your machines total number of processors.
5. Minimum delay(in seconds) for courier dispatch
This is the minimum amount of time a courier can take to arrive at CloudKitchen. The default is 3.
6. Maximum delay(in seconds) for courier dispatch
This is the maximum amount of time a courier can take to arrive at CloudKitchen. The default is 15. Simulation will use a random number between these min and max durations.
7. Match Strategy
This is strategy that will be used to match ready orders with arrived couriers. The default is 1 (FIFO)
"1" -> FIFO match i.e. couriers will be assigned cooked orders in the order they arrive at CloudKitchen.
"2" -> Order-Id based match i.e. couriers will be dispatched for specific order-id and they will pickup only those order-ids. 

### DispatchSimulator Output
   - when to configure thread count

### Approach
   - model explanation
   - CloudKitcheen and thread pool explanation
   - Matcher and strategy explanation
     - why it isnt multithreaded
   - not doing x because
### Conclusion
   - which one is better


to do project
	-cached thread pool cannot create native threads above 500 arrivals/second because macOs limit of 4k threads.. memory and stack size is not a problem.
	-fixed thread pool adds scheduling delays as if there are limited drivers  or limited couriers so it doesnt really simulate what we want to measure

Things Learnt
	- ideal-thread-count = (avg(prepDuration) * arrivalSpeed) + (avg(arrivalDelayDuration) * arrivalSpeed) + 1 (proceesor thread)
						   -ignore (avg(order-wait-duration) * arrivalSpeed) + (avg(courier-wait-duration) * arrivalSpeed)
	- ideal-thread-count = Min(ideal-thread-count, (totalOrders * 2) + 1)

To Write
	- I know CourierStatus and OrderStatus are similar but keep them separate with some redundant code for clarity.	
	- MatchInstant and DeliveryInstant APIs can be added but not adding to keep interface simple
	- Explain stats
	- FIFO is better with low arrival speed, OrderId is better for high arrival speed.
