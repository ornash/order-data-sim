# Cloud Kitchen Dispatch Simulator
-Roshan Ghumare

## Execution
This project is written in Scala, it can be executed using following options:
### sbt
1. Installing SBT on MacOS
`brew install sbt`
2. Execution
`sbt`
      #sbt
      #sbt -J-Xmx8G --to increase memory for large simulation
      #sbt:order-data-sim> compile
      #sbt:order-data-sim> test
      #sbt:order-data-sim> run 
      #sbt:order-data-sim> runMain com.css.simulator.DispatchSimulator
      - tests
      - run - memmory
### IntelliJ IDEA
      - import as new project
         - setup scala sdk
         - intellij will detect it as a sbt project and build it
      - tests - individual or entire suite in package
      - run - com.css.simulator.DispatchSimulator application
      - run - ~run as sbt-task
      - memory - -Xmx8G in JVM options of configuration
   - logging config
      - debug and info
      - use info when trying a large simulation because too many logs are printed and consume resources and affect performance
2. Meaning of terms
   - simulator config explanation
   - output explanation
   - when to configure thread count
3. My approach
   - model explanation
   - CloudKitcheen and thread pool explanation
   - Matcher and strategy explanation
     - why it isnt multithreaded
   - not doing x because
4. Conclusion
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
