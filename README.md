to do project
	-should I used separate thread pools for waiting and arrivals?
	-should I used separate thread pools for order workers and courier workers
	-tests
	-readme
	-try out other options
	-allow option to create infinite threads
	-cached thread pool cannot create native threads above 500 arrivals/second because macOs limit of 4k threads.. memory and stack size is not a problem.
	-fixed thread pool adds scheduling delays as if there are limited drivers  or limited couriers so it doesnt really simulate what we want to measure
	-

Things Learnt
	- thread.sleep doesnt release executor thread for others to use, CPU cannot be used during sleep. can you fix this?
	- onComplete needs another thread altogether
	- ideal-thread-count = (avg(prepDuration) * arrivalSpeed) + (avg(arrivalDelayDuration) * arrivalSpeed) + 1 (proceesor thread)
						   -ignore (avg(order-wait-duration) * arrivalSpeed) + (avg(courier-wait-duration) * arrivalSpeed)
	- ideal-thread-count = Min(ideal-thread-count, (totalOrders * 2) + 1)

To Write
	- I know CourierStatus and OrderStatus are similar but keep them separate with some redundant code for clarity.	
	- MatchInstant and DeliveryInstant APIs can be added but not adding to keep interface simple
	- Explain stats
	- FIFO is better with low arrival speed, OrderId is better for high arrival speed.

[13:06:49.443] - Order receipt Stats:                   ThreadCount= 32, ArrivalSpeed=2, Total=132, Avg=4153, Median=3929, Max=9807, Min=0
[13:06:49.445] - Expected order prep Stats:             ThreadCount= 32, ArrivalSpeed=2, Total=132, Avg=8946, Median=9000, Max=15000, Min=3000
[13:06:49.446] - Order cooking Stats:                   ThreadCount= 32, ArrivalSpeed=2, Total=132, Avg=8950, Median=9002, Max=15006, Min=3000
[13:06:49.448] - Order wait Stats:                      ThreadCount= 32, ArrivalSpeed=2, Total=132, Avg=1019, Median=390, Max=5460, Min=7
[13:06:49.448] - Expected courier arrivalDelay Stats:   ThreadCount= 32, ArrivalSpeed=2, Total=132, Avg=9015, Median=9000, Max=15000, Min=3000
[13:06:49.450] - Courier dispatch Stats:                ThreadCount= 32, ArrivalSpeed=2, Total=132, Avg=13383, Median=13404, Max=24793, Min=3006
[13:06:49.451] - Courier wait Stats:                    ThreadCount= 32, ArrivalSpeed=2, Total=132, Avg=739, Median=247, Max=14314, Min=15

[13:11:04.095] - Order Receipt Stats:                   ThreadCount= 64, ArrivalSpeed=2, Total=132, Avg=0, Median=0, Max=8, Min=0
[13:11:04.097] - Expected Order Prep Stats:             ThreadCount= 64, ArrivalSpeed=2, Total=132, Avg=8946, Median=9000, Max=15000, Min=3000
[13:11:04.099] - Order Cooking Stats:                   ThreadCount= 64, ArrivalSpeed=2, Total=132, Avg=8949, Median=9001, Max=15005, Min=3001
[13:11:04.101] - Order Wait Stats:                      ThreadCount= 64, ArrivalSpeed=2, Total=132, Avg=741, Median=151, Max=3028, Min=1
[13:11:04.101] - Expected Courier ArrivalDelay Stats:   ThreadCount= 64, ArrivalSpeed=2, Total=132, Avg=8825, Median=8000, Max=15000, Min=3000
[13:11:04.103] - Courier Dispatch Stats:                ThreadCount= 64, ArrivalSpeed=2, Total=132, Avg=8828, Median=8004, Max=15005, Min=3000
[13:11:04.104] - Courier Wait Stats:                    ThreadCount= 64, ArrivalSpeed=2, Total=132, Avg=863, Median=93, Max=16178, Min=6

[13:15:09.790] - Order Receipt Stats:                   ThreadCount= 32, ArrivalSpeed=20, Total=132, Avg=31183, Median=31992, Max=65998, Min=0
[13:15:09.792] - Expected Order Prep Stats:             ThreadCount= 32, ArrivalSpeed=20, Total=132, Avg=8946, Median=9000, Max=15000, Min=3000
[13:15:09.794] - Order Cooking Stats:                   ThreadCount= 32, ArrivalSpeed=20, Total=132, Avg=8949, Median=9002, Max=15006, Min=3001
[13:15:09.795] - Order Wait Stats:                      ThreadCount= 32, ArrivalSpeed=20, Total=132, Avg=1277, Median=1082, Max=6423, Min=15
[13:15:09.796] - Expected Courier ArrivalDelay Stats:   ThreadCount= 32, ArrivalSpeed=20, Total=132, Avg=9424, Median=9000, Max=15000, Min=3000
[13:15:09.797] - Courier Dispatch Stats:                ThreadCount= 32, ArrivalSpeed=20, Total=132, Avg=40921, Median=41491, Max=81003, Min=3006
[13:15:09.799] - Courier Wait Stats:                    ThreadCount= 32, ArrivalSpeed=20, Total=132, Avg=489, Median=228, Max=9291, Min=2

[13:18:43.931] - Order Receipt Stats:                   ThreadCount= 64, ArrivalSpeed=20, Total=132, Avg=11405, Median=10983, Max=26979, Min=0
[13:18:43.933] - Expected Order Prep Stats:             ThreadCount= 64, ArrivalSpeed=20, Total=132, Avg=8946, Median=9000, Max=15000, Min=3000
[13:18:43.935] - Order Cooking Stats:                   ThreadCount= 64, ArrivalSpeed=20, Total=132, Avg=8949, Median=9002, Max=15005, Min=3000
[13:18:43.936] - Order Wait Stats:                      ThreadCount= 64, ArrivalSpeed=20, Total=132, Avg=748, Median=165, Max=3043, Min=47
[13:18:43.937] - Expected Courier ArrivalDelay Stats:   ThreadCount= 64, ArrivalSpeed=20, Total=132, Avg=9121, Median=9000, Max=15000, Min=3000
[13:18:43.939] - Courier Dispatch Stats:                ThreadCount= 64, ArrivalSpeed=20, Total=132, Avg=20666, Median=20484, Max=38980, Min=3004
[13:18:43.940] - Courier Wait Stats:                    ThreadCount= 64, ArrivalSpeed=20, Total=132, Avg=437, Median=128, Max=6169, Min=14

[16:20:08.306] - Results Using match strategy: FifoMatchStrategy()
[16:20:08.324] - Order Receipt Stats:                   ThreadCount= 366, ArrivalSpeed=20, Total=1056, Avg=9, Median=1, Max=965, Min=0
[16:20:08.327] - Expected Order Prep Stats:             ThreadCount= 366, ArrivalSpeed=20, Total=1056, Avg=8946, Median=9000, Max=15000, Min=3000
[16:20:08.331] - Order Cooking Stats:                   ThreadCount= 366, ArrivalSpeed=20, Total=1056, Avg=8949, Median=9002, Max=15005, Min=3000
[16:20:08.335] - Order Wait Stats:                      ThreadCount= 366, ArrivalSpeed=20, Total=1056, Avg=255, Median=81, Max=2493, Min=3
[16:20:08.337] - Expected Courier ArrivalDelay Stats:   ThreadCount= 366, ArrivalSpeed=20, Total=1056, Avg=8945, Median=9000, Max=15000, Min=3000
[16:20:08.341] - Courier Dispatch Stats:                ThreadCount= 366, ArrivalSpeed=20, Total=1056, Avg=8959, Median=9002, Max=15902, Min=3000
[16:20:08.344] - Courier Wait Stats:                    ThreadCount= 366, ArrivalSpeed=20, Total=1056, Avg=254, Median=69, Max=10153, Min=0

[16:27:14.751] - Results Using match strategy: OrderIdMatchStrategy()
[16:27:14.769] - Order Receipt Stats:                   ThreadCount= 366, ArrivalSpeed=20, Total=1056, Avg=45, Median=1, Max=966, Min=0
[16:27:14.774] - Expected Order Prep Stats:             ThreadCount= 366, ArrivalSpeed=20, Total=1056, Avg=8946, Median=9000, Max=15000, Min=3000
[16:27:14.779] - Order Cooking Stats:                   ThreadCount= 366, ArrivalSpeed=20, Total=1056, Avg=8949, Median=9002, Max=15005, Min=3000
[16:27:14.783] - Order Wait Stats:                      ThreadCount= 366, ArrivalSpeed=20, Total=1056, Avg=2284, Median=497, Max=12138, Min=1
[16:27:14.786] - Expected Courier ArrivalDelay Stats:   ThreadCount= 366, ArrivalSpeed=20, Total=1056, Avg=9031, Median=9000, Max=15000, Min=3000
[16:27:14.791] - Courier Dispatch Stats:                ThreadCount= 366, ArrivalSpeed=20, Total=1056, Avg=9089, Median=9004, Max=15963, Min=3000
[16:27:14.794] - Courier Wait Stats:                    ThreadCount= 366, ArrivalSpeed=20, Total=1056, Avg=2190, Median=160, Max=12166, Min=0

[16:37:27.699] - Results Using match strategy: FifoMatchStrategy()
[16:37:27.719] - Order Receipt Stats:                   ThreadCount= 180, ArrivalSpeed=20, Total=1056, Avg=23547, Median=22944, Max=49887, Min=0
[16:37:27.722] - Expected Order Prep Stats:             ThreadCount= 180, ArrivalSpeed=20, Total=1056, Avg=8946, Median=9000, Max=15000, Min=3000
[16:37:27.727] - Order Cooking Stats:                   ThreadCount= 180, ArrivalSpeed=20, Total=1056, Avg=8949, Median=9002, Max=15006, Min=3000
[16:37:27.730] - Order Wait Stats:                      ThreadCount= 180, ArrivalSpeed=20, Total=1056, Avg=583, Median=393, Max=3022, Min=3
[16:37:27.733] - Expected Courier ArrivalDelay Stats:   ThreadCount= 180, ArrivalSpeed=20, Total=1056, Avg=9035, Median=9000, Max=15000, Min=3000
[16:37:27.737] - Courier Dispatch Stats:                ThreadCount= 180, ArrivalSpeed=20, Total=1056, Avg=32631, Median=32915, Max=64880, Min=3001
[16:37:27.741] - Courier Wait Stats:                    ThreadCount= 180, ArrivalSpeed=20, Total=1056, Avg=449, Median=285, Max=8238, Min=0

[16:40:49.872] - Results Using match strategy: OrderIdMatchStrategy()
[16:40:49.891] - Order Receipt Stats:                   ThreadCount= 180, ArrivalSpeed=20, Total=1056, Avg=23768, Median=23945, Max=49869, Min=0
[16:40:49.895] - Expected Order Prep Stats:             ThreadCount= 180, ArrivalSpeed=20, Total=1056, Avg=8946, Median=9000, Max=15000, Min=3000
[16:40:49.899] - Order Cooking Stats:                   ThreadCount= 180, ArrivalSpeed=20, Total=1056, Avg=8949, Median=9003, Max=15006, Min=3000
[16:40:49.903] - Order Wait Stats:                      ThreadCount= 180, ArrivalSpeed=20, Total=1056, Avg=2421, Median=485, Max=12481, Min=0
[16:40:49.906] - Expected Courier ArrivalDelay Stats:   ThreadCount= 180, ArrivalSpeed=20, Total=1056, Avg=9005, Median=9000, Max=15000, Min=3000
[16:40:49.910] - Courier Dispatch Stats:                ThreadCount= 180, ArrivalSpeed=20, Total=1056, Avg=32823, Median=32959, Max=63885, Min=3003
[16:40:49.913] - Courier Wait Stats:                    ThreadCount= 180, ArrivalSpeed=20, Total=1056, Avg=2316, Median=447, Max=12484, Min=0


[09:58:03.559] - Results Using match strategy: FifoMatchStrategy()
[09:58:03.563] - Sizes: receivedOrders=33792, matchedOrders=33792
[09:58:03.565] - Sizes: receivedCouriers=33792, matchedCouriers=33792
[09:58:03.636] - Order Receipt Stats:                   ThreadCount= 32, ArrivalSpeed=250, Total=33792, Avg=0, Median=0, Max=32, Min=0
[09:58:03.662] - Expected Order Prep Stats:             ThreadCount= 32, ArrivalSpeed=250, Total=33792, Avg=8946, Median=9000, Max=15000, Min=3000
[09:58:03.682] - Order Cooking Stats:                   ThreadCount= 32, ArrivalSpeed=250, Total=33792, Avg=9163, Median=9005, Max=25948, Min=3000
[09:58:03.700] - Order Wait Stats:                      ThreadCount= 32, ArrivalSpeed=250, Total=33792, Avg=100, Median=11, Max=5866, Min=0
[09:58:03.712] - Expected Courier ArrivalDelay Stats:   ThreadCount= 32, ArrivalSpeed=250, Total=33792, Avg=9006, Median=9000, Max=15000, Min=3000
[09:58:03.728] - Courier Dispatch Stats:                ThreadCount= 32, ArrivalSpeed=250, Total=33792, Avg=9220, Median=9006, Max=25948, Min=3000
[09:58:03.740] - Courier Wait Stats:                    ThreadCount= 32, ArrivalSpeed=250, Total=33792, Avg=43, Median=1, Max=7939, Min=0
[09:58:03.740] - Largest thread pool size: 3405
[09:58:03.740] - StartTime = 1607266299437 EndTime = 1607266683740 Diff = 384303


[10:09:32.116] - Results Using match strategy: FifoMatchStrategy()
[10:09:32.124] - Sizes: receivedOrders=33792, matchedOrders=33792
[10:09:32.126] - Sizes: receivedCouriers=33792, matchedCouriers=33792
[10:09:32.205] - Order Receipt Stats:                   ThreadCount= 32, ArrivalSpeed=500, Total=33792, Avg=0, Median=0, Max=113, Min=0
[10:09:32.233] - Expected Order Prep Stats:             ThreadCount= 32, ArrivalSpeed=500, Total=33792, Avg=8946, Median=9000, Max=15000, Min=3000
[10:09:32.253] - Order Cooking Stats:                   ThreadCount= 32, ArrivalSpeed=500, Total=33792, Avg=9202, Median=9006, Max=15011, Min=3000
[10:09:32.276] - Order Wait Stats:                      ThreadCount= 32, ArrivalSpeed=500, Total=33792, Avg=88, Median=12, Max=2069, Min=0
[10:09:32.289] - Expected Courier ArrivalDelay Stats:   ThreadCount= 32, ArrivalSpeed=500, Total=33792, Avg=8981, Median=9000, Max=15000, Min=3000
[10:09:32.307] - Courier Dispatch Stats:                ThreadCount= 32, ArrivalSpeed=500, Total=33792, Avg=9235, Median=9006, Max=15111, Min=3000
[10:09:32.321] - Courier Wait Stats:                    ThreadCount= 32, ArrivalSpeed=500, Total=33792, Avg=55, Median=1, Max=6371, Min=0
[10:09:32.321] - Largest thread pool size: 4011
[10:09:32.321] - StartTime = 1607267057989 EndTime = 1607267372321 Diff = 314332


